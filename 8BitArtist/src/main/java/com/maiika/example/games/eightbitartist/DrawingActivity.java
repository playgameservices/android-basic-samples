/*
 * Copyright (C) 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.maiika.example.games.eightbitartist;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.images.ImageManager;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.OnInvitationReceivedListener;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.turnbased.OnTurnBasedMatchUpdateReceivedListener;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchConfig;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMultiplayer;
import com.maiika.example.games.basegameutils.BaseGameActivity;

import java.util.ArrayList;
import java.util.Collections;

// This is the main activity for all of 8BitArtist.
// It displays a logged-out UI, a game-choosing UI (called "match up"),
// and various playing UIs for drawing and guessing.
public class DrawingActivity extends BaseGameActivity implements
        OnTurnBasedMatchUpdateReceivedListener, OnInvitationReceivedListener {
    public static final String TAG = "DrawingActivity";

    public static final int ROLE_NOTHING = 0;
    public static final int ROLE_GUESSER = 1;
    public static final int ROLE_ARTIST = 2;
    public int mMyRole = ROLE_ARTIST;
    // For our intents
    final static int RC_SELECT_PLAYERS = 10000;
    final static int RC_LOOK_AT_MATCHES = 10001;
    final static int REQUEST_ACHIEVEMENTS = 10002;
    // Should I be showing the turn API?
    public boolean isDoingTurn = false;

    // This is the current match we're in; null if not loaded
    public TurnBasedMatch mMatch;

    // This is the current match data after being unpersisted.
    // Do not retain references to match data once you have
    // taken an action on the match, such as takeTurn()
    public DrawingTurn mTurnData;

    // Game-specific data
    public StateManager mStateManager;
    public DrawView mDrawView;
    public TextView mInstructionsView;
    // Guessable words
    String[] words;
    Stroke mCurrentStroke;
    boolean skipTakeTurnUpdate;
    private AlertDialog mAlertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup sign-in button
        findViewById(R.id.sign_out_button).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        signOut();
                        updateViewVisibility();
                    }
                }
        );

        findViewById(R.id.sign_in_button).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // start the asynchronous sign in flow
                        beginUserInitiatedSignIn();

                        findViewById(R.id.sign_in_button).setVisibility(
                                View.GONE);
                    }
                }
        );

        ((ListView) findViewById(R.id.listView))
                .setOnItemClickListener(new OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view,
                                            int position, long id) {
                        if (mMyRole == ROLE_GUESSER) {
                            makeGuess(position);
                        }
                    }
                });

        mDrawView = ((DrawView) findViewById(R.id.drawView));
        mInstructionsView = ((TextView) findViewById(R.id.instructions));

        mDrawView.setActivity(this);

        mStateManager = StateManager.init(this);

        words = getResources().getString(R.string.words).split(",");

        ((ColorChooser) findViewById(R.id.colorChooser))
                .setDrawView(((DrawView) findViewById(R.id.drawView)));

    }

    // Create the menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    // Respond to the menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_achievements:
                startActivityForResult(Games.Achievements.getAchievementsIntent(getApiClient()),
                        REQUEST_ACHIEVEMENTS);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Look at your inbox. You will get back onActivityResult where
    // you will need to figure out what you clicked on.
    public void onCheckGamesClicked(View view) {
        Intent intent = Games.TurnBasedMultiplayer.getInboxIntent(getApiClient());
        startActivityForResult(intent, RC_LOOK_AT_MATCHES);
    }

    // Open the create-game UI. You will get back an onActivityResult
    // and figure out what to do.
    public void onStartMatchClicked(View view) {
        Intent intent = Games.TurnBasedMultiplayer.getSelectOpponentsIntent(getApiClient(), 1, 1, true);
        startActivityForResult(intent, RC_SELECT_PLAYERS);
    }

    // In-game controls

    // Cancel the game. Should possibly wait until the game is canceled before
    // giving up on the view.
    public void onCancelClicked(View view) {
        showSpinner();
        Games.TurnBasedMultiplayer.cancelMatch(getApiClient(), mMatch.getMatchId())
                .setResultCallback(new ResultCallback<TurnBasedMultiplayer.CancelMatchResult>() {
                    @Override
                    public void onResult(TurnBasedMultiplayer.CancelMatchResult result) {
                        processResult(result);
                    }
                });
        isDoingTurn = false;
        updateViewVisibility();
    }


    private void processResult(TurnBasedMultiplayer.CancelMatchResult result) {
        dismissSpinner();

        if (!checkStatusCode(null, result.getStatus().getStatusCode())) {
            return;
        }

        isDoingTurn = false;

        showWarning("Match",
                "This match is canceled.  All other players will have their game ended.");
    }

    private void processResult(TurnBasedMultiplayer.InitiateMatchResult result) {
        TurnBasedMatch match = result.getMatch();
        dismissSpinner();

        if (!checkStatusCode(match, result.getStatus().getStatusCode())) {
            return;
        }

        if (match.getData() != null) {
            // This is a game that has already started, so I'll just start
            updateMatch(match);
            return;
        }
        startMatch(match);
    }


    private void processResult(TurnBasedMultiplayer.LeaveMatchResult result) {
        TurnBasedMatch match = result.getMatch();
        dismissSpinner();
        if (!checkStatusCode(match, result.getStatus().getStatusCode())) {
            return;
        }
        isDoingTurn = (match.getTurnStatus() == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN);
        showWarning("Left", "You've left this match.");
    }


    public void processResult(TurnBasedMultiplayer.UpdateMatchResult result) {
        TurnBasedMatch match = result.getMatch();
        dismissSpinner();
        if (!checkStatusCode(match, result.getStatus().getStatusCode())) {
            return;
        }
        if (match.canRematch()) {
            askForRematch();
        }

        isDoingTurn = (match.getTurnStatus() == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN);

        if (isDoingTurn) {
            updateMatch(match);
            return;
        }
        updateViewVisibility();
    }

    // Leave the game during your turn. Note that there is a separate
    // GamesClient.leaveTurnBasedMatch() if you want to leave NOT on your turn.
    public void onLeaveClicked(View view) {
        showSpinner();
        String nextParticipantId = getNextParticipantId();
        Games.TurnBasedMultiplayer.leaveMatchDuringTurn(getApiClient(), mMatch.getMatchId(),
                nextParticipantId).setResultCallback(
                new ResultCallback<TurnBasedMultiplayer.LeaveMatchResult>() {
                    @Override
                    public void onResult(TurnBasedMultiplayer.LeaveMatchResult result) {
                        processResult(result);
                    }
                }
        );
        updateViewVisibility();
    }

    // Finish the game. Sometimes, this is your only choice.
    public void onFinishClicked(View view) {
        showSpinner();
        Games.TurnBasedMultiplayer.finishMatch(getApiClient(), mMatch.getMatchId())
                .setResultCallback(new ResultCallback<TurnBasedMultiplayer.UpdateMatchResult>() {
                    @Override
                    public void onResult(TurnBasedMultiplayer.UpdateMatchResult result) {
                        processResult(result);
                    }
                });
        isDoingTurn = false;
        updateViewVisibility();
    }

    // Kick off the appropriate transition
    public void onDoneClicked(View view) {
        mStateManager.transitionState(StateManager.STATE_SENDING);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    // Sign-in, Sign out behavior

    // Update the visibility based on what state we're in.
    // This pops the visibility, which breaks the transitions.
    public void updateViewVisibility() {
        if (!isSignedIn()) {
            findViewById(R.id.login_layout).setVisibility(View.VISIBLE);
            findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);

            findViewById(R.id.matchup_layout).setVisibility(View.GONE);
            findViewById(R.id.gameplay_layout).setVisibility(View.GONE);

            ((TextView) findViewById(R.id.name_field)).setText("");

            if (mAlertDialog != null) {
                mAlertDialog.dismiss();
            }

            return;
        }

        ((TextView) findViewById(R.id.name_field)).setText(
                Games.Players.getCurrentPlayer(getApiClient()).getDisplayName());

        refreshTurnsPending();

        findViewById(R.id.login_layout).setVisibility(View.GONE);

        if (isDoingTurn) {
            findViewById(R.id.matchup_layout).setVisibility(View.GONE);
            findViewById(R.id.gameplay_layout).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.matchup_layout).setVisibility(View.VISIBLE);
            findViewById(R.id.gameplay_layout).setVisibility(View.GONE);
        }
    }

    @Override
    public void onSignInFailed() {
        updateViewVisibility();
    }

    @Override
    public void onSignInSucceeded() {
        if (mHelper.getTurnBasedMatch() != null) {
            updateMatch(mHelper.getTurnBasedMatch());
            return;
        }

        updateViewVisibility();

        // As a demonstration, we are registering this activity as a handler for
        // invitation and match events.

        // This is *NOT* required; if you do not register a handler for
        // invitation events, you will get standard notifications instead.
        // Standard notifications may be preferable behavior in many cases.
        Games.Invitations.registerInvitationListener(getApiClient(), this);

        // Likewise, we are registering the optional MatchUpdateListener, which
        // will replace notifications you would get otherwise. You do *NOT* have
        // to register a MatchUpdateListener.
        Games.TurnBasedMultiplayer.registerMatchUpdateListener(getApiClient(), this);
    }

    // Update the number of available turns
    public void refreshTurnsPending() {
        int statuses[] = {TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN};
        Games.TurnBasedMultiplayer.loadMatchesByStatus(getApiClient(), statuses);
        TextView textView = (TextView) findViewById(R.id.turns_to_go);
        textView.setText("...");
    }

    // Helpful dialogs
    public void showSpinner() {
        findViewById(R.id.progressLayout).setVisibility(View.VISIBLE);
    }

    public void dismissSpinner() {
        findViewById(R.id.progressLayout).setVisibility(View.GONE);
    }

    // Generic warning/info dialog
    public void showWarning(String title, String message) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set title
        alertDialogBuilder.setTitle(title).setMessage(message);

        // set dialog message
        alertDialogBuilder.setCancelable(false).setPositiveButton("OK", null);

        // create alert dialog
        mAlertDialog = alertDialogBuilder.create();

        // show it
        mAlertDialog.show();
    }

    // This function is what gets called when you return from either the Play
    // Games built-in inbox, or else the create game built-in interface.
    @Override
    public void onActivityResult(int request, int response, Intent data) {
        // It's VERY IMPORTANT for you to remember to call your superclass.
        // BaseGameActivity will not work otherwise.
        super.onActivityResult(request, response, data);

        // We are coming back from the match inbox UI.
        if (request == RC_LOOK_AT_MATCHES) {
            if (response != Activity.RESULT_OK) {
                // user canceled
                return;
            }

            TurnBasedMatch match = data
                    .getParcelableExtra(Multiplayer.EXTRA_TURN_BASED_MATCH);

            if (match != null) {
                updateMatch(match);
            } else {
                refreshTurnsPending();
            }

            Log.d(TAG, "Match = " + match);
        }

        // We are coming back from the player selection UI, in preparation to start a match.
        if (request == RC_SELECT_PLAYERS) {
            if (response != Activity.RESULT_OK) {
                // user canceled
                refreshTurnsPending();
                return;
            }

            // get the invitee list
            final ArrayList<String> invitees = data.getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);

            // get auto-match criteria
            Bundle autoMatchCriteria = null;

            int minAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MIN_AUTOMATCH_PLAYERS, 0);
            int maxAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MAX_AUTOMATCH_PLAYERS, 0);

            if (minAutoMatchPlayers > 0) {
                autoMatchCriteria = RoomConfig.createAutoMatchCriteria(
                        minAutoMatchPlayers, maxAutoMatchPlayers, 0);
            }

            TurnBasedMatchConfig matchConfig = TurnBasedMatchConfig.builder()
                    .addInvitedPlayers(invitees)
                    .setAutoMatchCriteria(autoMatchCriteria).build();

            // Start the match
            Games.TurnBasedMultiplayer.createMatch(getApiClient(), matchConfig).setResultCallback(
                    new ResultCallback<TurnBasedMultiplayer.InitiateMatchResult>() {
                        @Override
                        public void onResult(TurnBasedMultiplayer.InitiateMatchResult result) {
                            Log.v(TAG, result.getMatch().getDescription());
                            processResult(result);
                        }
                    }
            );

            showSpinner();
        }
    }

    // This happens in response to the createTurnBasedMatch() above. This is
    // only called on success, so we should have a valid match object.
    // We're taking this opportunity to setup the game, saving our
    // initial state. Calling takeTurn() will callback to
    // OnTurnBasedMatchUpdated(), which will show the game UI
    public void startMatch(TurnBasedMatch match) {

        // Now, it's possible an auto-match could have taken place, so...
        if (match.getData() != null) {
            updateMatch(match);
            return;
        }

        // Otherwise, kick off a new match
        mMatch = match;

        mTurnData = new DrawingTurn();

        String myParticipantId = getCurrentParticipantId();

        mTurnData.artistTurn = createNewTurnSegment(myParticipantId);

        showSpinner();
        Log.e(TAG, myParticipantId);

        Games.TurnBasedMultiplayer.takeTurn(getApiClient(), mMatch.getMatchId(),
                mTurnData.persist(), myParticipantId).setResultCallback(
                new ResultCallback<TurnBasedMultiplayer.UpdateMatchResult>() {
                    @Override
                    public void onResult(TurnBasedMultiplayer.UpdateMatchResult result) {
                        processResult(result);
                    }
                }
        );

        // Persistence check
        DrawingTurn check = DrawingTurn.unpersist(mTurnData.persist());

        /*
        if (BuildConfig.DEBUG &&
                check.guessingTurn.strokes.size() == mTurnData.guessingTurn.strokes.size()){
            throw new RuntimeException();
        } */

        // Since we're calling TurnUpdated here
        mTurnData = null;
        mCurrentStroke = null;
    }

    // If you choose to rematch, then call it and wait for a response.
    public void rematch() {
        showSpinner();
        Games.TurnBasedMultiplayer.rematch(getApiClient(), mMatch.getMatchId()).setResultCallback(
                new ResultCallback<TurnBasedMultiplayer.InitiateMatchResult>() {

                    @Override
                    public void onResult(TurnBasedMultiplayer.InitiateMatchResult result) {
                        processResult(result);
                    }
                }
        );

        mMatch = null;
        isDoingTurn = false;
    }

    // This is the main function that gets called when players choose a match
    // from the inbox, or else create a match and want to start it.
    public void updateMatch(TurnBasedMatch match) {
        mMatch = match;
        // Unpack the turn data
        mTurnData = DrawingTurn.unpersist(mMatch.getData());

        int status = match.getStatus();
        int turnStatus = match.getTurnStatus();

        switch (status) {
            case TurnBasedMatch.MATCH_STATUS_CANCELED:
                showWarning("Canceled!", "This game was canceled!");
                return;
            case TurnBasedMatch.MATCH_STATUS_EXPIRED:
                showWarning("Expired!", "This game is expired.  So sad!");
                return;
            case TurnBasedMatch.MATCH_STATUS_AUTO_MATCHING:
                showWarning("Waiting for auto-match...",
                        "We're still waiting for an auto-match partner.");
                return;
            case TurnBasedMatch.MATCH_STATUS_COMPLETE:
                if (turnStatus == TurnBasedMatch.MATCH_TURN_STATUS_COMPLETE) {
                    showWarning(
                            "Complete!",
                            "This game is over; someone finished it, and so did you!  There is nothing to be done.");
                    break;
                }

                // Note that in this state, you must still call "Finish" yourself,
                // so we allow this to continue.
                showWarning("Complete!",
                        "This game is over; someone finished it!  You can only finish it now.");

                // Show the replay of them guessing, and then you're done.
                if (mTurnData.guessingTurn != null) {

                    // Move the guessed turn to the next turn
                    mTurnData.replayTurn = mTurnData.guessingTurn;
                    // Need to see your replay
                    mStateManager
                            .transitionState(StateManager.STATE_REPLAY_METADATA);
                }
                return;

        }

        // OK, it's active. Check on turn status.
        switch (turnStatus) {
            case TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN:
                // Should return results.
                showWarning("Alas...", "It's not your turn.");
                break;
            case TurnBasedMatch.MATCH_TURN_STATUS_INVITED:
                showWarning("Good initiative!",
                        "Still waiting for invitations.\n\nBe patient!");

            case TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN:
                if (mTurnData.replayTurn != null
                        && mTurnData.guessingTurn.guessedWord == -1) {
                    // Need to see your replay
                    mStateManager
                            .transitionState(StateManager.STATE_REPLAY_METADATA);
                    return;
                }

                // Should only hit this on turn 1
                if (mTurnData.guessingTurn != null
                        && mTurnData.guessingTurn.guessedWord == -1) {
                    // Need to see your guess and you haven't guessed yet
                    mStateManager
                            .transitionState(StateManager.STATE_GUESSING_METADATA);
                    return;
                }

                // The game needs to be initiated, because A) it is a rematch, or
                // B) something crashed during startup and we have a null starting situation.
                if (mTurnData.artistTurn == null) {
                    startMatch(match);
                    return;
                }

                // Otherwise, it must be turn 1
                if (mTurnData.artistTurn.turnNumber < 2) {
                    Games.Achievements.unlock(
                            getApiClient(), getString(R.string.achievement_started_a_game));
                }

                if (!skipTakeTurnUpdate) {
                    mStateManager
                            .transitionState(StateManager.STATE_NEW_TURN_METADATA);
                } else {
                    skipTakeTurnUpdate = false;
                }
                return;

        }

        mTurnData = null;
        mCurrentStroke = null;

        updateViewVisibility();
    }

    // Handle notification events.
    @Override
    public void onInvitationReceived(Invitation invitation) {
        Toast.makeText(
                this,
                "An invitation has arrived from "
                        + invitation.getInviter().getDisplayName(), Toast.LENGTH_SHORT
        )
                .show();

        refreshTurnsPending();
    }

    @Override
    public void onInvitationRemoved(String invitationId) {
        Toast.makeText(this, "An invitation was removed.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTurnBasedMatchReceived(TurnBasedMatch match) {
        Toast.makeText(this, "A match was updated.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onTurnBasedMatchRemoved(String matchId) {
        Toast.makeText(this, "A match was removed.", Toast.LENGTH_SHORT).show();

    }

    public void showErrorMessage(TurnBasedMatch match, int statusCode,
                                 int stringId) {

        showWarning("Warning", getResources().getString(stringId));
    }

    // Returns false if something went wrong, probably. This should handle
    // more cases, and probably report more accurate results.
    private boolean checkStatusCode(TurnBasedMatch match, int statusCode) {
        switch (statusCode) {
            case GamesStatusCodes.STATUS_OK:
                return true;
            case GamesStatusCodes.STATUS_NETWORK_ERROR_OPERATION_DEFERRED:
                // This is OK; the action is stored by Google Play Services and will
                // be dealt with later.
                Toast.makeText(
                        this,
                        "Stored action for later.  (Please remove this toast before release.)",
                        Toast.LENGTH_SHORT).show();
                // NOTE: This toast is for informative reasons only; please remove
                // it from your final application.
                return true;
            case GamesStatusCodes.STATUS_MULTIPLAYER_ERROR_NOT_TRUSTED_TESTER:
                showErrorMessage(match, statusCode,
                        R.string.status_multiplayer_error_not_trusted_tester);
                break;
            case GamesStatusCodes.STATUS_MATCH_ERROR_ALREADY_REMATCHED:
                showErrorMessage(match, statusCode,
                        R.string.match_error_already_rematched);
                break;
            case GamesStatusCodes.STATUS_NETWORK_ERROR_OPERATION_FAILED:
                showErrorMessage(match, statusCode,
                        R.string.network_error_operation_failed);
                break;
            case GamesStatusCodes.STATUS_CLIENT_RECONNECT_REQUIRED:
                showErrorMessage(match, statusCode,
                        R.string.client_reconnect_required);
                break;
            case GamesStatusCodes.STATUS_INTERNAL_ERROR:
                showErrorMessage(match, statusCode, R.string.internal_error);
                break;
            case GamesStatusCodes.STATUS_MATCH_ERROR_INACTIVE_MATCH:
                showErrorMessage(match, statusCode,
                        R.string.match_error_inactive_match);
                break;
            case GamesStatusCodes.STATUS_MATCH_ERROR_LOCALLY_MODIFIED:
                showErrorMessage(match, statusCode,
                        R.string.match_error_locally_modified);
                break;
            default:
                showErrorMessage(match, statusCode, R.string.unexpected_status);
                Log.d(TAG, "Did not have warning or string to deal with: "
                        + statusCode);
        }

        return false;
    }

    // Drawing specific stuff

    public void onPlaybackEnded() {
        findViewById(R.id.skip_button).setVisibility(View.INVISIBLE);
        findViewById(R.id.instructions).setVisibility(View.VISIBLE);
    }

    public void onSkipClicked(View v) {
        if (mStateManager.state == StateManager.STATE_GUESSING) {
            mTurnData.guessingTurn.skip();
        }
        if (mStateManager.state == StateManager.STATE_REPLAY) {
            mTurnData.replayTurn.skip();
        }
    }

    public void onClearClicked(View v) {
        mDrawView.clear(true);
    }

    public void onPlaybackStarted() {
        findViewById(R.id.skip_button).setVisibility(View.VISIBLE);
        findViewById(R.id.instructions).setVisibility(View.INVISIBLE);

    }

    public void emitDrawEvent(int gridX, int gridY, short colorIndex) {
        if (mTurnData == null) {
            Log.e(TAG, "Why are we drawing with no current turn?");
        }

        if (mTurnData.artistTurn == null) {
            Log.e(TAG, "Why are we drawing with no artist Turn?");
        }

        if (mCurrentStroke == null) {
            mCurrentStroke = new Stroke(colorIndex);
            mTurnData.artistTurn.strokes.add(mCurrentStroke);
        } else if (mCurrentStroke.color != colorIndex) {
            mCurrentStroke = new Stroke(colorIndex);
            mTurnData.artistTurn.strokes.add(mCurrentStroke);
        }

        mCurrentStroke.points.add(new EPoint(gridX, gridY));
    }

    public void emitClearEvent() {
        mCurrentStroke = new Stroke((short) -1);
        mCurrentStroke.isClear = true;
        mTurnData.artistTurn.strokes.add(mCurrentStroke);
        mCurrentStroke = new Stroke(mDrawView.mSelectedColor);
        mTurnData.artistTurn.strokes.add(mCurrentStroke);
    }

    public void createGuessDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        Log.d(TAG, "Guessed..." + mTurnData.guessingTurn.guessedWord);

        if (mTurnData.guessingTurn.guessedWord == mTurnData.guessingTurn.wordIndex) {
            // set title
            alertDialogBuilder.setTitle("You got it!").setMessage(
                    mTurnData.guessingTurn.words
                            .get(mTurnData.guessingTurn.guessedWord)
                            + " is correct!\n\nWould you like keep playing?"
            );

            Games.Achievements.reveal(
                    getApiClient(), getString(R.string.achievement_guessed_correctly));
            Games.Achievements.unlock(
                    getApiClient(), getString(R.string.achievement_guessed_correctly));
        } else {
            alertDialogBuilder.setTitle("No!").setMessage(
                    mTurnData.guessingTurn.words
                            .get(mTurnData.guessingTurn.guessedWord)
                            + " is wrong.  The real answer was "
                            + mTurnData.guessingTurn.words
                            .get(mTurnData.guessingTurn.wordIndex)
                            + "\n\nWould you like to draw next?"
            );
            Games.Achievements.unlock(
                    getApiClient(), getString(R.string.achievement_got_one_wrong));
        }

        if (mMatch.getStatus() == TurnBasedMatch.MATCH_STATUS_COMPLETE) {
            alertDialogBuilder.setCancelable(false).setPositiveButton("Done!",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            quitMatch();
                        }
                    }
            );
            showWarning("Game over.",
                    "This game is over, so you can't keep playing.");
        } else {
            // set dialog message
            alertDialogBuilder
                    .setCancelable(false)
                    .setPositiveButton("Keep going!",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int id) {
                                    mStateManager
                                            .transitionState(StateManager.STATE_NEW_TURN_METADATA);
                                }
                            }
                    )
                    .setNegativeButton("Quit",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int id) {
                                    quitMatch();

                                    askForRematch();

                                }
                            }
                    );
        }

        // create alert dialog
        mAlertDialog = alertDialogBuilder.create();

        // show it
        mAlertDialog.show();
    }

    public void askForRematch() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        alertDialogBuilder.setMessage("Do you want a rematch?");

        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("Sure, rematch!",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                rematch();
                            }
                        }
                )
                .setNegativeButton("No thanks.",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                quitMatch();
                            }
                        }
                );
    }

    public void createOpponentGuessDialog() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        StringBuilder message = new StringBuilder();

        if (mTurnData.replayTurn.guessedWord == -1) {
            alertDialogBuilder.setTitle("Weird.");
            message.append("There was no guess.  Weird.");
        } else if (mTurnData.replayTurn.guessedWord == mTurnData.replayTurn.wordIndex) {
            // set title

            message.append("They guessed ");
            message.append(mTurnData.replayTurn.words.get(mTurnData.replayTurn.guessedWord));
            message.append(", which is correct!");

            alertDialogBuilder.setTitle("They got it!");
        } else {
            alertDialogBuilder.setTitle("No!");

            message.append("They guessed ");
            message.append(mTurnData.replayTurn.words.get(mTurnData.replayTurn.guessedWord));
            message.append(", which is wrong.  The real answer was ");
            message.append(mTurnData.replayTurn.words.get(mTurnData.replayTurn.wordIndex));
            message.append(".");
        }

        if (mMatch.getStatus() == TurnBasedMatch.MATCH_STATUS_COMPLETE) {
            quitMatch();
            message.append("\n\nThis game is over.");
            alertDialogBuilder.setCancelable(false).setPositiveButton(
                    "Game over.", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            /*
                             * mStateManager .transitionState(StateManager.STATE_MATCH_UP);
                             */
                        }
                    }
            );
        } else {
            // set dialog message
            alertDialogBuilder.setCancelable(false).setPositiveButton(
                    "Keep going!", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            mStateManager
                                    .transitionState(StateManager.STATE_GUESSING_METADATA);
                        }
                    }
            );
        }

        alertDialogBuilder.setMessage(message.toString());

        // create alert dialog
        mAlertDialog = alertDialogBuilder.create();

        // show it
        mAlertDialog.show();
    }

    public void onMetadataDismissed(View view) {
        if (mStateManager.state == StateManager.STATE_NEW_TURN_METADATA) {
            mStateManager.transitionState(StateManager.STATE_NEW_TURN);
            return;
        }

        if (mStateManager.state == StateManager.STATE_REPLAY_METADATA) {
            mStateManager.transitionState(StateManager.STATE_REPLAY);
            return;
        }

        if (mStateManager.state == StateManager.STATE_GUESSING_METADATA) {
            mStateManager.transitionState(StateManager.STATE_GUESSING);
        }
    }

    public void beginOldTurnPlayback() {
        mDrawView.clear(false);
        if (mTurnData.replayTurn != null) {
            mTurnData.replayTurn.playback(this, true);
        } else {
            Log.d(TAG, "Bug, tried to replay without a replay.");
            beginGuessingTurnPlayback();
        }
    }

    public void beginGuessingTurnPlayback() {
        mDrawView.clear(false);
        mTurnData.guessingTurn.playback(this, false);
        setGuessingUI();
    }

    public void beginArtistTurn() {
        mDrawView.clear(false);
        findViewById(R.id.artistUI).setVisibility(View.VISIBLE);
        findViewById(R.id.guesserUI).setVisibility(View.GONE);
        setArtistUI();
    }

    public void beginSend() {
        String nextParticipantId = null;

        String myParticipantId = getCurrentParticipantId();

        ArrayList<String> participantIds = mMatch.getParticipantIds();

        int desiredIndex = -1;

        for (int i = 0; i < participantIds.size(); i++) {
            if (participantIds.get(i).equals(myParticipantId)) {
                desiredIndex = i + 1;
            }
        }

        // Loop once you reach the end of the line
        if (desiredIndex >= participantIds.size()) {
            if (mMatch.getAvailableAutoMatchSlots() == 0) {
                desiredIndex = 0;
            } else {
                // Interesting
                desiredIndex = -1;
            }
        }

        if (desiredIndex == -1) {

            if (mMatch.getAvailableAutoMatchSlots() > 0) {
                nextParticipantId = null;
            } else {
                Log.e(TAG, "Couldn't find next player, chaos erupts.");
            }
        } else {
            nextParticipantId = participantIds.get(desiredIndex);
        }

        // Create the next turn
        mTurnData.replayTurn = mTurnData.guessingTurn;
        mTurnData.guessingTurn = mTurnData.artistTurn;
        mTurnData.artistTurn = createNewTurnSegment(nextParticipantId);

        if (mTurnData.guessingTurn.strokes.size() == 0) {
            Log.d(TAG, "Sending empty turn.");
        }

        showSpinner();

        Games.TurnBasedMultiplayer.takeTurn(getApiClient(), mMatch.getMatchId(),
                mTurnData.persist(), nextParticipantId).setResultCallback(
                new ResultCallback<TurnBasedMultiplayer.UpdateMatchResult>() {

                    @Override
                    public void onResult(TurnBasedMultiplayer.UpdateMatchResult result) {
                        processResult(result);
                    }
                }
        );

        mTurnData = null;
        mCurrentStroke = null;
    }

    public TurnSegment createNewTurnSegment(String participantId) {
        TurnSegment retVal;

        int turnNum = (mTurnData.guessingTurn == null ? 0
                : mTurnData.guessingTurn.turnNumber + 1);

        if (turnNum >= 5) {
            Games.Achievements.unlock(
                    getApiClient(), getString(R.string.achievement_5_turns));
        }

        if (turnNum >= 10) {
            Games.Achievements.unlock(
                    getApiClient(), getString(R.string.achievement_10_turns));
        }

        // I am the only person who will create
        retVal = new TurnSegment(turnNum, participantId);

        ArrayList<String> subsetWords = new ArrayList<String>();
        Collections.addAll(subsetWords, words);
        Collections.shuffle(subsetWords);

        ArrayList<String> finalList = new ArrayList<String>(
                subsetWords.subList(0, 10));

        retVal.words = finalList;

        retVal.wordIndex = (int) Math.floor(Math.random() * 10);
        int count = 0;
        while (count < 20
                && mTurnData.seenWords.contains(finalList
                .get(retVal.wordIndex))) {
            retVal.wordIndex = (int) Math.floor(Math.random() * 10);
            count++;
        }

        mTurnData.seenWords.add(finalList.get(retVal.wordIndex));

        return retVal;
    }

    public void makeGuess(int position) {

        mTurnData.guessingTurn.skip();
        mTurnData.guessingTurn.guessedWord = position;
        createGuessDialog();

        skipTakeTurnUpdate = true;

        Games.TurnBasedMultiplayer.takeTurn(getApiClient(), mMatch.getMatchId(),
                mTurnData.persist(), getCurrentParticipantId()).setResultCallback(
                new ResultCallback<TurnBasedMultiplayer.UpdateMatchResult>() {

                    @Override
                    public void onResult(TurnBasedMultiplayer.UpdateMatchResult result) {
                        processResult(result);
                    }
                }
        );
    }

    public void setGuessingUI() {

        findViewById(R.id.guesserUI).setVisibility(View.VISIBLE);
        findViewById(R.id.colorChooser).setVisibility(View.GONE);
        findViewById(R.id.doneButton).setVisibility(View.GONE);
        findViewById(R.id.clearButton).setVisibility(View.GONE);
        findViewById(R.id.guessWord).setVisibility(View.GONE);
        findViewById(R.id.replayUI).setVisibility(View.GONE);

        String partId = mTurnData.guessingTurn.artistParticipantId;

        Player player = null;
        if (partId != null) {
            player = mMatch.getParticipant(partId).getPlayer();
        }

        ImageManager imMan = ImageManager.create(this);

        if (player == null) {
            ((TextView) findViewById(R.id.person_name))
                    .setText("Auto-match player");
            findViewById(R.id.person_image)
                    .setBackgroundResource(R.drawable.none);
        } else {
            ((TextView) findViewById(R.id.person_name)).setText(player
                    .getDisplayName());
            // In case the image load fails
            findViewById(R.id.person_image)
                    .setBackgroundDrawable(null);

            imMan.loadImage(new ImageManager.OnImageLoadedListener() {
                @Override
                public void onImageLoaded(Uri uri, Drawable drawable,
                                          boolean isRequestedDrawable) {
                    if (drawable == null) {
                        findViewById(R.id.person_image)
                                .setBackgroundResource(R.drawable.none);
                        return;
                    }
                    findViewById(R.id.person_image)
                            .setBackgroundDrawable(drawable);
                }
            }, player.getIconImageUri());
        }

        resetWords(mTurnData.guessingTurn);

        mDrawView.clear(false);
    }

    public void quitMatch() {
        showSpinner();

        // Quit the game
        Games.TurnBasedMultiplayer.finishMatch(getApiClient(), mMatch.getMatchId())
                .setResultCallback(new ResultCallback<TurnBasedMultiplayer.UpdateMatchResult>() {
                    @Override
                    public void onResult(TurnBasedMultiplayer.UpdateMatchResult result) {
                        processResult(result);
                    }
                });
    }

    public void resetWords(TurnSegment turn) {
        ListView list = (ListView) findViewById(R.id.listView);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1);

        for (String st : turn.words) {
            adapter.add(st);
        }

        list.setAdapter(adapter);
    }

    public void setReplayUI() {
        findViewById(R.id.guesserUI).setVisibility(View.GONE);
        findViewById(R.id.colorChooser).setVisibility(View.GONE);
        findViewById(R.id.doneButton).setVisibility(View.GONE);
        findViewById(R.id.clearButton).setVisibility(View.GONE);
        findViewById(R.id.guessWord).setVisibility(View.GONE);
        findViewById(R.id.replayUI).setVisibility(View.VISIBLE);
        resetWords(mTurnData.replayTurn);
        mDrawView.clear(false);

        String partId = mTurnData.replayTurn.artistParticipantId;

        Player player = null;
        if (partId != null) {
            player = mMatch.getParticipant(partId).getPlayer();
        }

        ImageManager imMan = ImageManager.create(this);

        if (player == null) {
            ((TextView) findViewById(R.id.replay_person_name))
                    .setText("Auto-match player");
            findViewById(R.id.replay_person_image)
                    .setBackgroundResource(R.drawable.none);
        } else {
            ((TextView) findViewById(R.id.replay_person_name)).setText(player
                    .getDisplayName());
            // In case the image load fails
            findViewById(R.id.person_image)
                    .setBackgroundResource(0);
            imMan.loadImage(new ImageManager.OnImageLoadedListener() {
                @Override
                public void onImageLoaded(Uri uri, Drawable drawable,
                                          boolean isRequestedDrawable) {
                    findViewById(R.id.replay_person_image)
                            .setBackgroundDrawable(drawable);

                }
            }, player.getIconImageUri());
        }

    }

    public void setArtistUI() {
        findViewById(R.id.guesserUI).setVisibility(View.GONE);
        findViewById(R.id.replayUI).setVisibility(View.GONE);

        findViewById(R.id.artistUI).setVisibility(View.VISIBLE);
        findViewById(R.id.colorChooser).setVisibility(View.VISIBLE);
        findViewById(R.id.doneButton).setVisibility(View.VISIBLE);
        findViewById(R.id.clearButton).setVisibility(View.VISIBLE);
        findViewById(R.id.guessWord).setVisibility(View.VISIBLE);

        ((TextView) findViewById(R.id.guessWord))
                .setText(mTurnData.artistTurn.words
                        .get(mTurnData.artistTurn.wordIndex));
        mDrawView.clear(false);
        mCurrentStroke = null;
    }

    // Utility functions
    public Participant getParticipantForPlayerId(String playerId) {
        for (Participant part : mMatch.getParticipants()) {
            if (part.getPlayer() != null
                    && part.getPlayer().getPlayerId().equals(playerId)) {
                return part;
            }
        }

        return null;
    }

    public String getCurrentParticipantId() {
        String playerId = Games.Players.getCurrentPlayerId(getApiClient());
        return mMatch.getParticipantId(playerId);
    }

    /**
     * Get the next participant. In this function, we assume that we are
     * round-robin, with all known players going before all auto-match players.
     * This is not a requirement; players can go in any order. However, you can
     * take turns in any order.
     *
     * @return participantId of next player, or null if auto-matching
     */
    public String getNextParticipantId() {

        String playerId = Games.Players.getCurrentPlayerId(getApiClient());
        String myParticipantId = mMatch.getParticipantId(playerId);

        ArrayList<String> participantIds = mMatch.getParticipantIds();

        int desiredIndex = -1;

        for (int i = 0; i < participantIds.size(); i++) {
            if (participantIds.get(i).equals(myParticipantId)) {
                desiredIndex = i + 1;
            }
        }

        if (desiredIndex < participantIds.size()) {
            return participantIds.get(desiredIndex);
        }

        if (mMatch.getAvailableAutoMatchSlots() <= 0) {
            // You've run out of auto-match slots, so we start over.
            return participantIds.get(0);
        } else {
            // You have not yet fully auto-matched, so null will find a new
            // person to play against.
            return null;
        }
    }
}
