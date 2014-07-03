# Trivial Quest 2 #
This sample demonstrates how to use the Events and Quests features of Google
Play Services. The sample presents a sign in button and four buttons to
simulate killing monsters in-game. When you click the buttons, an event is
created and sent to Google Play Games to track what the player is doing in game.

When milestones, specified in Quests, are reached in the game, the game will
receive a callback with an object describing the Quest reward.

## Setup ##
The following steps must be followed to setup the sample:
1. First, update the package name in AndroidManifest.xml to be something other
   than `com.google.example.games.tq2`. Next, modify your app folder
   structure to match your package name. Finally, replace the package name in
   MainActivity.java.  Note the package name must be unique or the API console
   will not correctly be able to link your app.
2. Create your project in the Google Play Games console. After you link your
   app, copy the APP ID from the console into TrivialQuest2/values/ids.xml.
3. Setup four events in the Play Games console. After you create each event,
   copy the event identifier into TrivialQuest2/values/ids.xml.
4. If you are using Android Studio, resync gradle and build the TrivialQuest 2
   module.

## Running the Sample ##
1. Press the run button in Android Studio and select a target device.
2. When the app starts, you will be presented with the Play Games UI and can
   sign in to the app.
3. After you sign in to the app, the application will Toast a message with the
   events and their current counts.
4. In the application, you can click the quests button to show current quests.

## Creating and completing Quests ##
1. Create a Quest in the Google Play Games console and set the date to include
   the current date.
2. Accept the Quest in the app.
3. Complete the Quest.
4. A toast will appear with the Quest reward (e.g. JSON data).
