Google Play game services - Android Samples
===========================================
Copyright (C) 2014 Google Inc.

<h2>Contents</h2>

These are the Android samples for Google Play game services.

* **BasicSamples** - a set of basic samples, including a convenience library (BaseGameUtils):

    * **BaseGameUtils**. Utilities used on all samples, which you can use in your projects too. This is not a stand-alone sample, it's a library project.

    * **ButtonClicker2000**. Represents the new generation in modern button-clicking excitement. A simple multiplayer game sample that shows how to set up the Google Play real-time multiplayer API, invite friends, automatch, accept invitations, use the waiting room UI, send and receive messages and other multiplayer topics.

    * **CollectAllTheStars2**. Demonstrates how to use the Snapshots feature to save game data. The sample signs the user in, synchronizes their data from a named Snapshot, then updates the UI to reflect the game state saved in the Snapshot.

    * **TrivialQuest2**. Demonstrates how to use the Events and Quests features of Google Play Services. The sample presents a sign in button and four buttons to simulate killing monsters in-game. When you click the buttons, an event is
created and sent to Google Play Games to track what the player is doing in game.

    * **TrivialQuest**. The simplest possible single-player game. Shows how to sign in and how to unlock one achievement. Sign-in and click the button to win the game. Are you ready for this epic adventure?

    * **TypeANumber**. Shows leaderboards and achievements. In this exciting game, you type the score you think you deserve. But wait! There is a twist. If you are playing in easy mode, you get the score you requested. However, if you are playing in hard mode, you only get half! (tough game, we know).

   * **SkeletonTbmp** A trivial turn-based-multiplayer game.  In this thrilling game, you can invite many friends, then send a shared gamestate string back and forth until someone finishes, cancels, or the second-to-last player leaves.

   * **BeGenerous** Send gifts and game requests to other players of BeGenerous.

   * **SavedGames**. Demonstrates the used of Saved Games (Snapshots) feature and how to migrate data from the older Cloud Save (AppState) service to the newer service.  The sample allows the user to save/load data from both Cloud Save and Saved Games.

**Note:** the samples that have corresponding counterparts for iOS and web (particularly, CollectAllTheStars and TypeANumber) are compatible across the platforms. This means that you can play some levels on CollectAllTheStars on your Android device, and then pick up your iOS device and continue where you left off! For TypeANumber, you will see your achievements and leaderboards on all platforms, and progress obtained on one will be reflected on the others.

<h2>Frequently Asked Questions</h2>

If you have questions about the samples (particularly, about *BaseGameActivity* and *GameHelper*), please
take a look at [our FAQ](https://github.com/playgameservices/android-samples/blob/master/FAQ.txt).

<h2>How to run a sample</h2>

1. Set up the project in Developer Console. For more info:

      https://developers.google.com/games/services/console/enabling

   Note your package name and the APP ID of the project.

1. Create leaderboards/achievements as appropriate for the sample (see the ones that the sample needs in its res/values/ids.xml).  You can do this automatically by clicking the link below for the sample you want to configure:
   1. [Type a Number](http://playgameservices.github.io/android-basic-samples/config-magic/index.html?sample=typeanumber)
   1. [Trivial Quest](http://playgameservices.github.io/android-basic-samples/config-magic/index.html?sample=trivialquest)

Pick a set of instructions below depending on whether you're using Eclipse or Android Studio.

<h3>If you're using Eclipse...</h3>

1. From the command line run Scripts/make_eclipse_compat (or Scripts/make_eclipse_compat.cmd on Windows).  This creates the directory structure needed to import the projects correctly.
1. Start Eclipse
1. Import the Google Play Services library project (available for download through the SDK manager):
    1. Click **File | Import | Android | Existing Android Code into Workspace**
    1. Select `SDK/extras/google/libproject/google_play_services/google_play_services_lib` (where `SDK` stands for the path where you installed your Android SDK)
    1. Click **Finish**
1. Import `eclipse_compat/libraries/BaseGameUtils` **as a library**
    1. Click **File | Import | Android | Existing Android Code into Workspace**
    1. Select the `eclipse_compat/libraries/BaseGameUtils` project
    1. Click **Finish**
    1. Right-click on `BaseGameUtils`, then click **Properties**
    1. In the project properties window, click the **Android** section
    1. Check the **Is Library** checkbox
    1. Add a reference to the `google_play_services_lib` project (click **Remove** to remove any broken references, then click **Add** to add the correct one)
1. Import the desired sample from the `eclipse_compat` directory (Project | Import | Android | Existing Android Source)
1. Go into the project properties window for that project (right-click, **Properties**) and check that this project has a reference to the `BaseGameUtils` project.

Your project should now compile. However, don't run it yet, since you still need to adjust your game's IDs
in order for the sample(s) to work.

Now jump to the *Modify IDs, compile and run* section and continue to follow the instructions there.

<h3>If you're using Android Studio...</h3>

1. Open Android Studio and launch the Android SDK manager from it (Tools | Android | SDK Manager)
1. Check that these two components are installed and updated to the latest version. Install or upgrade
   them if necessary.
   1. *Google Play Services*
   1. *Google Repository*
1. Return to Android Studio and select *Import Project*
1. Select the **BasicSamples** directory
1. Select "Import from existing model - Gradle"

<h3>Modify IDs, compile and run</h3>

To set up a sample:

1. Change the application id in the build.gradle file to your own package name
   (the same one you registered in Developer Console!).  You will have to update
   the build.gradle file for each sample you want to run.  There is no need to
   edit the AndroidManifest.xml file.
1. Modify res/values/ids.xml and place your IDs there, as given by the
   Developer Console (create the leaderboards and achievements necessary for
   the sample, if any). In the Developer console, select a resource type
   (Achievements, Events, Leaderboards) and click "Get Resources".  Copy the
    contents from the console and replace the contents of res/values/ids.xml.
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

*Samples written by [Bruno Oliveira](http://plus.google.com/+BrunoOliveira) with contributions from [Wolff](http://plus.google.com/+WolffDobson).* Feel free to add us to your circles on Google Plus and pester us to fix stuff that's broken or answer a question on stackoverflow :-)

*Samples written by [Bruno Oliveira](http://plus.google.com/+BrunoOliveira).* Feel free to add me to your circles on Google Plus and pester me to fix anything that's broken or answer a question on stackoverflow!

<h2>Special Thanks</h2>

* To [ligi](http://github.com/ligi) for contributing the initial Gradle build files
* To [grantkemp](https://github.com/grantkemp) for fixing the Eclipse compat build when we migrated to Gradle
* To [bechhansen](https://github.com/bechhansen) for fixing a bug in GameHelper where the turn-based match was being lost when a non-Games client connected.
