Google Play game services - Android Samples
===========================================

Copyright (C) 2013 Google Inc.

To run a sample:

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

