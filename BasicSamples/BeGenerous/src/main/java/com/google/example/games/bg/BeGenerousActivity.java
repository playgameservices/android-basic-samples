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

package com.google.example.games.bg;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.request.GameRequest;
import com.google.android.gms.games.request.GameRequestBuffer;
import com.google.android.gms.games.request.OnRequestReceivedListener;
import com.google.android.gms.games.request.Requests;
import com.google.android.gms.games.request.Requests.LoadRequestsResult;
import com.google.android.gms.games.request.Requests.UpdateRequestsResult;
import com.google.android.gms.games.stats.PlayerStats;
import com.google.android.gms.games.stats.Stats;
import com.google.android.gms.plus.Plus;
import com.google.example.games.basegameutils.BaseGameUtils;

/**
 * Be Generous. A sample game that sets up the Google Play game services API and
 * allows the user to click buttons to give gifts, request gifts, and accept
 * gifts. Win by being the most generous!
 *
 * @author Dan Galpin (Google) and Wolff Dobson (Google)
 */
public class BeGenerousActivity extends Activity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener {

    private static final String TAG = "BeGenerous";

    private static final int SHOW_INBOX = 1;

    private static final int SEND_GIFT_CODE = 2;

    private static final int SEND_REQUEST_CODE = 3;

    /** Default lifetime of a request, 1 week. */
    private static final int DEFAULT_LIFETIME = 7;

    /** Icon to be used to send gifts/requests */
    private Bitmap mGiftIcon;

    // Request code used to invoke sign in user interactions.
    private static final int RC_SIGN_IN = 9001;

    // Client used to interact with Google APIs.
    private GoogleApiClient mGoogleApiClient;

    // Are we currently resolving a connection failure?
    private boolean mResolvingConnectionFailure = false;

    // Has the user clicked the sign-in button?
    private boolean mSignInClicked = false;

    // Set to true to automatically start the sign in flow when the Activity starts.
    // Set to false to require the user to click the button in order to sign in.
    private boolean mAutoStartSignInFlow = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");

        // Create the Google Api Client with access to Plus and Games
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API).addScope(Plus.SCOPE_PLUS_LOGIN)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .build();

        // Set up click listeners
        setContentView(R.layout.activity_main);
        findViewById(R.id.button_open_inbox).setOnClickListener(this);
        findViewById(R.id.button_send_gift).setOnClickListener(this);
        findViewById(R.id.button_send_request).setOnClickListener(this);
        findViewById(R.id.button_sign_in).setOnClickListener(this);
        findViewById(R.id.button_sign_out).setOnClickListener(this);

        mGiftIcon = BitmapFactory.decodeResource(getResources(),
                R.drawable.ic_send_gift);
    }

    // Shows the "sign in" bar (explanation and button).
    private void showSignInBar() {
        findViewById(R.id.sign_in_bar).setVisibility(View.VISIBLE);
        findViewById(R.id.sign_out_bar).setVisibility(View.GONE);
        ImageView vw = (ImageView) findViewById(R.id.avatar);
        vw.setImageBitmap(null);
        TextView name = (TextView)findViewById(R.id.playerName);
        name.setText("");
        TextView email = (TextView)findViewById((R.id.playerEmail));
        email.setText("");

    }

    // Shows the "sign out" bar (explanation and button).
    private void showSignOutBar() {
        findViewById(R.id.sign_in_bar).setVisibility(View.GONE);
        findViewById(R.id.sign_out_bar).setVisibility(View.VISIBLE);

        Player player = Games.Players.getCurrentPlayer(mGoogleApiClient);
        String url = player.getIconImageUrl();
        TextView name = (TextView)findViewById(R.id.playerName);
        name.setText(player.getDisplayName());
        if (url != null) {
            ImageView vw = (ImageView) findViewById(R.id.avatar);

            // load the image in the background.
            new DownloadImageTask(vw).execute(url);
         }
        String email = Plus.AccountApi.getAccountName(mGoogleApiClient);
        TextView emailView = (TextView)findViewById((R.id.playerEmail));
        emailView.setText(email);
    }

    /**
     * AsyncTask to download an image from a URL and set the image to the
     * ImageView that is passed in on the constructor.
     */
    class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        @Override
        protected Bitmap doInBackground(String... strings) {
            Bitmap mIcon11 = null;
            String url = strings[0];
            try {
                InputStream in = new URL(url).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
            return mIcon11;
        }
        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
            bmImage.setVisibility(View.VISIBLE);
        }
    }

    // Count GameRequests in a GameRequestBuffer that have not yet expired
    private int countNotExpired(GameRequestBuffer buf) {
        if (buf == null) {
            return 0;
        }

        int giftCount = 0;
        for (GameRequest gr : buf) {
            if (gr.getExpirationTimestamp() > System.currentTimeMillis()) {
                giftCount++;
            }
        }
        return giftCount;
    }

    // Called back after you load the current requests
    private final ResultCallback<Requests.LoadRequestsResult> mLoadRequestsCallback =
            new ResultCallback<Requests.LoadRequestsResult>() {

                @Override
                public void onResult(LoadRequestsResult result) {
                    int giftCount = countNotExpired(result.getRequests(GameRequest.TYPE_GIFT));
                    int wishCount = countNotExpired(result.getRequests(GameRequest.TYPE_WISH));

                    ((TextView) findViewById(R.id.tv_gift_count)).setText(String
                            .format(getString(R.string.gift_count), giftCount));
                    ((TextView) findViewById(R.id.tv_request_count)).setText(String
                            .format(getString(R.string.request_count), wishCount));
                }

            };

    // Changes the numbers at the top of the layout
    private void updateRequestCounts() {
        PendingResult<Requests.LoadRequestsResult> result = Games.Requests
                .loadRequests(mGoogleApiClient,
                        Requests.REQUEST_DIRECTION_INBOUND,
                        GameRequest.TYPE_ALL,
                        Requests.SORT_ORDER_EXPIRING_SOON_FIRST);
        result.setResultCallback(mLoadRequestsCallback);
    }

    // This shows how to set up a listener for requests received. It is not
    // necessary; it only is useful if you do not want the default notifications
    // to happen when someone sends a request to someone.
    private final OnRequestReceivedListener mRequestListener = new OnRequestReceivedListener() {

        /*
         * (non-Javadoc)
         *
         * @see com.google.android.gms.games.request.OnRequestReceivedListener#
         * onRequestReceived(com.google.android.gms.games.request.GameRequest)
         */
        @Override
        public void onRequestReceived(GameRequest request) {
            int requestStringResource;
            switch (request.getType()) {
                case GameRequest.TYPE_GIFT:
                    requestStringResource = R.string.new_gift_received;
                    break;
                case GameRequest.TYPE_WISH:
                    requestStringResource = R.string.new_request_received;
                    break;
                default:
                    return;
            }
            Toast.makeText(BeGenerousActivity.this, requestStringResource,
                    Toast.LENGTH_LONG).show();
            updateRequestCounts();
        }

        /*
         * (non-Javadoc)
         *
         * @see com.google.android.gms.games.request.OnRequestReceivedListener#
         * onRequestRemoved(java.lang.String)
         */
        @Override
        public void onRequestRemoved(String requestId) {
            updateRequestCounts();
        }
    };


    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "onConnected() called. Sign in successful!");
        showSignOutBar();
        checkPlayerStats();

        // This is *NOT* required; if you do not register a handler for
        // request events, you will get standard notifications instead.
        Games.Requests.registerRequestListener(mGoogleApiClient, mRequestListener);

        if (connectionHint != null) {
            ArrayList<GameRequest> requests;
            // Do we have any requests pending? (getGameRequestsFromBundle never returns null
            requests = Games.Requests.getGameRequestsFromBundle(connectionHint);
            if (!requests.isEmpty()) {
                // We have requests in onConnected's connectionHint.
                Log.d(TAG, "onConnected: connection hint has " + requests.size() + " request(s)");
            }
            Log.d(TAG, "===========\nRequests count " + requests.size());
            // Use regular handler
            handleRequests(requests);
        }

        // Our sample displays the request counts.
        updateRequestCounts();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended() called. Trying to reconnect.");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed() called, result: " + connectionResult);

        if (mResolvingConnectionFailure) {
            Log.d(TAG, "onConnectionFailed() ignoring connection failure; already resolving.");
            return;
        }

        if (mSignInClicked || mAutoStartSignInFlow) {
            mAutoStartSignInFlow = false;
            mSignInClicked = false;
            mResolvingConnectionFailure = BaseGameUtils
                    .resolveConnectionFailure(this, mGoogleApiClient,
                            connectionResult, RC_SIGN_IN, getString(R.string.signin_other_error));
        }
        showSignInBar();
    }

    /**
     * Show a send gift or send wish request using startActivityForResult.
     *
     * @param type
     *            the type of GameRequest (gift or wish) to show
     */
    private void showSendIntent(int type) {
        // Make sure we have a valid API client.
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {

            String description;
            int intentCode;
            Bitmap icon;
            switch (type) {
            case GameRequest.TYPE_GIFT:
                description = getString(R.string.send_gift_description);
                intentCode = SEND_GIFT_CODE;
                icon = mGiftIcon;
                break;
            case GameRequest.TYPE_WISH:
                description = getString(R.string.send_request_description);
                intentCode = SEND_REQUEST_CODE;
                icon = mGiftIcon;
                break;
            default:
                return;
            }
            Intent intent = Games.Requests.getSendIntent(mGoogleApiClient, type,
                    "".getBytes(), DEFAULT_LIFETIME, icon, description);
            startActivityForResult(intent, intentCode);
        }
    }

    private String getRequestsString(ArrayList<GameRequest> requests) {
        if (requests.size() == 0) {
            return "You have no requests to accept.";
        }

        if (requests.size() == 1) {
            return "Do you want to accept this request from "
                    + requests.get(0).getSender().getDisplayName() + "?";
        }

        StringBuffer retVal = new StringBuffer(
                "Do you want to accept the following requests?\n\n");

        for (GameRequest request : requests) {
            retVal.append("  A "
                    + (request.getType() == GameRequest.TYPE_GIFT ? "gift"
                            : "game request") + " from "
                    + request.getSender().getDisplayName() + "\n");
        }

        return retVal.toString();
    }

    // Actually accepts the requests
    private void acceptRequests(ArrayList<GameRequest> requests) {
        // Attempt to accept these requests.
        ArrayList<String> requestIds = new ArrayList<String>();

        /**
         * Map of cached game request ID to its corresponding game request
         * object.
         */
        final HashMap<String, GameRequest> gameRequestMap = new HashMap<String, GameRequest>();

        // Cache the requests.
        for (GameRequest request : requests) {
            String requestId = request.getRequestId();
            requestIds.add(requestId);
            gameRequestMap.put(requestId, request);

            Log.d(TAG, "Processing request " + requestId);
        }
        // Accept the requests.
        Games.Requests.acceptRequests(mGoogleApiClient, requestIds).setResultCallback(
                new ResultCallback<UpdateRequestsResult>() {
                    @Override
                    public void onResult(UpdateRequestsResult result) {
                        int numGifts = 0;
                        int numRequests = 0;
                        // Scan each result outcome.
                        for (String requestId : result.getRequestIds()) {
                            // We must have a local cached copy of the request
                            // and the request needs to be a
                            // success in order to continue.
                            if (!gameRequestMap.containsKey(requestId)
                                    || result.getRequestOutcome(requestId) != Requests.REQUEST_UPDATE_OUTCOME_SUCCESS) {
                                continue;
                            }
                            // Update succeeded here. Find the type of request
                            // and act accordingly. For wishes, a
                            // responding gift will be automatically sent.
                            switch (gameRequestMap.get(requestId).getType()) {
                            case GameRequest.TYPE_GIFT:
                                // Toast the player!
                                ++numGifts;
                                break;
                            case GameRequest.TYPE_WISH:
                                ++numRequests;
                                break;
                            }
                        }

                        if (numGifts != 0) {
                            // Toast our gifts.
                            Toast.makeText(
                                    BeGenerousActivity.this,
                                    String.format(
                                            getString(R.string.gift_toast),
                                            numGifts), Toast.LENGTH_LONG)
                                    .show();
                        }
                        if (numGifts != 0 || numRequests != 0) {
                            // if the user accepted any gifts or requests,
                            // update
                            // the displayed counts
                            updateRequestCounts();
                        }
                    }
                });

    }

    // Deal with any requests that are incoming, either from a bundle from the
    // app starting via notification, or from the inbox. Players should give
    // explicit approval to accept any gift or request, so we pop up a dialog.
    private void handleRequests(ArrayList<GameRequest> requests) {
        if (requests == null) {
            return;
        }

        // Must have final for anonymous function
        final ArrayList<GameRequest> theRequests = requests;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getRequestsString(requests))
                .setPositiveButton("Absolutely!",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                acceptRequests(theRequests);
                            }
                        })
                .setNegativeButton("No thanks",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                // Do nothing---requests will remain un-created.
                            }
                        });
        // Create the AlertDialog object and return it
        builder.create().show();
    }

    public void checkPlayerStats() {
        PendingResult<Stats.LoadPlayerStatsResult> result = Games.Stats.loadPlayerStats(
                mGoogleApiClient, false /* forceReload */);
        result.setResultCallback(new ResultCallback<Stats.LoadPlayerStatsResult>() {
            public void onResult(Stats.LoadPlayerStatsResult result) {
                Status status = result.getStatus();
                if (status.isSuccess()) {
                    PlayerStats stats = result.getPlayerStats();
                    if (stats != null) {
                        Log.d(TAG, "Player stats loaded");
                        if (stats.getDaysSinceLastPlayed() > 7) {
                            Log.d(TAG, "It's been longer than a week");
                        }
                        if (stats.getNumberOfSessions() > 1000) {
                            Log.d(TAG, "Veteran player");
                        }
                    }
                } else {
                    Log.d(TAG, "Failed to fetch Stats Data status: " + status.getStatusMessage());
                }
            }
        });
    }

    // Response to inbox check
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case SEND_REQUEST_CODE:
                if (resultCode == GamesActivityResultCodes.RESULT_SEND_REQUEST_FAILED) {
                    Toast.makeText(this, "FAILED TO SEND REQUEST!",
                            Toast.LENGTH_LONG).show();
                }
                break;
            case SEND_GIFT_CODE:
                if (resultCode == GamesActivityResultCodes.RESULT_SEND_REQUEST_FAILED) {
                    Toast.makeText(this, "FAILED TO SEND GIFT!", Toast.LENGTH_LONG)
                            .show();
                }
                break;
            case SHOW_INBOX:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    handleRequests(Games.Requests
                            .getGameRequestsFromInboxResponse(data));
                } else {
                    Log.e(TAG, "Failed to process inbox result: resultCode = "
                            + resultCode + ", data = "
                            + (data == null ? "null" : "valid"));
                }
                break;
            case RC_SIGN_IN:
                Log.d(TAG, "onActivityResult with requestCode == RC_SIGN_IN, responseCode="
                        + resultCode + ", intent=" + data);
                mSignInClicked = false;
                mResolvingConnectionFailure = false;
                if (resultCode == RESULT_OK) {
                    mGoogleApiClient.connect();
                } else {
                    BaseGameUtils.showActivityResultError(this, requestCode, resultCode, R.string.signin_other_error);
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_sign_in:
                // Check to see the developer who's running this sample code read the instructions :-)
                // NOTE: this check is here only because this is a sample! Don't include this
                // check in your actual production app.
                if (!BaseGameUtils.verifySampleSetup(this, R.string.app_id)) {
                    Log.w(TAG, "*** Warning: setup problems detected. Sign in may not work!");
                }

                // start the sign-in flow
                Log.d(TAG, "Sign-in button clicked");
                mSignInClicked = true;
                mGoogleApiClient.connect();
                break;
            case R.id.button_sign_out:
                // sign out.
                Log.d(TAG, "Sign-out button clicked");
                mSignInClicked = false;
                Games.signOut(mGoogleApiClient);
                mGoogleApiClient.disconnect();
                showSignInBar();
                break;
            case R.id.button_send_gift:
                // send gift!
                showSendIntent(GameRequest.TYPE_GIFT);
                break;
            case R.id.button_send_request:
                // request gift!
                showSendIntent(GameRequest.TYPE_WISH);
                break;
            case R.id.button_open_inbox:
                // show inbox!
                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    startActivityForResult(
                            Games.Requests.getInboxIntent(mGoogleApiClient),
                            SHOW_INBOX);
                }
                break;
        }
    }
}
