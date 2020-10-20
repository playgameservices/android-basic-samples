Google Play game services - Android Samples
===========================================
Copyright (C) 2014 Google Inc.

<h2>Contents</h2>

These are the Android samples for Google Play game services.

* **CollectAllTheStars2**. Demonstrates how to use the Snapshots feature to save game data. The sample signs the user in, synchronizes their data from a named Snapshot, then updates the UI to reflect the game state saved in the Snapshot.

* **TypeANumber**. Demonstrates how to use leaderboards, achievements, events, and friends. In this exciting game, you type the score you think you deserve. But wait! There is a twist. If you are playing in easy mode, you get the score you requested. However, if you are playing in hard mode, you only get half! (tough game, we know). You can also check how your friends perform in this game by checking out social leaderboards.

<h2>How to run a sample</h2>

1. Set up the project in the Developer Console by following [these instructions](https://developers.google.com/games/services/console/enabling).
   Note your **package name** and the **application ID** of the project!

1. For the **Type a Number sample**, you need to create leaderboards/achievements.
(You can see the ones that the sample needs in its [res/values/ids.xml](TypeANumber/src/main/res/values/ids.xml) file.)
  You can create them two ways:
   1. Add them via the [Developer console](https://play.google.com/apps/publish/#GameListPlace).
   1. Use [this utility](http://playgameservices.github.io/android-basic-samples/config-magic/index.html?sample=typeanumber), which will automatically create them for you.

<h3>Building using Android Studio...</h3>

1. Open Android Studio and launch the Android SDK manager from it (Tools | Android | SDK Manager)
1. Ensure the following components are installed and updated to the latest version.
   1. *Android SDK Platform-Tools*
   1. *Android Support Repository*
   1. *Google Repository*
1. Return to Android Studio and select *Open an existing Android Studio project*
1. Select the **android-basic-samples** directory.

<h3>Modify IDs, compile and run</h3>

To set up a sample:

1. Change the *applicationId* in the build.gradle file to your own **package name** (ie - com.example.package.name)
   (the same one you registered in Developer Console!).  You will have to update
   the build.gradle file for *each* sample you want to run.  There is no need to
   edit the AndroidManifest.xml file.
1. In the Developer console, select a resource type
   (Achievements, Events, Leaderboards) and click "Get Resources".  Copy the
    contents from the console and replace the contents of res/values/ids.xml.
    1.  If you are running Android Studio, check the TODO window to see if there are any remaining tasks.
1. Compile and run.

**IMPORTANT**: make sure to sign your apk with the same certificate
as the one whose fingerprint you configured on Developer Console, otherwise
you will see errors.

**IMPORTANT**: if you are testing an unpublished game, make sure that the account you intend
to sign in with (the account on the test device) is listed as a tester in the
project on your Developer Console setup (check the list in the "Testing"
section), otherwise the server will act as though your project did not exist and
return errors like 'Failed to sign in. Please check your network connection and try again.'

<h3>Building</h3>
To build the samples after you have applied the changes above, you can use the build/run option in Android Studio, or build directly from the command line if you prefer.

**IMPORTANT** Ensure you have set the ANDROID_HOME environment variable.

    cd /path/to/android-basic-samples
    export ANDROID_HOME = /path/to/android/sdk
    ./gradlew build

<h2>Support</h2>

First of all, take a look at our [troubleshooting guide](https://developers.google.com/games/services/android/troubleshooting). Most setup issues can be solved by following this guide.

If your question is not answered by the troubleshooting guide, we encourage you to post your question to [stackoverflow.com](https://stackoverflow.com/questions/tagged/google-play-games). Our team answers questions there regularly.

Samples written by [Bruno Oliveira](https://plus.google.com/102451193315916178828) with contributions from [Wolff](http://plus.google.com/+WolffDobson).* Feel free to add us to your circles on Google Plus and pester us to fix stuff that's broken or answer a question on stackoverflow :-)

<h2>Special Thanks</h2>

* To [ligi](http://github.com/ligi) for contributing the initial Gradle build files
* To [bechhansen](https://github.com/bechhansen) for fixing a bug in GameHelper where the turn-based match was being lost when a non-Games client connected.
