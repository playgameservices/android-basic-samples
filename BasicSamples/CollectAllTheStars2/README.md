# Collect all the Stars 2 #
This sample demonstrates how to use the Snapshots feature to save game data.
The sample signs the user in, synchronizes their data from a named Snapshot,
then updates the UI to reflect the game state saved in the Snapshot.

## Setup ##
The following steps must be followed to setup the sample:
1. First, update the package name in AndroidManifest.xml to be something other
   than `com.google.example.games.catt2`. Next, modify your app folder
   structure to match your package name. Finally, replace the package name in
   MainActivity.java.  Note the package name must be unique or the API console
   will not correctly be able to link your app.
2. Create your project in the Google Play Games console. After you link your
   app, copy the APP ID from the console into CollectAllTheStars/values/ids.xml.

## Running ##
1. Sign in, upon success, you will see the Play Games toast.
2. Make changes to the game state by selecting stars.
3. Sign out, the game UI should reset.
4. Sign back in with your account and the game UI should reflect the saved game
   state.
