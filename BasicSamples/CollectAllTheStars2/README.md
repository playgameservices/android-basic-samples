# Collect all the Stars 2 #
This sample demonstrates how to use the Snapshots feature to save game data.
It signs the user into Google Play Game Services ("GPGS"), synchronizes his or
her data from a named Snapshot, and then updates the UI to reflect the game
state saved in the Snapshot.

## Setup ##
1. Update the package name in `AndroidManifest.xml` to be something other
   than `com.google.example.games.catt2`.
2. Modify your app folder structure to match your package name.
3. Replace the package name in `MainActivity.java`. Note that the package name
   must be unique; otherwise, the API console cannot link your app correctly.
4. Create your project in the Google Play Developer Console and
   set **Saved Games** to **ON**.
5. Link your app.
6. Copy the `APP ID` from the console into `CollectAllTheStars/values/ids.xml`.

## Running ##
1. Sign in. Upon success, the Play Games toast appears.
2. Make changes to the game state by selecting stars.
3. Sign out. The game UI resets.
4. Sign back in with your account. The game UI reflects the saved game state.

## Troubleshooting ##
If you are encountering errors such as:

    {statusCode=INTERNAL_ERROR, resolution=null}

You may need to manually enable the Drive API from the
[Google API Console] (https://console.developers.google.com/) or may need to
enable the saved games feature from the
[Play Games Developer Console](https://play.google.com/apps/publish).
