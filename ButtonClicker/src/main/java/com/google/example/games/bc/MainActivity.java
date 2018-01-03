/* Copyright (C) 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.example.games.bc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.GamesCallbackStatusCodes;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.GamesClientStatusCodes;
import com.google.android.gms.games.InvitationsClient;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.PlayersClient;
import com.google.android.gms.games.RealTimeMultiplayerClient;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.InvitationCallback;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.OnRealTimeMessageReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessage;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.realtime.RoomStatusUpdateCallback;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateCallback;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Button Clicker 2000. A minimalistic game showing the multiplayer features of
 * the Google Play game services API. The objective of this game is clicking a
 * button. Whoever clicks the button the most times within a 20 second interval
 * wins. It's that simple. This game can be played with 2, 3 or 4 players. The
 * code is organized in sections in order to make understanding as clear as
 * possible. We start with the integration section where we show how the game
 * is integrated with the Google Play game services API, then move on to
 * game-specific UI and logic.
 * <p>
 * INSTRUCTIONS: To run this sample, please set up
 * a project in the Developer Console. Then, place your app ID on
 * res/values/ids.xml. Also, change the package name to the package name you
 * used to create the client ID in Developer Console. Make sure you sign the
 * APK with the certificate whose fingerprint you entered in Developer Console
 * when creating your Client Id.
 *
 * @author Bruno Oliveira (btco), 2013-04-26
 */
public class MainActivity extends Activity implements
    View.OnClickListener {

    /*
     * API INTEGRATION SECTION. This section contains the code that integrates
     * the game with the Google Play game services API.
     */

  final static String TAG = "ButtonClicker2000";

  // Request codes for the UIs that we show with startActivityForResult:
  final static int RC_SELECT_PLAYERS = 10000;
  final static int RC_INVITATION_INBOX = 10001;
  final static int RC_WAITING_ROOM = 10002;

  // Request code used to invoke sign in user interactions.
  private static final int RC_SIGN_IN = 9001;

  // Client used to sign in with Google APIs
  private GoogleSignInClient mGoogleSignInClient = null;

  // Client used to interact with the real time multiplayer system.
  private RealTimeMultiplayerClient mRealTimeMultiplayerClient = null;

  // Client used to interact with the Invitation system.
  private InvitationsClient mInvitationsClient = null;

  // Room ID where the currently active game is taking place; null if we're
  // not playing.
  String mRoomId = null;

  // Holds the configuration of the current room.
  RoomConfig mRoomConfig;

  // Are we playing in multiplayer mode?
  boolean mMultiplayer = false;

  // The participants in the currently active game
  ArrayList<Participant> mParticipants = null;

  // My participant ID in the currently active game
  String mMyId = null;

  // If non-null, this is the id of the invitation we received via the
  // invitation listener
  String mIncomingInvitationId = null;

  // Message buffer for sending messages
  byte[] mMsgBuf = new byte[2];

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // Create the client used to sign in.
    mGoogleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN);

    // set up a click listener for everything we care about
    for (int id : CLICKABLES) {
      findViewById(id).setOnClickListener(this);
    }

    switchToMainScreen();
    checkPlaceholderIds();
  }

  // Check the sample to ensure all placeholder ids are are updated with real-world values.
  // This is strictly for the purpose of the samples; you don't need this in a production
  // application.
  private void checkPlaceholderIds() {
    StringBuilder problems = new StringBuilder();

    if (getPackageName().startsWith("com.google.")) {
      problems.append("- Package name start with com.google.*\n");
    }

    for (Integer id : new Integer[]{R.string.app_id}) {

      String value = getString(id);

      if (value.startsWith("YOUR_")) {
        // needs replacing
        problems.append("- Placeholders(YOUR_*) in ids.xml need updating\n");
        break;
      }
    }

    if (problems.length() > 0) {
      problems.insert(0, "The following problems were found:\n\n");

      problems.append("\nThese problems may prevent the app from working properly.");
      problems.append("\n\nSee the TODO window in Android Studio for more information");
      (new AlertDialog.Builder(this)).setMessage(problems.toString())
          .setNeutralButton(android.R.string.ok, null).create().show();
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    Log.d(TAG, "onResume()");

    // Since the state of the signed in user can change when the activity is not active
    // it is recommended to try and sign in silently from when the app resumes.
    signInSilently();
  }

  @Override
  protected void onPause() {
    super.onPause();

    // unregister our listeners.  They will be re-registered via onResume->signInSilently->onConnected.
    if (mInvitationsClient != null) {
      mInvitationsClient.unregisterInvitationCallback(mInvitationCallback);
    }
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.button_single_player:
      case R.id.button_single_player_2:
        // play a single-player game
        resetGameVars();
        startGame(false);
        break;
      case R.id.button_sign_in:
        // start the sign-in flow
        Log.d(TAG, "Sign-in button clicked");
        startSignInIntent();
        break;
      case R.id.button_sign_out:
        // user wants to sign out
        // sign out.
        Log.d(TAG, "Sign-out button clicked");
        signOut();
        switchToScreen(R.id.screen_sign_in);
        break;
      case R.id.button_invite_players:
        switchToScreen(R.id.screen_wait);

        // show list of invitable players
        mRealTimeMultiplayerClient.getSelectOpponentsIntent(1, 3).addOnSuccessListener(
            new OnSuccessListener<Intent>() {
              @Override
              public void onSuccess(Intent intent) {
                startActivityForResult(intent, RC_SELECT_PLAYERS);
              }
            }
        ).addOnFailureListener(createFailureListener("There was a problem selecting opponents."));
        break;
      case R.id.button_see_invitations:
        switchToScreen(R.id.screen_wait);

        // show list of pending invitations
        mInvitationsClient.getInvitationInboxIntent().addOnSuccessListener(
            new OnSuccessListener<Intent>() {
              @Override
              public void onSuccess(Intent intent) {
                startActivityForResult(intent, RC_INVITATION_INBOX);
              }
            }
        ).addOnFailureListener(createFailureListener("There was a problem getting the inbox."));
        break;
      case R.id.button_accept_popup_invitation:
        // user wants to accept the invitation shown on the invitation popup
        // (the one we got through the OnInvitationReceivedListener).
        acceptInviteToRoom(mIncomingInvitationId);
        mIncomingInvitationId = null;
        break;
      case R.id.button_quick_game:
        // user wants to play against a random opponent right now
        startQuickGame();
        break;
      case R.id.button_click_me:
        // (gameplay) user clicked the "click me" button
        scoreOnePoint();
        break;
    }
  }

  void startQuickGame() {
    // quick-start a game with 1 randomly selected opponent
    final int MIN_OPPONENTS = 1, MAX_OPPONENTS = 1;
    Bundle autoMatchCriteria = RoomConfig.createAutoMatchCriteria(MIN_OPPONENTS,
        MAX_OPPONENTS, 0);
    switchToScreen(R.id.screen_wait);
    keepScreenOn();
    resetGameVars();

    mRoomConfig = RoomConfig.builder(mRoomUpdateCallback)
        .setOnMessageReceivedListener(mOnRealTimeMessageReceivedListener)
        .setRoomStatusUpdateCallback(mRoomStatusUpdateCallback)
        .setAutoMatchCriteria(autoMatchCriteria)
        .build();
    mRealTimeMultiplayerClient.create(mRoomConfig);
  }

  /**
   * Start a sign in activity.  To properly handle the result, call tryHandleSignInResult from
   * your Activity's onActivityResult function
   */
  public void startSignInIntent() {
    startActivityForResult(mGoogleSignInClient.getSignInIntent(), RC_SIGN_IN);
  }

  /**
   * Try to sign in without displaying dialogs to the user.
   * <p>
   * If the user has already signed in previously, it will not show dialog.
   */
  public void signInSilently() {
    Log.d(TAG, "signInSilently()");

    mGoogleSignInClient.silentSignIn().addOnCompleteListener(this,
        new OnCompleteListener<GoogleSignInAccount>() {
          @Override
          public void onComplete(@NonNull Task<GoogleSignInAccount> task) {
            if (task.isSuccessful()) {
              Log.d(TAG, "signInSilently(): success");
              onConnected(task.getResult());
            } else {
              Log.d(TAG, "signInSilently(): failure", task.getException());
              onDisconnected();
            }
          }
        });
  }

  public void signOut() {
    Log.d(TAG, "signOut()");

    mGoogleSignInClient.signOut().addOnCompleteListener(this,
        new OnCompleteListener<Void>() {
          @Override
          public void onComplete(@NonNull Task<Void> task) {

            if (task.isSuccessful()) {
              Log.d(TAG, "signOut(): success");
            } else {
              handleException(task.getException(), "signOut() failed!");
            }

            onDisconnected();
          }
        });
  }

  /**
   * Since a lot of the operations use tasks, we can use a common handler for whenever one fails.
   *
   * @param exception The exception to evaluate.  Will try to display a more descriptive reason for the exception.
   * @param details   Will display alongside the exception if you wish to provide more details for why the exception
   *                  happened
   */
  private void handleException(Exception exception, String details) {
    int status = 0;

    if (exception instanceof ApiException) {
      ApiException apiException = (ApiException) exception;
      status = apiException.getStatusCode();
    }

    String errorString = null;
    switch (status) {
      case GamesCallbackStatusCodes.OK:
        break;
      case GamesClientStatusCodes.MULTIPLAYER_ERROR_NOT_TRUSTED_TESTER:
        errorString = getString(R.string.status_multiplayer_error_not_trusted_tester);
        break;
      case GamesClientStatusCodes.MATCH_ERROR_ALREADY_REMATCHED:
        errorString = getString(R.string.match_error_already_rematched);
        break;
      case GamesClientStatusCodes.NETWORK_ERROR_OPERATION_FAILED:
        errorString = getString(R.string.network_error_operation_failed);
        break;
      case GamesClientStatusCodes.INTERNAL_ERROR:
        errorString = getString(R.string.internal_error);
        break;
      case GamesClientStatusCodes.MATCH_ERROR_INACTIVE_MATCH:
        errorString = getString(R.string.match_error_inactive_match);
        break;
      case GamesClientStatusCodes.MATCH_ERROR_LOCALLY_MODIFIED:
        errorString = getString(R.string.match_error_locally_modified);
        break;
      default:
        errorString = getString(R.string.unexpected_status, GamesClientStatusCodes.getStatusCodeString(status));
        break;
    }

    if (errorString == null) {
      return;
    }

    String message = getString(R.string.status_exception_error, details, status, exception);

    new AlertDialog.Builder(MainActivity.this)
        .setTitle("Error")
        .setMessage(message + "\n" + errorString)
        .setNeutralButton(android.R.string.ok, null)
        .show();
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent intent) {

    if (requestCode == RC_SIGN_IN) {

      Task<GoogleSignInAccount> task =
          GoogleSignIn.getSignedInAccountFromIntent(intent);

      try {
        GoogleSignInAccount account = task.getResult(ApiException.class);
        onConnected(account);
      } catch (ApiException apiException) {
        String message = apiException.getMessage();
        if (message == null || message.isEmpty()) {
          message = getString(R.string.signin_other_error);
        }

        onDisconnected();

        new AlertDialog.Builder(this)
            .setMessage(message)
            .setNeutralButton(android.R.string.ok, null)
            .show();
      }
    } else if (requestCode == RC_SELECT_PLAYERS) {
      // we got the result from the "select players" UI -- ready to create the room
      handleSelectPlayersResult(resultCode, intent);

    } else if (requestCode == RC_INVITATION_INBOX) {
      // we got the result from the "select invitation" UI (invitation inbox). We're
      // ready to accept the selected invitation:
      handleInvitationInboxResult(resultCode, intent);

    } else if (requestCode == RC_WAITING_ROOM) {
      // we got the result from the "waiting room" UI.
      if (resultCode == Activity.RESULT_OK) {
        // ready to start playing
        Log.d(TAG, "Starting game (waiting room returned OK).");
        startGame(true);
      } else if (resultCode == GamesActivityResultCodes.RESULT_LEFT_ROOM) {
        // player indicated that they want to leave the room
        leaveRoom();
      } else if (resultCode == Activity.RESULT_CANCELED) {
        // Dialog was cancelled (user pressed back key, for instance). In our game,
        // this means leaving the room too. In more elaborate games, this could mean
        // something else (like minimizing the waiting room UI).
        leaveRoom();
      }
    }
    super.onActivityResult(requestCode, resultCode, intent);
  }

  // Handle the result of the "Select players UI" we launched when the user clicked the
  // "Invite friends" button. We react by creating a room with those players.

  private void handleSelectPlayersResult(int response, Intent data) {
    if (response != Activity.RESULT_OK) {
      Log.w(TAG, "*** select players UI cancelled, " + response);
      switchToMainScreen();
      return;
    }

    Log.d(TAG, "Select players UI succeeded.");

    // get the invitee list
    final ArrayList<String> invitees = data.getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);
    Log.d(TAG, "Invitee count: " + invitees.size());

    // get the automatch criteria
    Bundle autoMatchCriteria = null;
    int minAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MIN_AUTOMATCH_PLAYERS, 0);
    int maxAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MAX_AUTOMATCH_PLAYERS, 0);
    if (minAutoMatchPlayers > 0 || maxAutoMatchPlayers > 0) {
      autoMatchCriteria = RoomConfig.createAutoMatchCriteria(
          minAutoMatchPlayers, maxAutoMatchPlayers, 0);
      Log.d(TAG, "Automatch criteria: " + autoMatchCriteria);
    }

    // create the room
    Log.d(TAG, "Creating room...");
    switchToScreen(R.id.screen_wait);
    keepScreenOn();
    resetGameVars();

    mRoomConfig = RoomConfig.builder(mRoomUpdateCallback)
        .addPlayersToInvite(invitees)
        .setOnMessageReceivedListener(mOnRealTimeMessageReceivedListener)
        .setRoomStatusUpdateCallback(mRoomStatusUpdateCallback)
        .setAutoMatchCriteria(autoMatchCriteria).build();
    mRealTimeMultiplayerClient.create(mRoomConfig);
    Log.d(TAG, "Room created, waiting for it to be ready...");
  }

  // Handle the result of the invitation inbox UI, where the player can pick an invitation
  // to accept. We react by accepting the selected invitation, if any.
  private void handleInvitationInboxResult(int response, Intent data) {
    if (response != Activity.RESULT_OK) {
      Log.w(TAG, "*** invitation inbox UI cancelled, " + response);
      switchToMainScreen();
      return;
    }

    Log.d(TAG, "Invitation inbox UI succeeded.");
    Invitation invitation = data.getExtras().getParcelable(Multiplayer.EXTRA_INVITATION);

    // accept invitation
    if (invitation != null) {
      acceptInviteToRoom(invitation.getInvitationId());
    }
  }

  // Accept the given invitation.
  void acceptInviteToRoom(String invitationId) {
    // accept the invitation
    Log.d(TAG, "Accepting invitation: " + invitationId);

    mRoomConfig = RoomConfig.builder(mRoomUpdateCallback)
        .setInvitationIdToAccept(invitationId)
        .setOnMessageReceivedListener(mOnRealTimeMessageReceivedListener)
        .setRoomStatusUpdateCallback(mRoomStatusUpdateCallback)
        .build();

    switchToScreen(R.id.screen_wait);
    keepScreenOn();
    resetGameVars();

    mRealTimeMultiplayerClient.join(mRoomConfig)
        .addOnSuccessListener(new OnSuccessListener<Void>() {
          @Override
          public void onSuccess(Void aVoid) {
            Log.d(TAG, "Room Joined Successfully!");
          }
        });
  }

  // Activity is going to the background. We have to leave the current room.
  @Override
  public void onStop() {
    Log.d(TAG, "**** got onStop");

    // if we're in a room, leave it.
    leaveRoom();

    // stop trying to keep the screen on
    stopKeepingScreenOn();

    switchToMainScreen();

    super.onStop();
  }

  // Handle back key to make sure we cleanly leave a game if we are in the middle of one
  @Override
  public boolean onKeyDown(int keyCode, KeyEvent e) {
    if (keyCode == KeyEvent.KEYCODE_BACK && mCurScreen == R.id.screen_game) {
      leaveRoom();
      return true;
    }
    return super.onKeyDown(keyCode, e);
  }

  // Leave the room.
  void leaveRoom() {
    Log.d(TAG, "Leaving room.");
    mSecondsLeft = 0;
    stopKeepingScreenOn();
    if (mRoomId != null) {
      mRealTimeMultiplayerClient.leave(mRoomConfig, mRoomId)
          .addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
              mRoomId = null;
              mRoomConfig = null;
            }
          });
      switchToScreen(R.id.screen_wait);
    } else {
      switchToMainScreen();
    }
  }

  // Show the waiting room UI to track the progress of other players as they enter the
  // room and get connected.
  void showWaitingRoom(Room room) {
    // minimum number of players required for our game
    // For simplicity, we require everyone to join the game before we start it
    // (this is signaled by Integer.MAX_VALUE).
    final int MIN_PLAYERS = Integer.MAX_VALUE;
    mRealTimeMultiplayerClient.getWaitingRoomIntent(room, MIN_PLAYERS)
        .addOnSuccessListener(new OnSuccessListener<Intent>() {
          @Override
          public void onSuccess(Intent intent) {
            // show waiting room UI
            startActivityForResult(intent, RC_WAITING_ROOM);
          }
        })
        .addOnFailureListener(createFailureListener("There was a problem getting the waiting room!"));
  }

  private InvitationCallback mInvitationCallback = new InvitationCallback() {
    // Called when we get an invitation to play a game. We react by showing that to the user.
    @Override
    public void onInvitationReceived(@NonNull Invitation invitation) {
      // We got an invitation to play a game! So, store it in
      // mIncomingInvitationId
      // and show the popup on the screen.
      mIncomingInvitationId = invitation.getInvitationId();
      ((TextView) findViewById(R.id.incoming_invitation_text)).setText(
          invitation.getInviter().getDisplayName() + " " +
              getString(R.string.is_inviting_you));
      switchToScreen(mCurScreen); // This will show the invitation popup
    }

    @Override
    public void onInvitationRemoved(@NonNull String invitationId) {

      if (mIncomingInvitationId.equals(invitationId) && mIncomingInvitationId != null) {
        mIncomingInvitationId = null;
        switchToScreen(mCurScreen); // This will hide the invitation popup
      }
    }
  };

    /*
     * CALLBACKS SECTION. This section shows how we implement the several games
     * API callbacks.
     */

  private String mPlayerId;

  // The currently signed in account, used to check the account has changed outside of this activity when resuming.
  GoogleSignInAccount mSignedInAccount = null;

  private void onConnected(GoogleSignInAccount googleSignInAccount) {
    Log.d(TAG, "onConnected(): connected to Google APIs");
    if (mSignedInAccount != googleSignInAccount) {

      mSignedInAccount = googleSignInAccount;

      // update the clients
      mRealTimeMultiplayerClient = Games.getRealTimeMultiplayerClient(this, googleSignInAccount);
      mInvitationsClient = Games.getInvitationsClient(MainActivity.this, googleSignInAccount);

      // get the playerId from the PlayersClient
      PlayersClient playersClient = Games.getPlayersClient(this, googleSignInAccount);
      playersClient.getCurrentPlayer()
          .addOnSuccessListener(new OnSuccessListener<Player>() {
            @Override
            public void onSuccess(Player player) {
              mPlayerId = player.getPlayerId();

              switchToMainScreen();
            }
          })
          .addOnFailureListener(createFailureListener("There was a problem getting the player id!"));
    }

    // register listener so we are notified if we receive an invitation to play
    // while we are in the game
    mInvitationsClient.registerInvitationCallback(mInvitationCallback);

    // get the invitation from the connection hint
    // Retrieve the TurnBasedMatch from the connectionHint
    GamesClient gamesClient = Games.getGamesClient(MainActivity.this, googleSignInAccount);
    gamesClient.getActivationHint()
        .addOnSuccessListener(new OnSuccessListener<Bundle>() {
          @Override
          public void onSuccess(Bundle hint) {
            if (hint != null) {
              Invitation invitation =
                  hint.getParcelable(Multiplayer.EXTRA_INVITATION);

              if (invitation != null && invitation.getInvitationId() != null) {
                // retrieve and cache the invitation ID
                Log.d(TAG, "onConnected: connection hint has a room invite!");
                acceptInviteToRoom(invitation.getInvitationId());
              }
            }
          }
        })
        .addOnFailureListener(createFailureListener("There was a problem getting the activation hint!"));
  }

  private OnFailureListener createFailureListener(final String string) {
    return new OnFailureListener() {
      @Override
      public void onFailure(@NonNull Exception e) {
        handleException(e, string);
      }
    };
  }

  public void onDisconnected() {
    Log.d(TAG, "onDisconnected()");

    mRealTimeMultiplayerClient = null;
    mInvitationsClient = null;

    switchToMainScreen();
  }

  private RoomStatusUpdateCallback mRoomStatusUpdateCallback = new RoomStatusUpdateCallback() {
    // Called when we are connected to the room. We're not ready to play yet! (maybe not everybody
    // is connected yet).
    @Override
    public void onConnectedToRoom(Room room) {
      Log.d(TAG, "onConnectedToRoom.");

      //get participants and my ID:
      mParticipants = room.getParticipants();
      mMyId = room.getParticipantId(mPlayerId);

      // save room ID if its not initialized in onRoomCreated() so we can leave cleanly before the game starts.
      if (mRoomId == null) {
        mRoomId = room.getRoomId();
      }

      // print out the list of participants (for debug purposes)
      Log.d(TAG, "Room ID: " + mRoomId);
      Log.d(TAG, "My ID " + mMyId);
      Log.d(TAG, "<< CONNECTED TO ROOM>>");
    }

    // Called when we get disconnected from the room. We return to the main screen.
    @Override
    public void onDisconnectedFromRoom(Room room) {
      mRoomId = null;
      mRoomConfig = null;
      showGameError();
    }


    // We treat most of the room update callbacks in the same way: we update our list of
    // participants and update the display. In a real game we would also have to check if that
    // change requires some action like removing the corresponding player avatar from the screen,
    // etc.
    @Override
    public void onPeerDeclined(Room room, @NonNull List<String> arg1) {
      updateRoom(room);
    }

    @Override
    public void onPeerInvitedToRoom(Room room, @NonNull List<String> arg1) {
      updateRoom(room);
    }

    @Override
    public void onP2PDisconnected(@NonNull String participant) {
    }

    @Override
    public void onP2PConnected(@NonNull String participant) {
    }

    @Override
    public void onPeerJoined(Room room, @NonNull List<String> arg1) {
      updateRoom(room);
    }

    @Override
    public void onPeerLeft(Room room, @NonNull List<String> peersWhoLeft) {
      updateRoom(room);
    }

    @Override
    public void onRoomAutoMatching(Room room) {
      updateRoom(room);
    }

    @Override
    public void onRoomConnecting(Room room) {
      updateRoom(room);
    }

    @Override
    public void onPeersConnected(Room room, @NonNull List<String> peers) {
      updateRoom(room);
    }

    @Override
    public void onPeersDisconnected(Room room, @NonNull List<String> peers) {
      updateRoom(room);
    }
  };

  // Show error message about game being cancelled and return to main screen.
  void showGameError() {
    new AlertDialog.Builder(this)
        .setMessage(getString(R.string.game_problem))
        .setNeutralButton(android.R.string.ok, null).create();

    switchToMainScreen();
  }

  private RoomUpdateCallback mRoomUpdateCallback = new RoomUpdateCallback() {

    // Called when room has been created
    @Override
    public void onRoomCreated(int statusCode, Room room) {
      Log.d(TAG, "onRoomCreated(" + statusCode + ", " + room + ")");
      if (statusCode != GamesCallbackStatusCodes.OK) {
        Log.e(TAG, "*** Error: onRoomCreated, status " + statusCode);
        showGameError();
        return;
      }

      // save room ID so we can leave cleanly before the game starts.
      mRoomId = room.getRoomId();

      // show the waiting room UI
      showWaitingRoom(room);
    }

    // Called when room is fully connected.
    @Override
    public void onRoomConnected(int statusCode, Room room) {
      Log.d(TAG, "onRoomConnected(" + statusCode + ", " + room + ")");
      if (statusCode != GamesCallbackStatusCodes.OK) {
        Log.e(TAG, "*** Error: onRoomConnected, status " + statusCode);
        showGameError();
        return;
      }
      updateRoom(room);
    }

    @Override
    public void onJoinedRoom(int statusCode, Room room) {
      Log.d(TAG, "onJoinedRoom(" + statusCode + ", " + room + ")");
      if (statusCode != GamesCallbackStatusCodes.OK) {
        Log.e(TAG, "*** Error: onRoomConnected, status " + statusCode);
        showGameError();
        return;
      }

      // show the waiting room UI
      showWaitingRoom(room);
    }

    // Called when we've successfully left the room (this happens a result of voluntarily leaving
    // via a call to leaveRoom(). If we get disconnected, we get onDisconnectedFromRoom()).
    @Override
    public void onLeftRoom(int statusCode, @NonNull String roomId) {
      // we have left the room; return to main screen.
      Log.d(TAG, "onLeftRoom, code " + statusCode);
      switchToMainScreen();
    }
  };

  void updateRoom(Room room) {
    if (room != null) {
      mParticipants = room.getParticipants();
    }
    if (mParticipants != null) {
      updatePeerScoresDisplay();
    }
  }

    /*
     * GAME LOGIC SECTION. Methods that implement the game's rules.
     */

  // Current state of the game:
  int mSecondsLeft = -1; // how long until the game ends (seconds)
  final static int GAME_DURATION = 20; // game duration, seconds.
  int mScore = 0; // user's current score

  // Reset game variables in preparation for a new game.
  void resetGameVars() {
    mSecondsLeft = GAME_DURATION;
    mScore = 0;
    mParticipantScore.clear();
    mFinishedParticipants.clear();
  }

  // Start the gameplay phase of the game.
  void startGame(boolean multiplayer) {
    mMultiplayer = multiplayer;
    updateScoreDisplay();
    broadcastScore(false);
    switchToScreen(R.id.screen_game);

    findViewById(R.id.button_click_me).setVisibility(View.VISIBLE);

    // run the gameTick() method every second to update the game.
    final Handler h = new Handler();
    h.postDelayed(new Runnable() {
      @Override
      public void run() {
        if (mSecondsLeft <= 0) {
          return;
        }
        gameTick();
        h.postDelayed(this, 1000);
      }
    }, 1000);
  }

  // Game tick -- update countdown, check if game ended.
  void gameTick() {
    if (mSecondsLeft > 0) {
      --mSecondsLeft;
    }

    // update countdown
    ((TextView) findViewById(R.id.countdown)).setText("0:" +
        (mSecondsLeft < 10 ? "0" : "") + String.valueOf(mSecondsLeft));

    if (mSecondsLeft <= 0) {
      // finish game
      findViewById(R.id.button_click_me).setVisibility(View.GONE);
      broadcastScore(true);
    }
  }

  // indicates the player scored one point
  void scoreOnePoint() {
    if (mSecondsLeft <= 0) {
      return; // too late!
    }
    ++mScore;
    updateScoreDisplay();
    updatePeerScoresDisplay();

    // broadcast our new score to our peers
    broadcastScore(false);
  }

    /*
     * COMMUNICATIONS SECTION. Methods that implement the game's network
     * protocol.
     */

  // Score of other participants. We update this as we receive their scores
  // from the network.
  Map<String, Integer> mParticipantScore = new HashMap<>();

  // Participants who sent us their final score.
  Set<String> mFinishedParticipants = new HashSet<>();

  // Called when we receive a real-time message from the network.
  // Messages in our game are made up of 2 bytes: the first one is 'F' or 'U'
  // indicating
  // whether it's a final or interim score. The second byte is the score.
  // There is also the
  // 'S' message, which indicates that the game should start.
  OnRealTimeMessageReceivedListener mOnRealTimeMessageReceivedListener = new OnRealTimeMessageReceivedListener() {
    @Override
    public void onRealTimeMessageReceived(@NonNull RealTimeMessage realTimeMessage) {
      byte[] buf = realTimeMessage.getMessageData();
      String sender = realTimeMessage.getSenderParticipantId();
      Log.d(TAG, "Message received: " + (char) buf[0] + "/" + (int) buf[1]);

      if (buf[0] == 'F' || buf[0] == 'U') {
        // score update.
        int existingScore = mParticipantScore.containsKey(sender) ?
            mParticipantScore.get(sender) : 0;
        int thisScore = (int) buf[1];
        if (thisScore > existingScore) {
          // this check is necessary because packets may arrive out of
          // order, so we
          // should only ever consider the highest score we received, as
          // we know in our
          // game there is no way to lose points. If there was a way to
          // lose points,
          // we'd have to add a "serial number" to the packet.
          mParticipantScore.put(sender, thisScore);
        }

        // update the scores on the screen
        updatePeerScoresDisplay();

        // if it's a final score, mark this participant as having finished
        // the game
        if ((char) buf[0] == 'F') {
          mFinishedParticipants.add(realTimeMessage.getSenderParticipantId());
        }
      }
    }
  };

  // Broadcast my score to everybody else.
  void broadcastScore(boolean finalScore) {
    if (!mMultiplayer) {
      // playing single-player mode
      return;
    }

    // First byte in message indicates whether it's a final score or not
    mMsgBuf[0] = (byte) (finalScore ? 'F' : 'U');

    // Second byte is the score.
    mMsgBuf[1] = (byte) mScore;

    // Send to every other participant.
    for (Participant p : mParticipants) {
      if (p.getParticipantId().equals(mMyId)) {
        continue;
      }
      if (p.getStatus() != Participant.STATUS_JOINED) {
        continue;
      }
      if (finalScore) {
        // final score notification must be sent via reliable message
        mRealTimeMultiplayerClient.sendReliableMessage(mMsgBuf,
            mRoomId, p.getParticipantId(), new RealTimeMultiplayerClient.ReliableMessageSentCallback() {
              @Override
              public void onRealTimeMessageSent(int statusCode, int tokenId, String recipientParticipantId) {
                Log.d(TAG, "RealTime message sent");
                Log.d(TAG, "  statusCode: " + statusCode);
                Log.d(TAG, "  tokenId: " + tokenId);
                Log.d(TAG, "  recipientParticipantId: " + recipientParticipantId);
              }
            })
            .addOnSuccessListener(new OnSuccessListener<Integer>() {
              @Override
              public void onSuccess(Integer tokenId) {
                Log.d(TAG, "Created a reliable message with tokenId: " + tokenId);
              }
            });
      } else {
        // it's an interim score notification, so we can use unreliable
        mRealTimeMultiplayerClient.sendUnreliableMessage(mMsgBuf, mRoomId,
            p.getParticipantId());
      }
    }
  }

    /*
     * UI SECTION. Methods that implement the game's UI.
     */

  // This array lists everything that's clickable, so we can install click
  // event handlers.
  final static int[] CLICKABLES = {
      R.id.button_accept_popup_invitation, R.id.button_invite_players,
      R.id.button_quick_game, R.id.button_see_invitations, R.id.button_sign_in,
      R.id.button_sign_out, R.id.button_click_me, R.id.button_single_player,
      R.id.button_single_player_2
  };

  // This array lists all the individual screens our game has.
  final static int[] SCREENS = {
      R.id.screen_game, R.id.screen_main, R.id.screen_sign_in,
      R.id.screen_wait
  };
  int mCurScreen = -1;

  void switchToScreen(int screenId) {
    // make the requested screen visible; hide all others.
    for (int id : SCREENS) {
      findViewById(id).setVisibility(screenId == id ? View.VISIBLE : View.GONE);
    }
    mCurScreen = screenId;

    // should we show the invitation popup?
    boolean showInvPopup;
    if (mIncomingInvitationId == null) {
      // no invitation, so no popup
      showInvPopup = false;
    } else if (mMultiplayer) {
      // if in multiplayer, only show invitation on main screen
      showInvPopup = (mCurScreen == R.id.screen_main);
    } else {
      // single-player: show on main screen and gameplay screen
      showInvPopup = (mCurScreen == R.id.screen_main || mCurScreen == R.id.screen_game);
    }
    findViewById(R.id.invitation_popup).setVisibility(showInvPopup ? View.VISIBLE : View.GONE);
  }

  void switchToMainScreen() {
    if (mRealTimeMultiplayerClient != null) {
      switchToScreen(R.id.screen_main);
    } else {
      switchToScreen(R.id.screen_sign_in);
    }
  }

  // updates the label that shows my score
  void updateScoreDisplay() {
    ((TextView) findViewById(R.id.my_score)).setText(formatScore(mScore));
  }

  // formats a score as a three-digit number
  String formatScore(int i) {
    if (i < 0) {
      i = 0;
    }
    String s = String.valueOf(i);
    return s.length() == 1 ? "00" + s : s.length() == 2 ? "0" + s : s;
  }

  // updates the screen with the scores from our peers
  void updatePeerScoresDisplay() {
    ((TextView) findViewById(R.id.score0)).setText(
        getString(R.string.score_label, formatScore(mScore)));
    int[] arr = {
        R.id.score1, R.id.score2, R.id.score3
    };
    int i = 0;

    if (mRoomId != null) {
      for (Participant p : mParticipants) {
        String pid = p.getParticipantId();
        if (pid.equals(mMyId)) {
          continue;
        }
        if (p.getStatus() != Participant.STATUS_JOINED) {
          continue;
        }
        int score = mParticipantScore.containsKey(pid) ? mParticipantScore.get(pid) : 0;
        ((TextView) findViewById(arr[i])).setText(formatScore(score) + " - " +
            p.getDisplayName());
        ++i;
      }
    }

    for (; i < arr.length; ++i) {
      ((TextView) findViewById(arr[i])).setText("");
    }
  }

    /*
     * MISC SECTION. Miscellaneous methods.
     */


  // Sets the flag to keep this screen on. It's recommended to do that during
  // the
  // handshake when setting up a game, because if the screen turns off, the
  // game will be
  // cancelled.
  void keepScreenOn() {
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  }

  // Clears the flag that keeps the screen on.
  void stopKeepingScreenOn() {
    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  }
}
