#!/bin/bash -eu

# On the master branch we want to build against the latest version of 
# GMS Core. That involves fiddling with the Android SDK directly, since
# the current Android build system doesn't have a hook to specify
# a custom location for the GMS archive.
#
# In order to keep the build somewhat hermetic, create a new SDK
# directory and symlink everything but the directory where the 
# GMS archives live.
: ${script_dir:="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"}

declare -r android_sdk_copy="`pwd`/android-sdk"
if [ -d "${android_sdk_copy}" -o -e "${android_sdk_copy}" ]
then
  rm -rf "${android_sdk_copy}"
fi
mkdir "${android_sdk_copy}"

IFS=$'\n'
for dir in $( ls ${ANDROID_HOME} | grep -v extras ); do
    ln -s ${ANDROID_HOME}/${dir} ${android_sdk_copy}/${dir}
done

cp -r ${ANDROID_HOME}/extras ${android_sdk_copy}/extras
ANDROID_HOME=${android_sdk_copy}


: ${gms_branch:=manchego}
: ${gms_ver:=6.00.00}
: ${gms_aar:=gms.aar}

# Download the latest dogfood 
"${script_dir}/DO_NOT_MERGE/dogfood_gms.par" -k $CHEESETASTER_KEY -b ub-gcore-${gms_branch}-release gms.aar

# Force the build to use the dogfood GMS by removing everything else.
"${script_dir}/DO_NOT_MERGE/fake_maven.par" remove -r $ANDROID_HOME/extras/google/m2repository -p com.google.android.gms.play-services -v '.*'

# "install" the latest dogfood into Android's fake maven repo.
"${script_dir}/DO_NOT_MERGE/fake_maven.par" add -r $ANDROID_HOME/extras/google/m2repository -p com.google.android.gms.play-services -v ${gms_ver} -f "${gms_aar}"

