#!/bin/bash -eu

# Set environment variables
declare -r script_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

: ${ANDROID_HOME:=/opt/android-sdk}
: ${JAVA_HOME:=/usr/lib/jvm/java-7-openjdk-amd64}
PATH=$PATH:$JAVA_HOME/bin


echo Running branch-specific build steps...
if [ -e "${script_dir}/branch_build.sh" ]; then
  . "${script_dir}/branch_build.sh"
fi

cd "${script_dir}/BasicSamples"

# The Android build system wants to see a local.properties file that tells
# it where to find the Android SDK. 
echo "sdk.dir=${ANDROID_HOME}" >local.properties 

# Build all samples.
./gradlew --stacktrace assembleDebug

