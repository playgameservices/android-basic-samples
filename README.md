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

2. Create leaderboards/achievements as appropriate for the sample
   (see the ones that the sample needs in its res/values/ids.xml)

3. Start Eclipse
4. Import the desired sample (Project | Import | Android | Existing Android Source)
5. Import the Google Play Services library project (available for download through the SDK manager).
   Make sure that the sample is REFERENCING the library project (Project Properties | Android | References)
6. Import BaseGameUtils AS A LIBRARY
   Make sure that the sample is REFERENCING the library project (Project Properties | Android | References)
7. Change the package name from com.google.example.games.* to your own package name
   (the same one you registered in Developer Console!). To do that, open AndroidManifest.xml and put
   your package name in the "package" attribute of the <manifest> tag. You will need to
   fix some of the references (particularly to the generated R class) because of the package name
   change. Ctrl+Shift+O in Eclipse should take care of most of the work.
8. Modify res/values/ids.xml and place your IDs there, as given by the
   Developer Console (create the leaderboards and achievements necessary for
   the sample, if any).
9. Compile and run.

IMPORTANT: make sure to sign your apk with the same certificate
as the one whose fingerprint you configured on Developer Console, otherwise
you will see errors.

IMPORTANT: if you are testing an unpublished game, make sure you're signing in with 
an account that's listed as a tester in the project on Developer Console,
otherwise the server will act as though your project did not exist.

Note: BaseGameUtils doesn't have any resources, so it doesn't have a res/ directory. If your build system seems unhappy about this, you can just create an empty res/ directory.
