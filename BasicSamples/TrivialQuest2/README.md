# Trivial Quest 2 #
This sample demonstrates how to use the Event-and-Quest feature of Google
Play Game Services. In this sample, the game displays a sign-in button, along with four colored buttons. Clicking each button causes the application to send an event to Google Play Game Services ("GPGS"), enabling GPGS to track the player's progress toward a milestone.

When the player reaches a milestone specified in a Quest, the game receives a callback with an object describing the Quest reward.

## Setup ##
Follow these steps to set up the sample:<br>
1. Update the package name in `AndroidManifest.xml` to be something other
   than `com.google.example.games.tq2`.<br>
2. Modify your app folder structure to match your package name.<br>
3. Replace the package name in `MainActivity.java`.  The package name must be unique; otherwise, the API console
   cannot link your app correctly.<br>
4. Create your project in the Google Play Developer Console.<br>
5. Link your app.<br>
6. Copy the `APP ID` from the console into `TrivialQuest2/values/ids.xml`.<br>
7. Set up four events in the console. As you create each event,
   copy its identifier into `TrivialQuest2/values/ids.xml`.<br>
8. If you are using Android Studio, resync gradle, and build the TrivialQuest 2
   module.<br>

## Running the Sample ##
1. Press the **Run** button in Android Studio, and select a target device. When the app starts, the Play Games UI appears.
2. Sign in to the app. The application toasts a message with the events and their current counts.
3. In the application, click the **Quests** button to show current quests.

## Creating and completing Quests ##
1. Create a Quest in the Google Play Developer Console, setting a date range that includes the current date.
2. Accept the Quest in the app.
3. Complete the Quest.
4. A toast appears showing the Quest reward (as JSON data, for example).
