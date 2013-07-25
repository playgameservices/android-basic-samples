Google Play game services - Android Samples
===========================================
Copyright (C) 2013 Google Inc.

<h2>Contents</h2>

These are the Android samples for Google Play game services.

<b>BaseGameUtils</b> - utilities used on all samples, which you can use in your projects too! This is not a stand-alone sample, it's a library project.

<b>ButtonClicker2000:</b> Represents the new generation in modern button-clicking excitement. A simple multiplayer game sample that shows how to set up the Google Play real-time multiplayer API, invite friends, automatch, accept invitations, use the waiting room UI, send and receive messages and other multiplayer topics.

<b>CollectAllTheStars:</b> Demonstrates a typical use of cloud save. In this challenging game, there are 20 worlds of 12 levels each. When you click on a level, it will ask you how many stars you think you deserve on it. Honesty required! This sample demonstrates how to deal with cloud save conflicts (for example, if you play some levels on your phone and a different set of levels on your tablet).

<b>TrivialQuest:</b> The simplest possible single-player game. Shows how to sign in and how to unlock one achievement. Sign-in and click the button to win the game. Are you ready for this epic adventure?

<b>TypeANumber:</b> Shows leaderboards and achievements. In this exciting game, you type the score you think you deserve. But wait! There is a twist. If you are playing in easy mode, you get the score you requested. However, if you are playing in hard mode, you only get half! (tough game, we know).

Note: the samples that have corresponding counterparts for iOS and web (particularly, CollectAllTheStars and TypeANumber) are compatible across the platforms. This means that you can play some levels on CollectAllTheStars on your Android device, and then pick up your iOS device and continue where you left off! For TypeANumber, you will see your achievements and leaderboards on all platforms, and progress obtained on one will be reflected on the others.

<h2>How to run a sample</h2>

1. Set up the project in Developer Console. For more info:

      http://developers.google.com/games/services
 
   Note your package name and the APP ID of the project.

1. Create leaderboards/achievements as appropriate for the sample
   (see the ones that the sample needs in its res/values/ids.xml)

Pick a set of instructions below depending on whether you're using Eclipse or Android Studio.

<h3>If you're using Eclipse...</h3>

1. Start Eclipse
1. Import the desired sample (Project | Import | Android | Existing Android Source)
1. Import the Google Play Services library project (available for download through the SDK manager).
   Make sure that the sample is REFERENCING the library project (Project Properties | Android | References)
1. Import libraries/BaseGameUtils AS A LIBRARY
   Make sure that the sample is REFERENCING the library project (Project Properties | Android | References)

Now jump to the *Modify IDs, compile and run* section and continue to follow the instructions there.

<h3>If you're using Android Studio...</h3>

1. Open Android Studio and launch the Android SDK manager from it (Tools | Android | SDK Manager)
1. Check that these two components are installed. Install them if they are not installed yet.
   1. *Google Play Services*
   1. *Google Play Services Repository*
1. Go to the `build_scripts` directory and run the `copy_gradle_to_prj.sh` to copy the Gradle build files to the appropriate places in the directory tree.
1. Launch Android Studio
1. Select *Open Project* (do not select "Import Project").
1. Select the top-level directory (the one that contains `README.md`).

If this process does not work, you can also try importing the sources manually as described below.

<h3>Importing manually in Android Studio...</h3>

*Note: If you already have some experience with Android Studio, please forgive the verbosity of these instructions... since Android Studio is relatively new at the time of this writing, these instructions assume you're a newbie and try to be as specific as possible.*

1. Open Android Studio
1. Make sure you're running the latest version of Android Studio. As of this writing, Android Studio is in alpha and evolving rapidly, which means that updating it to the latest version might just fix that mysterious bug that's blocking you :-)
1. Select **File | Import project**
1. Select the Google Play Services library project. It's usually a directory called `google-play-services_lib` in your SDK's `extras/google` directory.
1. Select the **Create project from existing sources** option, when asked.
1. When asked which source directories to use, leave all of them selected (`/`, `/gen` and `/src`).
1. When asked which library to use, select *only* the ones in the `libs` directory.
1. Finish the import process.
1. Click on the newly created module and press `F4` to bring up the module properties.
1. Select **google-play-services_lib** in the middle pane, expand it and go its **Android** sub-item
1. Check the **Library Module** checkbox (very important!)
1. Next, click **File | Import Module** (don't confuse with **Import Project**)
1. Select the `libraries/BaseGameUtils` directory from the samples.
1. Import it exactly the same way you imported the other project just now (steps 5 to 11). Did you remember to check the **Library Module** checkbox?
1. Now you should have two modules: `google-play-services_lib` and `BaseGameUtils`
1. Click **File | Import Module** one more time.
1. Select the directory of the sample you wish to import. For example, `TrivialQuest`.
1. Import it as in steps 5 to 11, but **don't** check the **Library Module** checkbox.
1. Select the `BaseGameUtils` module, and press `F4`
1. In the **Dependencies** tab, click the green plus icon to add a new dependency. Select **Module Dependency** in the popup menu.
1. Select **google-play-services_lib** in the box that appears.
1. Now you are back to the **Dependencies** tab. Now, check the **Export** checkbox to the left of the newly added **google-play-services_lib** item.
1. Look at the leftmost list in this window, under **Project Settings**. Click on the **Libraries** item.
1. Make sure you see **google-play-services** and **android-support-v4** in the middle pane. If not, something went wrong.
1. Now, go back to the **Modules** pane (click on the **Modules** item in the leftmost list box).
1. Select **BaseGameUtils** in the middle pane.
1. Click the green plus icon to add a library. Select **Library** in the popup menu.
1. Select the **google-play-services** library in the box that appears.
1. Click the green plus icon to add a library. Select **Library** in the popup menu.
1. Select the **android-support-v4** library in the box that appears.
1. Check the **Export** checkbox next to **android-support-v4**
1. Check the **Export** checkbox next to **google-play-services_lib**
1. Check the **Export** checkbox next to **google-play-services**
1. Select **TrivialQuest** in the middle pane.
1. Click the green plus icon to add a dependency. Select **Module dependency** in the popup menu.
1. Select the **BaseGameUtils** module.
1. *Don't* check the **Export*** checkbox.
1. Close this window.

Continue below.

<h3>Modify IDs, compile and run</h3>

1. Change the package name from com.google.example.games.* to your own package name
   (the same one you registered in Developer Console!). To do that, open AndroidManifest.xml and put
   your package name in the "package" attribute of the <manifest> tag. You will need to
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

IMPORTANT: if you are testing an unpublished game, make sure you're signing in with 
an account that's listed as a tester in the project on Developer Console,
otherwise the server will act as though your project did not exist.

<h3>If you're using another build system...</h3>

If you are using your own build system, here is a summary of what you have to do:

1. Configure it to treat **google-play-services_lib** and **BaseGameUtils** as library projects, which means that not only their code but also their resources will also get added to the final build.
1. Make sure **TrivialQuest** depends on **BaseGameUtils**
1. Make sure **BaseGameUtils** depends on **google-play-services_lib**.
1. Make sure the build system is signing the APK with the right certificate (the one whose fingerprint you provided in the Developer Console when creating your client ID)

<h2>Support</h2>

First of all, take a look at our (hopefully) thorough [troubleshooting guide](https://developers.google.com/games/services/android/troubleshooting). In our experience, *most* setup issues can be solved by following this guide.

If your question is not answered by the troubleshooting guide, we encourage you to post your question to [stackoverflow.com](stackoverflow.com). Our team answers questions there reguarly.

*Samples written by [Bruno Oliveira](http://plus.google.com/+BrunoOliveira).* Feel free to add me to your circles on Google Plus and pester me to fix stuff that's broken or answer a question on stackoverflow :-)
