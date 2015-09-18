/* Copyright (C) 2014 Google Inc.
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

package com.google.example.games.tq2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.event.Events;
import com.google.android.gms.games.quest.Quest;
import com.google.android.gms.games.quest.QuestBuffer;
import com.google.android.gms.games.quest.QuestUpdateListener;
import com.google.android.gms.games.quest.Quests;
import com.google.android.gms.plus.Plus;
import com.google.example.games.basegameutils.BaseGameUtils;

import java.io.UnsupportedEncodingException;

/**
 * Trivial Quest 2. A sample game that uses Play Games Services to
 * manage events and quests. The user clicks attack buttons to trigger Events.
 * Using the event data, you can configure quests and milestones to trigger
 * experiences in your games. The most exciting part is that you can instrument
 * in-game bonus rewards and determine the criteria for unlocking them after your
 * game is published by instrumenting in-game events as we're demonstrating here.
 *
 * @author Gus Class (Google)
 *
 */
public class MainActivity extends Activity implements View.OnClickListener,
        QuestUpdateListener, GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks {

    private static final String TAG = "TrivialQuest2";
    private static final int RC_SIGN_IN = 9001;
    private static EventCallback ec;
    private static QuestCallback qc;
    private ResultCallback<Quests.ClaimMilestoneResult> mClaimMilestoneResultCallback;

    // Client used to interact with Google APIs
    private GoogleApiClient mGoogleApiClient;

    // Are we currently resolving a connection failure?
    private boolean mResolvingConnectionFailure = false;

    // Has the user clicked the sign-in button?
    private boolean mSignInClicked = false;

    // Automatically start the sign-in flow when the Activity starts
    private boolean mAutoStartSignInFlow = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create the Google API Client with access to Plus and Games
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API).addScope(Plus.SCOPE_PLUS_LOGIN)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .build();

        setContentView(R.layout.activity_main);
        findViewById(R.id.button_sign_in).setOnClickListener(this);
        findViewById(R.id.button_sign_out).setOnClickListener(this);
        findViewById(R.id.button_red).setOnClickListener(this);
        findViewById(R.id.button_green).setOnClickListener(this);
        findViewById(R.id.button_blue).setOnClickListener(this);
        findViewById(R.id.button_yellow).setOnClickListener(this);
        findViewById(R.id.button_quests).setOnClickListener(this);

        // Initialize the callbacks for API data return.
        ec = new EventCallback(this);
        qc = new QuestCallback(this);

        // Set the callback for when milestones are claimed.
        mClaimMilestoneResultCallback = new ResultCallback<Quests.ClaimMilestoneResult>() {
            @Override
            public void onResult(Quests.ClaimMilestoneResult result) {
                onMilestoneClaimed(result);
            }
        };

        // List event stats.
        loadAndPrintEvents();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart(): connecting");
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop(): disconnecting");
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == RC_SIGN_IN) {
            mSignInClicked = false;
            mResolvingConnectionFailure = false;
            if (resultCode == RESULT_OK) {
                mGoogleApiClient.connect();
            } else {
                BaseGameUtils.showActivityResultError(this, requestCode, resultCode, R.string.signin_other_error);
            }
        }
    }

    /**
     * Lists all of the events for this app as well as the event counts.
     */
    public void loadAndPrintEvents() {
        // Load up a list of events
        com.google.android.gms.common.api.PendingResult<Events.LoadEventsResult> pr =
                Games.Events.load(mGoogleApiClient, true);

        // Set the callback to the EventCallback class.
        pr.setResultCallback(ec);
    }

    /**
     * Class implementation for handling Event results.
     */
    class EventCallback implements com.google.android.gms.common.api.ResultCallback {
        /**
         * The activity that creates the callback handler class.
         */
        MainActivity m_parent;

        public EventCallback (MainActivity main){
            m_parent = main;
        }

        /**
         * Receives event results.
         *
         * @param result The result from the Event.
         */
        public void onResult(com.google.android.gms.common.api.Result result) {
            Events.LoadEventsResult r = (Events.LoadEventsResult)result;
            com.google.android.gms.games.event.EventBuffer eb = r.getEvents();

            String message = "Current stats: \n";

            Log.i(TAG, "number of events: " + eb.getCount());

            String currentEvent = "";
            for(int i=0; i < eb.getCount(); i++) {
                message += "event: " + eb.get(i).getName() + " " + eb.get(i).getEventId() +
                        " " + eb.get(i).getValue() + "\n";
            }
            eb.close();

            Toast.makeText(m_parent, message, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * List all of the quests for this app.
     */
    public void loadAndListQuests() {
        int[] selection = {Quests.SELECT_OPEN, Quests.SELECT_COMPLETED_UNCLAIMED,
                Quests.SELECT_ACCEPTED};
        com.google.android.gms.common.api.PendingResult<Quests.LoadQuestsResult> pr =
                Games.Quests.load(mGoogleApiClient, selection,
                Quests.SORT_ORDER_ENDING_SOON_FIRST, true);

        // Set the callback to the Quest callback.
        pr.setResultCallback(qc);
    }

    /**
     * Class implementation for handling Quest results.
     */
    class QuestCallback implements com.google.android.gms.common.api.ResultCallback {
        /**
         * The activity that creates the callback handler class.
         */
        MainActivity m_parent;

        public QuestCallback (MainActivity main){
            m_parent = main;
        }

        /**
         * Receives Quest results.
         *
         * @param result The result from the Quest.
         */
        public void onResult(com.google.android.gms.common.api.Result result) {
            Quests.LoadQuestsResult r = (Quests.LoadQuestsResult)result;
            QuestBuffer qb = r.getQuests();

            String message = "Current quest details: \n";

            Log.i(TAG, "Number of quests: " + qb.getCount());

            String currentEvent = "";
            for(int i=0; i < qb.getCount(); i++) {
                message += "Quest: " + qb.get(i).getName() + " id: " + qb.get(i).getQuestId();
            }
            qb.close();

            Toast.makeText(m_parent, message, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Event handler for when Quests are completed.
     *
     * @param quest The quest that has been completed.
     */
    @Override
    public void onQuestCompleted(Quest quest) {
        // create a message string indicating that the quest was successfully completed
        String message = "You successfully completed quest " + quest.getName();

        // Print out message for debugging purposes.
        Log.i(TAG, message);

        // Create a custom toast to indicate the quest was successfully completed.
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

        // Claim the quest reward.
        Games.Quests.claim(
                mGoogleApiClient,
                quest.getQuestId(),
                quest.getCurrentMilestone().getMilestoneId())
                .setResultCallback(mClaimMilestoneResultCallback);
    }

    public void onMilestoneClaimed(Quests.ClaimMilestoneResult result){
        // Process the RewardData binary array to provide a specific reward and present the
        // information to the user.
        try {
            if (result.getStatus().isSuccess()){
                String reward = new String(result.getQuest().getCurrentMilestone().
                        getCompletionRewardData(),
                        "UTF-8");
                // TOAST to let the player what they were rewarded.
                Toast.makeText(this, "Congratulations, you got a " + reward,
                        Toast.LENGTH_LONG).show();
            } else {
                Log.e(TAG, "Reward was not claimed due to error.");
                Toast.makeText(this, "Reward was not claimed due to error.",
                        Toast.LENGTH_LONG).show();
            }
        } catch (UnsupportedEncodingException uee) {
            uee.printStackTrace();
        }
    }


    /**
     * Shows the "sign in" bar (explanation and button).
     */
    private void showSignInBar() {
        findViewById(R.id.sign_in_bar).setVisibility(View.VISIBLE);
        findViewById(R.id.sign_out_bar).setVisibility(View.GONE);
    }

    /**
     * Shows the "sign out" bar (explanation and button).
     */
    private void showSignOutBar() {
        findViewById(R.id.sign_in_bar).setVisibility(View.GONE);
        findViewById(R.id.sign_out_bar).setVisibility(View.VISIBLE);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected(): connected to Google APIs");
        showSignOutBar();

        // Start the quest listener.
        Games.Quests.registerQuestUpdateListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended(): attempting to reconnect");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed(): attempting to resolve");
        if (mResolvingConnectionFailure) {
            Log.d(TAG, "onConnectionFailed(): already resolving, ignoring");
            return;
        }

        if (mSignInClicked || mAutoStartSignInFlow) {
            mAutoStartSignInFlow = false;
            mSignInClicked = false;
            mResolvingConnectionFailure = true;
            if (!BaseGameUtils.resolveConnectionFailure(this, mGoogleApiClient, connectionResult,
                    RC_SIGN_IN, getString(R.string.signin_other_error))) {
                mResolvingConnectionFailure = false;
            }
        }
        showSignInBar();
    }


    /**
     * Shows the current list of available quests.
     */
    public void showQuests() {
        int[] selection = {Quests.SELECT_OPEN, Quests.SELECT_COMPLETED_UNCLAIMED,
                Quests.SELECT_ACCEPTED};
        android.content.Intent questsIntent = Games.Quests.getQuestsIntent(mGoogleApiClient,
                selection);
        startActivityForResult(questsIntent, 0);
    }


    /**
     * Click handler for the activity. This method is used to determine which monster the user
     * has defeated by clicking the according button.
     *
     * @param view Contains event data such as which button was clicked.
     */
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_sign_in:
                // Check to see the developer who's running this sample code read the instructions :-)
                // NOTE: this check is here only because this is a sample! Don't include this
                // check in your actual production app.
                if(!BaseGameUtils.verifySampleSetup(this, R.string.app_id, R.string.event_red)) {
                    Log.w(TAG, "*** Warning: setup problems detected. Sign in may not work!");
                }

                // start the sign-in flow
                mSignInClicked = true;
                mGoogleApiClient.connect();
                break;
            case R.id.button_sign_out:
                // sign out.
                mSignInClicked = false;
                Games.signOut(mGoogleApiClient);
                if (mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.disconnect();
                }
                showSignInBar();
                break;
            case R.id.button_quests:
                showQuests();
                break;
            case R.id.button_red:
                if (mGoogleApiClient.isConnected()) {
                    Games.Events.increment(mGoogleApiClient, getString(R.string.event_red), 1);
                }
                BaseGameUtils.makeSimpleDialog(this,
                        getString(R.string.victory), getString(R.string.defeat_red_monster)).show();
                break;
            case R.id.button_blue:
                if (mGoogleApiClient.isConnected()) {
                    Games.Events.increment(mGoogleApiClient, getString(R.string.event_blue), 1);
                }
                BaseGameUtils.makeSimpleDialog(this,
                        getString(R.string.victory), getString(R.string.defeat_blue_monster)).show();
                break;
            case R.id.button_green:
                if (mGoogleApiClient.isConnected()) {
                    Games.Events.increment(mGoogleApiClient, getString(R.string.event_green), 1);
                }
                BaseGameUtils.makeSimpleDialog(this,
                        getString(R.string.victory), getString(R.string.defeat_green_monster)).show();
                break;
            case R.id.button_yellow:
                if (mGoogleApiClient.isConnected()) {
                    Games.Events.increment(mGoogleApiClient, getString(R.string.event_yellow), 1);
                }
                BaseGameUtils.makeSimpleDialog(this,
                        getString(R.string.victory), getString(R.string.defeat_yellow_monster)).show();
                break;
            default:
                break;

        }
    }
}
