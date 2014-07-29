from argparse import ArgumentParser
import deploy_local
import inspect
import os
import subprocess
import sys


def get_script_dir(follow_symlinks=True):
  """get_script_dir() from JF Sebastian (http://stackoverflow.com/a/22881871/416621)"""
  if getattr(sys, 'frozen', False): # py2exe, PyInstaller, cx_Freeze
    path = os.path.abspath(sys.executable)
  else:
    path = inspect.getabsfile(get_script_dir)
  if follow_symlinks:
    path = os.path.realpath(path)
  return os.path.dirname(path)

if __name__ == '__main__':
  parser = ArgumentParser("Fetch an internal version of gms core and install to the Android local repository")
  parser.add_argument(
    '-b', '--branch',
    help = 'Gerrit branch to fetch GMS binaries from (e.g. ub-gcore-manchego-release)',
    dest = 'branch',
    required = True
  )
  parser.add_argument(
    '-v', '--install-as-version',
    help = 'Version identifier to assign to the installed GMS Core libraries (e.g. 6.0.0)',
    dest = 'version',
    required = True
  )
  parser.add_argument(
    '-k', '--keyfile',
    help = 'Path to OAuth keyfile to authenticate to the Android build server',
    dest = 'keyfile',
    required = True
  )
  args = parser.parse_args()

  repo_path = os.path.join(os.environ['ANDROID_HOME'], 'extras', 'google', 'm2repository')
  if not os.path.exists(repo_path):
    print 'ANDROID_HOME not set or not valid. Please set ANDROID_HOME to the location of a valid Android SDK'
    exit(1)

  script_path = get_script_dir()
  fetch_artifact_path = os.path.join(script_path, 'fetch_artifact')
  if not os.path.exists(fetch_artifact_path):
    print "Couldn't find fetch_artifact executable. Should be placed in same directory as this script."
    exit(1)

  fetch_to = '/tmp/gmscore-latest.aar'
  cmd = (fetch_artifact_path + 
    ' --branch ' + args.branch +
    ' --target '+ 'GmsCore' +
    ' --latest' 
    ' --apiary_service_account_email=1088486625756-9i8onaub0hcnmqm16st2f3h6lhc7r2k7@developer.gserviceaccount.com'
    ' --apiary_service_account_private_key_path=' + args.keyfile +
    ' google-play-services-???????.aar ' +
    fetch_to
  )

  print cmd
  subprocess.call([os.environ['SHELL'], '-lc', cmd])

  deploy_local(repo_path, fetch_to, 'com.google.android.gms.play-services', args.version)
