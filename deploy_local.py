from argparse import ArgumentParser
import os
import shutil
import subprocess
from xml.etree import ElementTree

default_pom_template = """
<?xml version="1.0" encoding="UTF-8"?>
<project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <modelVersion>4.0.0</modelVersion>
  <groupId>{groupId}</groupId>
  <artifactId>{artifactId}</artifactId>
  <version>{version}</version>
  <packaging>{packaging}</packaging>
  <dependencies>
    <dependency>
      <groupId>com.android.support</groupId>
      <artifactId>support-v4</artifactId>
      <version>19.1.0</version>
      <scope>compile</scope>
    </dependency>
  </dependencies>
</project>
"""

def gen_hashes(path):
  subprocess.call([os.environ['SHELL'], '-lc', 'md5sum {p} > {p}.md5'.format(p = path)])
  subprocess.call([os.environ['SHELL'], '-lc', 'sha1sum {p} > {p}.sha1'.format(p = path)])

def deploy_local(repo_path, archive_src_path, package_name, package_version):
  # Infer strings that weren't supplied explicitly
  package_path = package_name.replace('.', os.path.sep)
  metadata_path = os.path.join(repo_path, package_path, 'maven-metadata.xml')
  group, artifact = package_name.rsplit('.', 1)
  archive_extension = os.path.splitext(archive_src_path)[1].strip('.')
  archive_dest_base_filename = '{}-{}'.format(artifact, package_version)
  archive_dest_filename = '{}.{}'.format(archive_dest_base_filename, archive_extension)
  archive_dest_dir = os.path.join(package_path, package_version)
  archive_dest_path = os.path.join(archive_dest_dir, archive_dest_filename)
  pomfile_path = os.path.join(archive_dest_dir, archive_dest_base_filename + '.pom')

  # Copy the archive into the repository
  print 'Copying {} => {}'.format(archive_src_path, archive_dest_path)
  if not os.path.exists(archive_dest_dir):
    os.makedirs(archive_dest_dir)
  shutil.copyfile(archive_src_path, archive_dest_path)
  gen_hashes(archive_dest_path)

  # Generate the POM for the archive
  print "Generating {}".format(pomfile_path)
  pom_text = default_pom_template.format(
    groupId = group,
    artifactId = artifact,
    version = package_version,
    packaging = archive_extension
  )
  with open(pomfile_path, 'w') as pomfile:
    pomfile.write(unicode(pom_text))
  gen_hashes(pomfile_path)

  # Modify the repo metadata file to include this archive's version
  print "Modifying metadata file at {}".format(metadata_path)
  metadata_xml = ElementTree.parse(metadata_path)
  versions_element = metadata_xml.find('./*/versions')
  existing_version_element = None
  for el in versions_element.findall('version'):
    if el.text == package_version:
      existing_version_element = el
      break
  if existing_version_element is None:
    new_version_element = ElementTree.SubElement(versions_element, 'version')
    new_version_element.text = package_version
    metadata_xml.write(metadata_path)



if __name__ == '__main__':
  parser = ArgumentParser("Register a package with Android's fake Maven repo")
  parser.add_argument(
    '-r', '--repository-path',
    help = 'Path to local Android repository (defaults to $ANDROID_HOME/extras/google/m2repository)',
    dest = 'repo_path',
    required = False,
    default = os.environ['ANDROID_HOME'] + '/extras/google/m2repository'
  )
  parser.add_argument(
    '-f', '--archive-file',
    help = 'Archive file to register',
    dest = 'archive_file',
    required = True
  )
  parser.add_argument(
    '-p', '--package',
    help = 'Fully qualified package name to register',
    dest = 'pkg',
    required = True
  )
  parser.add_argument(
    '-v', '--version',
    help = 'package version number',
    dest = 'ver',
    required = True
  )
  args = parser.parse_args()

  deploy_local(args.repo_path, args.archive_file, args.pkg, args.ver)