Google Play game services - Android Samples
===========================================
Copyright (C) 2013 Google Inc.

<h2>Contents</h2>

These are the Android samples for Google Play game services.

* **BasicSamples** - a set of basic samples, including a convenience library (BaseGameUtils):

    * **BaseGameUtils**. Utilities used on all samples, which you can use in your projects too. This is not a stand-alone sample, it's a library project.

    * **ButtonClicker2000**. Represents the new generation in modern button-clicking excitement. A simple multiplayer game sample that shows how to set up the Google Play real-time multiplayer API, invite friends, automatch, accept invitations, use the waiting room UI, send and receive messages and other multiplayer topics.

    * **CollectAllTheStars**. Demonstrates a typical use of cloud save. In this challenging game, there are 20 worlds of 12 levels each. When you click on a level, it will ask you how many stars you think you deserve on it. Honesty required! This sample demonstrates how to deal with cloud save conflicts (for example, if you play some levels on your phone and a different set of levels on your tablet).

    * **TrivialQuest**. The simplest possible single-player game. Shows how to sign in and how to unlock one achievement. Sign-in and click the button to win the game. Are you ready for this epic adventure?

    * **TypeANumber**. Shows leaderboards and achievements. In this exciting game, you type the score you think you deserve. But wait! There is a twist. If you are playing in easy mode, you get the score you requested. However, if you are playing in hard mode, you only get half! (tough game, we know).

* **EndlessTunnel** - a more complex sample that shows how to integrate Google Play Games into an NDK game written in C++.

**Note:** the samples that have corresponding counterparts for iOS and web (particularly, CollectAllTheStars and TypeANumber) are compatible across the platforms. This means that you can play some levels on CollectAllTheStars on your Android device, and then pick up your iOS device and continue where you left off! For TypeANumber, you will see your achievements and leaderboards on all platforms, and progress obtained on one will be reflected on the others.

<h2>How to run a sample</h2>

1. Set up the project in Developer Console. For more info:

      https://developers.google.com/games/services/console/enabling
 
   Note your package name and the APP ID of the project.

1. Create leaderboards/achievements as appropriate for the sample
   (see the ones that the sample needs in its res/values/ids.xml)

Pick a set of instructions below depending on whether you're using Eclipse or Android Studio.

<h3>If you're using Eclipse...</h3>

1. Start Eclipse
1. Import the desired sample from the `eclipse_compat` directory (Project | Import | Android | Existing Android Source)
1. Import the Google Play Services library project (available for download through the SDK manager).
   Make sure that the sample is REFERENCING the library project (Project Properties | Android | References)
1. Import `eclipse_compat/libraries/BaseGameUtils` AS A LIBRARY
   Make sure that the sample is REFERENCING the library project (Project Properties | Android | References)

Now jump to the *Modify IDs, compile and run* section and continue to follow the instructions there.

<h3>If you're using Android Studio...</h3>

1. Open Android Studio and launch the Android SDK manager from it (Tools | Android | SDK Manager)
1. Check that these two components are installed. Install them if they are not installed yet.
   1. *Google Play Services*
   1. *Google Play Services Repository*
1. Return to Android Studio and select *Import Project*
1. Select the **BasicSamples** directory
1. Select "Import from existing model - Gradle"

<h3>Modify IDs, compile and run</h3>

To set up a sample:

1. Change the package name from com.google.example.games.\* to your own package name
   (the same one you registered in Developer Console!). To do that, open **AndroidManifest.xml** and put
   your package name in the "package" attribute of the **manifest** tag. You will need to
   fix some of the references (particularly to the generated R class) because of the package name
   change. Ctrl+Shift+O in Eclipse (and Alt+Enter in Android Studio) should take care of most of the work.
1. Modify res/values/ids.xml and place your IDs there, as given by the
   Developer Console (create the leaderboards and achievements necessary for
   the sample, if any). Remember that the App ID is only the *numerical* portion
   of your client ID, so use `123456789012` and not `123456789012.apps.gooogleusercontent.com`.
1. Compile and run.

IMPORTANT: make sure to sign your apk with the same certificate
as the one whose fingerprint you configured on Developer Console, otherwise
you will see errors.

IMPORTANT: if you are testing an unpublished game, make sure that the account you intend
to sign in with (the account on the test device) is listed as a tester in the
project on your Developer Console setup (check the list in the "Testing"
section), otherwise the server will act as though your project did not exist and
return errors.

<h3>If you're using another build system...</h3>

If you are using your own build system, here is a summary of what you must do:

1. Configure it to treat **google-play-services_lib** and **BaseGameUtils** as library projects, which means that not only their code but also their resources will also get added to the final build.
1. Make sure **TrivialQuest** depends on **BaseGameUtils**
1. Make sure **BaseGameUtils** depends on **google-play-services_lib**.
1. Make sure the build system is signing the APK with the right certificate (the one whose fingerprint you provided in the Developer Console when creating your client ID)

<h3>Building</h3>
To build the samples after you have applied the changes above, you can use the build/run option in
Eclipse or Android Studio, or build directly from the command line if you prefer:

    cd /path/to/BasicSamples
    ./gradlew build

<h2>Support</h2>

First of all, take a look at our [troubleshooting guide](https://developers.google.com/games/services/android/troubleshooting). Most setup issues can be solved by following this guide.

If your question is not answered by the troubleshooting guide, we encourage you to post your question to [stackoverflow.com](stackoverflow.com). Our team answers questions there reguarly.

*Samples written by [Bruno Oliveira](http://plus.google.com/+BrunoOliveira).* Feel free to add me to your circles on Google Plus and pester me to fix anything that's broken or answer a question on stackoverflow!

