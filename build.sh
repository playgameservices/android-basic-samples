export JAVA_HOME=/usr/lib/jvm/java-7-openjdk-amd64
export PATH=$PATH:/usr/lib/jvm/java-7-openjdk-amd64/bin
cd BasicSamples
echo "sdk.dir=/opt/android-sdk" >local.properties 
./gradlew --stacktrace assembleDebug

