# Collect all the Stars 2 #
This sample demonstrates how to use the Snapshots feature to save game data.
It signs the user into Google Play Game Services, synchronizes his or her data from a named Snapshot,
and then updates the UI to reflect the game state saved in the Snapshot.

## Setup ##
Follow these steps to set up the sample:
1. Update the package name in `AndroidManifest.xml` to be something other
   than `com.google.example.games.catt2`.<br>
2. Modify your app folder structure to match your package name.
3. Replace the package name in `MainActivity.java`. Note that the package name must be unique; otherwise, the API console
   will not be able to link your app correctly.
4. Create your project in the Google Play Games console.
5. Link your app
6. Copy the APP ID from the console into `CollectAllTheStars/values/ids.xml`.

## Running ##
1. Sign in. Upon success, the Play Games toast appears.
2. Select stars to make changes to the game state.
3. Sign out. The game UI resets.
4. Sign back in with your account. The game UI reflects the saved game state.
