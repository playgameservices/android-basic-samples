#!/bin/bash
set -eua

# On the master branch we want to build against the latest version of
# GMS Core. That involves fiddling with the Android SDK directly, since
# the current Android build system doesn't have a hook to specify
# a custom location for the GMS archive.

# This script needs to call binaries that are relative to its own
# location in the path. Use conditional-set because branch_build.sh
# is presumably called from build.sh, which sets this variable
# correctly.
: ${script_dir:="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"}


# Defines which GMS branch we consider "latest." Must be updated when
# we move to a new GMS branch. The expectation is that updating this
# will immediately break all of the tests, so keeping it as a manual
# step seems reasonable.
: ${GMS_BRANCH:=manchego}

# The version number to assign to the "latest" GMS archive. This is
# somewhat arbitrary, since x.x.x versions aren't assigned until
# release. The important thing is that it match the version string
# we're specifying in the sample build.gradle files (e.g. '6.+')
: ${GMS_VER:=6.00.00}

# temporary filename to use for storing the downloaded GMS archive
: ${GMS_AAR:=gms.aar}

# In order to keep the build somewhat hermetic, create a new SDK
# directory and symlink everything but the directory where the
# GMS archives live.
declare -r android_sdk_copy="`pwd`/android-sdk"
rm -rf ${android_sdk_copy}
mkdir "${android_sdk_copy}"

# Copy the entire extras directory over. This is a little less surgical than
# it could be, but it's quick enough, and copying the whole top-level directory
# simplifies the symlinking step that we'll do next.
cp -r ${ANDROID_HOME}/extras ${android_sdk_copy}/extras

# Symlink everything *but* the extras directory.
IFS=$'\n'
for dir in $( ls ${ANDROID_HOME} | grep -v extras ); do
  ln -s ${ANDROID_HOME}/${dir} ${android_sdk_copy}/${dir}
done

ANDROID_HOME=${android_sdk_copy}

# Download the latest dogfood GMS aar
"${script_dir}/DO_NOT_MERGE/dogfood_gms.par" -k $CHEESETASTER_KEY \
  -b ub-gcore-${GMS_BRANCH}-release ${GMS_AAR}

# Force the build to use the dogfood GMS by removing everything else.
"${script_dir}/DO_NOT_MERGE/fake_maven.par" remove -r \
  $ANDROID_HOME/extras/google/m2repository \
  -p com.google.android.gms.play-services -v '.*'

# "install" the latest dogfood into Android's fake maven repo.
"${script_dir}/DO_NOT_MERGE/fake_maven.par" add \
  -r $ANDROID_HOME/extras/google/m2repository \
  -p com.google.android.gms.play-services -v ${GMS_VER} -f "${GMS_AAR}"

# Clean up
rm -rf ${GMS_AAR}

