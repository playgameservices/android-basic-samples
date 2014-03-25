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

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.request.GameRequest;
import com.google.android.gms.games.request.GameRequestBuffer;
import com.google.android.gms.games.request.OnRequestReceivedListener;
import com.google.android.gms.games.request.Requests;
import com.google.android.gms.games.request.Requests.LoadRequestsResult;
import com.google.android.gms.games.request.Requests.UpdateRequestsResult;
import com.google.example.games.basegameutils.BaseGameActivity;

/**
 * Be Generous. A sample game that sets up the Google Play game services API and
 * allows the user to click buttons to give gifts, request gifts, and accept
 * gifts. Win by being the most generous!
 * 
 * @author Dan Galpin (Google) and Wolff Dobson (Google)
 */
public class BeGenerousActivity extends BaseGameActivity implements
        View.OnClickListener {
    private static boolean DEBUG_ENABLED = true;
    private static final String TAG = "BeGenerous";
    private static final int SHOW_INBOX = 1;
    private static final int SEND_GIFT_CODE = 2;
    private static final int SEND_REQUEST_CODE = 3;

    /** Default lifetime of a request, 1 week. */
    private static final int DEFAULT_LIFETIME = 7;

    /** Icon to be used to send gifts/requests */
    private Bitmap mGiftIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        enableDebugLog(DEBUG_ENABLED);
        super.onCreate(savedInstanceState);

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
    }

    // Shows the "sign out" bar (explanation and button).
    private void showSignOutBar() {
        findViewById(R.id.sign_in_bar).setVisibility(View.GONE);
        findViewById(R.id.sign_out_bar).setVisibility(View.VISIBLE);
    }

    // Called back after you load the current requests
    private final ResultCallback<Requests.LoadRequestsResult> mLoadRequestsCallback = new ResultCallback<Requests.LoadRequestsResult>() {

        @Override
        public void onResult(LoadRequestsResult result) {
            int giftCount = 0;
            int wishCount = 0;
            GameRequestBuffer buf;
            buf = result.getRequests(GameRequest.TYPE_GIFT);
            if (null != buf) {
                giftCount = buf.getCount();
            }
            buf = result.getRequests(GameRequest.TYPE_WISH);
            if (null != buf) {
                wishCount = buf.getCount();
            }
            // Update the counts in the layout
            ((TextView) findViewById(R.id.tv_gift_count)).setText(String
                    .format(getString(R.string.gift_count), giftCount));
            ((TextView) findViewById(R.id.tv_request_count)).setText(String
                    .format(getString(R.string.request_count), wishCount));
        }

    };

    // Changes the numbers at the top of the layout
    private void updateRequestCounts() {
        PendingResult<Requests.LoadRequestsResult> result = Games.Requests
                .loadRequests(getApiClient(),
                        Requests.REQUEST_DIRECTION_INBOUND,
                        GameRequest.TYPE_ALL,
                        Requests.SORT_ORDER_EXPIRING_SOON_FIRST);
        result.setResultCallback(mLoadRequestsCallback);
    }

    // This shows how to set up a listener for requests receieved. It is not
    // necessary; it only is useful if you do not want the default notifications
    // to
    // happen when someone sends a request to someone.
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
        }
    };

    /**
     * Called to notify us that sign in failed. Notice that a failure in sign in
     * is not necessarily due to an error; it might be that the user never
     * signed in, so our attempt to automatically sign in fails if the user has
     * not gone through the authorization flow. So our reaction to sign in
     * failure is to show the sign in button. When the user clicks that button,
     * the sign in process will start/resume.
     */
    @Override
    public void onSignInFailed() {
        // Sign-in has failed. So show the user the sign-in button
        // so they can click the "Sign-in" button.
        showSignInBar();
    }

    /**
     * Called to notify us that sign in succeeded. We react by loading the loot
     * from the cloud and updating the UI to show a sign-out button.
     */
    @Override
    public void onSignInSucceeded() {
        // Sign-in worked!
        showSignOutBar();

        // This is *NOT* required; if you do not register a handler for
        // request events, you will get standard notifications instead.
        Games.Requests
                .registerRequestListener(getApiClient(), mRequestListener);

        // Get any pending requests from the connection bundle
        ArrayList<GameRequest> requests = getGameHelper().getRequests();

        if (requests != null) {
            Log.d(TAG, "===========\nReqests count " + requests.size());
        } else {
            Log.d(TAG, "===========\nReqests are null");
        }
        // Use regular handler
        handleRequests(requests);
        // Make sure you don't handle these requests twice
        getGameHelper().clearRequests();

        // Our sample displays the request counts.
        updateRequestCounts();
    }

    /**
     * Show a send gift or send wish request using startActivityForResult.
     * 
     * @param type
     *            the type of GameRequest (gift or wish) to show
     */
    private void showSendIntent(int type) {
        // Make sure we have a valid API client.
        if (getGameHelper().isSignedIn()) {
            GoogleApiClient client = getApiClient();
            if (!client.isConnected()) {
                Log.i(TAG,
                        "Failed to show send intent, Google API client isn't connected!");
                return;
            }

            String description = "";
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
            Intent intent = Games.Requests.getSendIntent(client, type,
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
            retVal.append("  ¥ A "
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

        // Make sure we have a valid API client.
        GoogleApiClient client = getApiClient();

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
        Games.Requests.acceptRequests(client, requestIds).setResultCallback(
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
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
        case R.id.button_sign_in:
            // Check to see the developer who's running this sample code
            // read the instructions :-)
            // NOTE: this check is here only because this is a sample! Don't
            // include this
            // check in your actual production app.
            // if (!verifyPlaceholderIdsReplaced()) {
            // showAlert("Error: sample not correctly set up. See README!");
            // break;
            // }

            // start the sign-in flow
            beginUserInitiatedSignIn();
            break;
        case R.id.button_sign_out:
            // sign out.
            signOut();
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
            if (getGameHelper().isSignedIn()) {
                startActivityForResult(
                        Games.Requests.getInboxIntent(getApiClient()),
                        SHOW_INBOX);
            }
            break;
        }
    }

    /**
     * Checks that the developer (that's you!) read the instructions. IMPORTANT:
     * a method like this SHOULD NOT EXIST in your production app! It merely
     * exists here to check that anyone running THIS PARTICULAR SAMPLE did what
     * they were supposed to in order for the sample to work.
     */
    boolean verifyPlaceholderIdsReplaced() {
        final boolean CHECK_PKGNAME = true; // set to false to disable check
                                            // (not recommended!)

        // Did the developer forget to change the package name?
        if (CHECK_PKGNAME && getPackageName().startsWith("com.google.example.")) {
            Log.e(TAG,
                    "*** Sample setup problem: "
                            + "package name cannot be com.google.example.*. Use your own "
                            + "package name.");
            return false;
        }
        return true;
    }
}
