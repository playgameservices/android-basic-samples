== The Play Games Hide-O-matic!

This utility will allow you to hide and and unhide players from public
leaderboards on Google Play Games.

Hiding a player from a leaderboard is an abuse counter-measure you can
take when you detect that a player's score is not legitimate. When you
hide a player from a leaderboard, the player will still be able to see
their own score, but other players will no longer see that player in
public leaderboards.

More information at:

https://developers.google.com/games/services/management/api/index

== Setup and usage

Open index.html in your browser and follow the directions.  

To summarize:

   * Replace CLIENT_ID, APP_ID, and LEADERBOARD_ID in these two files
with appropriate values from the Developer Console.

   * Reload index.html and log in with the app owner's account (or
account with owner permissions).

   * Hide or unhide players by playerId.

Warning:  If you hide a player, you will not be able to retrieve their
player ID again, so keep a log of who you hide.

Last edited: 2013/9/13