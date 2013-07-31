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

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessage;
import com.google.example.games.basegameutils.BaseGameActivity;
import com.google.example.games.basegameutils.RtmpHelper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Button Clicker 2000. A minimalistic game showing the multiplayer features of the Google Play
 * game services API. The objective of this game is clicking a button. Whoever clicks the
 * button the most times within a 20 second interval wins. It's that simple. This game can
 * be played with 2, 3 or 4 players. To run this sample, please set up a project in Developer
 * Console. Then, place your app ID on res/values/ids.xml. Also, change the package name to the
 * package name you used to create the client ID in Developer Console. Make sure you sign the
 * APK with the certificate whose fingerprint you entered in Developer Console when creating
 * your Client ID.
 *
 * @author Bruno Oliveira (btco), 2013-04-26
 */
public class MainActivity extends BaseGameActivity implements View.OnClickListener  {
    final static boolean ENABLE_DEBUG = true;
    final static String TAG = "ButtonClicker2000";

    // This array lists all the individual screens our game has.
    final static int[] SCREENS = {
            R.id.screen_game, R.id.screen_main, R.id.screen_sign_in, R.id.screen_wait
    };
    int mCurScreen = -1; // screen currently being shown

    // This array lists everything that's clickable, so we can install click event handlers.
    final static int[] CLICKABLES = {
            R.id.button_accept_popup_invitation, R.id.button_invite_players,
            R.id.button_quick_game, R.id.button_see_invitations, R.id.button_sign_in,
            R.id.button_sign_out, R.id.button_click_me, R.id.button_single_player,
            R.id.button_single_player_2
    };

    boolean mMultiplayer = false;  // multiplayer mode?
    Invitation mIncomingInvitation = null;  // invitation we got via onRtmpInvite
    byte[] mMsgBuf = new byte[2]; // message buffer for sending/receiving messages
    final static int MIN_OPPONENTS = 1, MAX_OPPONENTS = 3; // min and max # of opponents

    // Score of other participants. We update this as we receive their scores from the network.
    Map<String, Integer> mParticipantScore = new HashMap<String, Integer>();

    // Participants who sent us their final score.
    Set<String> mFinishedParticipants = new HashSet<String>();

    // Current state of the game:
    int mSecondsLeft = -1; // how long until the game ends (seconds)
    final static int GAME_DURATION = 20; // game duration, seconds.
    int mScore = 0; // user's current score
    Runnable mClockTickRunnable = null;
    Handler mHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        enableDebugLog(ENABLE_DEBUG, TAG);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // set up a click listener for everything we care about
        for (int id : CLICKABLES) {
            findViewById(id).setOnClickListener(this);
        }

        mHandler = new Handler();
        getRtmp().enableEarlyStart(true); // DEBUG
    }

    @Override
    public void onSignInFailed() {
        switchToScreen(R.id.screen_sign_in);
    }

    @Override
    public void onSignInSucceeded() {
        switchToMainScreen();
    }

    @Override
    public void onClick(View v) {
        Intent intent;

        switch (v.getId()) {
            case R.id.button_single_player:
            case R.id.button_single_player_2:
                startGame(false);
                break;
            case R.id.button_sign_in:
                if (!verifyPlaceholderIdsReplaced()) {
                    showAlert("Error", "Sample not set up correctly. Please see README.");
                    return;
                }
                switchToScreen(R.id.screen_wait);
                beginUserInitiatedSignIn();
                break;
            case R.id.button_sign_out:
                signOut();
                switchToScreen(R.id.screen_sign_in);
                break;
            case R.id.button_invite_players:
                // show list of invitable players
                switchToScreen(R.id.screen_wait);
                getRtmp().startWithInviteDialog(MIN_OPPONENTS, MAX_OPPONENTS);
                break;
            case R.id.button_see_invitations:
                // show list of pending invitations
                switchToScreen(R.id.screen_wait);
                getRtmp().startWithInvitationInbox();
                break;
            case R.id.button_accept_popup_invitation:
                // user wants to accept the invitation shown on the invitation popup
                // (the one we got through the onRtmpInvite)
                switchToScreen(R.id.screen_wait);
                getRtmp().startWithInvitation(mIncomingInvitation);
                mIncomingInvitation = null; // important!
                break;
            case R.id.button_quick_game:
                // user wants to play against a random opponent right now
                switchToScreen(R.id.screen_wait);
                getRtmp().startWithRandomOpponents(MIN_OPPONENTS, MAX_OPPONENTS);
                break;
            case R.id.button_click_me:
                // (gameplay) user clicked the "click me" button
                scoreOnePoint();
                break;
        }
    }

    @Override
    public void onStart() {
        // Activity just got to foreground, so the connection process is started. Show
        // wait screen until connected (at which point we'll get onSignInFailed or
        // onSignInSucceeded).
        switchToScreen(R.id.screen_wait);
        super.onStart();
    }

    @Override
    public void onBackPressed() {
        if (mCurScreen == R.id.screen_game) {
            if (mMultiplayer) {
                // must shut down the RTMP game cleanly:
                switchToScreen(R.id.screen_wait);
                getRtmp().end();
            } else {
                // end right away
                mSecondsLeft = 0;
                stopGameClock();
                switchToMainScreen();
            }
        } else {
            super.onBackPressed();
        }
    }

    // Called when we get an invitation to play a game. We react by showing that to the user.
    @Override
    public void onRtmpInvitation(Invitation invitation) {
        // We got an invitation to play a game! So, store it in mIncomingInvitationId
        // and show the popup on the screen.
        mIncomingInvitation = invitation;
        String inviter = invitation.getInviter().getDisplayName();
        ((TextView) findViewById(R.id.incoming_invitation_text)).setText(inviter + " " +
                getString(R.string.is_inviting_you));
        switchToScreen(mCurScreen); // This will show the invitation popup
    }

    @Override
    public void onRtmpPreparing() {
        // show "please wait" screen while preparing to play
        switchToScreen(R.id.screen_wait);
    }

    @Override
    public void onRtmpStarted() {
        // start the game!
        switchToScreen(R.id.screen_game);
        startGame(true);
    }

    @Override
    public void onRtmpEnding(int reason) {
        // show "please wait" screen while preparing to end
        switchToScreen(R.id.screen_wait);
    }

    @Override
    public void onRtmpEnded(int reason) {
        stopGameClock();
        switchToMainScreen();
        switch (reason) {
            case RtmpHelper.END_REASON_ERROR:
                showAlert(getString(R.string.error), getString(R.string.game_problem));
                break;
            case RtmpHelper.END_REASON_INSUFFICIENT_PLAYERS:
                showAlert(getString(R.string.error), getString(R.string.not_enough_players));
                break;
        }
    }

    @Override
    public void onRtmpAdded(Participant participant) {
        updatePeerScoresDisplay();
    }

    @Override
    public void onRtmpDropped(Participant participant) {
        updatePeerScoresDisplay();
        if (getRtmp().amIAlone()) {
            // everyone left :-(
            getRtmp().end();
        }
    }

    // Start the gameplay phase of the game.
    void startGame(boolean multiplayer) {
        mSecondsLeft = GAME_DURATION;
        mScore = 0;
        mParticipantScore.clear();
        mFinishedParticipants.clear();
        mMultiplayer = multiplayer;
        updateScoreDisplay();
        broadcastScore(false);
        switchToScreen(R.id.screen_game);

        findViewById(R.id.button_click_me).setVisibility(View.VISIBLE);

        // run the gameTick() method every second to update the game.
        mHandler.postDelayed(mClockTickRunnable = new Runnable() {
            @Override
            public void run() {
                if (mSecondsLeft >= 0) {
                    gameTick();
                    mHandler.postDelayed(this, 1000);
                }
            }
        }, 1000);
    }

    void stopGameClock() {
        if (mClockTickRunnable != null) {
            mHandler.removeCallbacks(mClockTickRunnable);
            mClockTickRunnable = null;
        }
    }

    // Game tick -- update countdown, check if game ended.
    void gameTick() {
        if (mSecondsLeft > 0) {
            --mSecondsLeft;
            // update countdown on the screen
            ((TextView) findViewById(R.id.countdown)).setText("0:" +
                    (mSecondsLeft < 10 ? "0" : "") + String.valueOf(mSecondsLeft));
        } else {
            // finish game
            findViewById(R.id.button_click_me).setVisibility(View.GONE);
            broadcastScore(true); // broadcast final score to peers
        }
    }

    // indicates the player scored one point
    void scoreOnePoint() {
        if (mSecondsLeft > 0) {
            ++mScore;
            updateScoreDisplay();
            updatePeerScoresDisplay();
            // broadcast our new score to our peers
            broadcastScore(false);
        }
    }

    // Called when we get a message from the network
    @Override
    public void onRtmpMessage(RealTimeMessage rtm) {
        byte[] buf = rtm.getMessageData();
        String sender = rtm.getSenderParticipantId();
        Log.d(TAG, "Message received: " + (char) buf[0] + "/" + (int) buf[1]);
        char type = (char) buf[0];
        int score = (int)buf[1];

        if (type != 'F' && type != 'U') {
            Log.e(TAG, "*** Unknown message type received.");
            return;
        }

        // peer sent us a update score
        int prevScore = mParticipantScore.containsKey(sender) ? mParticipantScore.get(sender) : 0;
        if (score > prevScore) {
            // this check is necessary because packets may arrive out of order, so we
            // should only ever consider the highest score we received.
            mParticipantScore.put(sender, score);

            // update the scores on the screen
            updatePeerScoresDisplay();
        }

        // if it's a final score, mark this participant as having finished the game
        if (type == 'F') {
            mFinishedParticipants.add(rtm.getSenderParticipantId());
        }
    }

    // Broadcast my score to everybody else.
    void broadcastScore(boolean finalScore) {
        if (mMultiplayer && getRtmp().isPlaying()) {
            // Broadcast score. If final, use a reliable message. If not final, use unreliable.
            boolean useReliableMessage = finalScore;
            mMsgBuf[0] = (byte) (finalScore ? 'F' : 'U');
            mMsgBuf[1] = (byte) mScore;
            getRtmp().broadcast(useReliableMessage, mMsgBuf);
        }
    }

    void switchToScreen(int screenId) {
        // make the requested screen visible; hide all others.
        for (int id : SCREENS) {
            findViewById(id).setVisibility(screenId == id ? View.VISIBLE : View.GONE);
        }
        mCurScreen = screenId;

        // should we show the invitation popup?
        boolean showInvPopup;
        if (mIncomingInvitation == null) {
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
        switchToScreen(isSignedIn() ? R.id.screen_main : R.id.screen_sign_in);
    }

    // updates the label that shows my score
    void updateScoreDisplay() {
        ((TextView) findViewById(R.id.my_score)).setText(formatScore(mScore));
    }

    // formats a score as a three-digit number
    String formatScore(int i) {
        String s = String.valueOf(i < 0 ? 0 : i);
        return s.length() == 1 ? "00" + s : s.length() == 2 ? "0" + s : s;
    }

    // updates the screen with the scores from our peers
    void updatePeerScoresDisplay() {
        ((TextView) findViewById(R.id.score0)).setText(formatScore(mScore) + " - Me");
        int[] arr = { R.id.score1, R.id.score2, R.id.score3 };
        int i = 0;

        if (getRtmp().isPlaying()) {
            for (Participant p : getRtmp().getConnectedParticipants(false)) {
                String pid = p.getParticipantId();
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

    // Checks that the developer (that's you!) read the instructions. IMPORTANT:
    // a method like this SHOULD NOT EXIST in your production app!
    boolean verifyPlaceholderIdsReplaced() {
        final boolean CHECK_PKGNAME = true; // set to false to disable check
                                             // (not recommended!)

        // Did the developer forget to change the package name?
        if (CHECK_PKGNAME && getPackageName().startsWith("com.google.example."))
            return false;

        // Did the developer forget to replace a placeholder ID?
        int res_ids[] = new int[] {
                R.string.app_id
        };
        for (int i : res_ids) {
            if (getString(i).equalsIgnoreCase("ReplaceMe"))
                return false;
        }
        return true;
    }
}
