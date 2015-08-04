# SavedGames
This sample demonstrates how to migrate data from the Cloud Save (AppState) service to the Saved
Games (Snapshot) service.

**Note**: this sample requires the Cloud Save (AppState) API to work correctly.  However, the Cloud Save API was recently deprecated and can no longer be enabled for new projects.  This means that if you do not have an older project with Cloud Save enabled, you will not be able to properly configure this sample.  In that case, you should use the sample as a code reference only, rather than a runnable demonstration.

## Background
All developers should use the Saved Games API in place of the old Cloud Save API.  Here is a
feature comparison of the two services:

* Saved Games can store unlimited Snapshots per user with up to 3MB of raw data and 800KB of
metadata each, up to the quota of the user's Google Drive storage.  Cloud Save can only store 4
AppState saves per user with a max size of 256KB each.
* Saved Games separates data from metadata and provides a rich set of standard metadata
attributes such as description, total play time, and a cover image for each Snapshot.
Cloud Save stores only raw data.
* Saved Games has a built-in UI with Material Design that can be launched with an
`Intent` to allow users to select, create, and delete Snapshots.  Cloud Save requires
the developer to build a custom selection UI for each app.

## Setup
Follow these steps to set up the sample:

1. Update the package name in `AndroidManifest.xml` to be something other than
`com.google.example.games.savedgames`.  The package name must be unique; otherwise,
the API console cannot link your app correctly.
1. Create your project in the Google Play Developer Console.
1. Enable **Saved Games** on the Game Details page when creating your app in order to enable the
Snapshots API.
1. Link your app.
1. Copy the `APP_ID` from the console into `values/ids.xml`.
1. If you are using Android Studio, resync gradle and build the SavedGames module.

## Requirements
If you are migrating an app from Cloud Save to Saved Games, there are a few things you will need to do
before you can begin using the Snapshots API:

* Snapshots are stored in the user's Google Drive, so you will need to use the Drive API (`Drive.API`)
along with the App Folder Scope (`Drive.SCOPE_APPFOLDER)` when creating your `GoogleApiClient`.
* In the Google Play Developers Console, make sure to enable the Saved Games feature on your app
in the Game Details tab.  If you do not enable this feature, your app will crash when attempting to
access the Snapshots API.

## Running the Sample
1. Press the **Run** button in Android Studio and select a target device.  When the app starts, the
Play Games UI appears.

## Migrating Data from Cloud Save to Saved Games
1. Sign in to the app.
1. Create some AppState data by entering some text into the text field at the top of the screen
and then pressing the **Update** button under **Cloud Save Actions**.  Then click the **Load**
button under Cloud Save Actions to ensure that the data was correctly saved.
1. Migrate the data from AppState to Snapshots by pressing the **Migrate** button under **Cloud Save
Actions**.  Then press the **Load** button under **Saved Games Actions** to ensure that the data
was correctly migrated.
1. Once the data has been migrated from Cloud Save to Saved Games, explore the actions under
**Saved Games Actions** to view and edit the saved data.

## Next Steps
For a complete overview of Saved Games on Android and a comparison to the Cloud Save API, see the
[Saved Games in Android](https://developers.google.com/games/services/android/savedgames) page.
