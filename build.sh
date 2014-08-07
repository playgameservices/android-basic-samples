set -e
set -u
echo Setting environment variables...
: ${ANDROID_HOME:=/opt/android-sdk}


JAVA_HOME=/usr/lib/jvm/java-7-openjdk-amd64
PATH=$PATH:/usr/lib/jvm/java-7-openjdk-amd64/bin

echo Copying Android SDK...
ANDROID_SDK_COPY=`pwd`/android-sdk
if [ -d $ANDROID_SDK_COPY -o -e $ANDROID_SDK_COPY ] 
then
  rm -rf $ANDROID_SDK_COPY
fi
echo "$ANDROID_HOME --> $ANDROID_SDK_COPY"
mkdir $ANDROID_SDK_COPY
#mkdir $ANDROID_SDK_COPY/extras
for DIR in $( ls $ANDROID_HOME )
do
  if [ $DIR != "extras" ]
  then
    ln -s $ANDROID_HOME/$DIR $ANDROID_SDK_COPY/$DIR
  fi
done
cp -r $ANDROID_HOME/extras $ANDROID_SDK_COPY/extras
ANDROID_HOME=$ANDROID_SDK_COPY

echo Running branch-specific build steps...
if [ -e .branch_build ]
then
  . .branch_build
fi

echo Building...
cd BasicSamples
echo "sdk.dir=$ANDROID_HOME" >local.properties 

./gradlew --stacktrace assembleDebug

