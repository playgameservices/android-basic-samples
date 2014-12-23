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

package com.google.example.games.catt2;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.RatingBar.OnRatingBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.snapshot.Snapshot;
import com.google.android.gms.games.snapshot.SnapshotMetadata;
import com.google.android.gms.games.snapshot.SnapshotMetadataChange;
import com.google.android.gms.games.snapshot.Snapshots;
import com.google.android.gms.plus.Plus;
import com.google.example.games.basegameutils.BaseGameUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

/**
 * Collect All the Stars sample. This sample demonstrates how to use the cloud save features
 * of the Google Play game services API. It's a "game" where there are several worlds
 * with several levels each, and on each level the player can get from 0 to 5 stars.
 * The progress of the player is saved to the cloud and kept in sync across all their devices.
 * If they earn 5 stars on level 1 on one device and then earn 4 stars on level 2 on a different
 * device, upon synchronizing the consolidated progress will be 5 stars on level 1 AND
 * 4 stars on level 2. If they clear the same level on two different devices, then the biggest
 * star rating of the two will apply.
 *
 * It's worth noting that this sample also works offline, and even when the player is not
 * signed in. In all cases, the progress is saved locally as well.
 *
 * @author Bruno Oliveira (Google)
 */
public class MainActivity extends Activity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener, OnRatingBarChangeListener {


    private static final String TAG = "CollectAllTheStars2";

    // Request code used to invoke sign in user interactions.
    private static final int RC_SIGN_IN = 9001;

    // Request code for listing saved games
    private static final int RC_LIST_SAVED_GAMES = 9002;

    // Request code for selecting a snapshot
    private static final int RC_SELECT_SNAPSHOT = 9003;

    // Request code for saving the game to a snapshot.
    private static final int RC_SAVE_SNAPSHOT = 9004;

    private static final int RC_LOAD_SNAPSHOT = 9005;

    // Client used to interact with Google APIs.
    private GoogleApiClient mGoogleApiClient;

    // Are we currently resolving a connection failure?
    private boolean mResolvingConnectionFailure = false;

    // Has the user clicked the sign-in button?
    private boolean mSignInClicked = false;

    // Set to true to automatically start the sign in flow when the Activity starts.
    // Set to false to require the user to click the button in order to sign in.
    private boolean mAutoStartSignInFlow = true;

    // current save game - serializable to and from the saved game
    SaveGame mSaveGame = new SaveGame();

    private String currentSaveName = "snapshotTemp";

    // world we're currently viewing
    int mWorld = 1;
    private static final int WORLD_MIN = 1;
    private static final int WORLD_MAX = 20;
    private static final int LEVELS_PER_WORLD = 12;

    // level we're currently "playing"
    int mLevel = 0;

    // state of "playing" - used to make the back button work correctly
    boolean mInLevel = false;

    // progress dialog we display while we're loading state from the cloud
    ProgressDialog mLoadingDialog = null;

    // whether we already loaded the state the first time (so we don't reload
    // every time the activity goes to the background and comes back to the foreground)
    boolean mAlreadyLoadedState = false;

    // the level buttons (the ones the user clicks to play a given level)
    final static int[] LEVEL_BUTTON_IDS = {
            R.id.button_level_1, R.id.button_level_2, R.id.button_level_3, R.id.button_level_4,
            R.id.button_level_5, R.id.button_level_6, R.id.button_level_7, R.id.button_level_8,
            R.id.button_level_9, R.id.button_level_10, R.id.button_level_11, R.id.button_level_12
    };

    // star strings (we use the Unicode BLACK STAR and WHITE STAR characters -- lazy graphics!)
    final static String[] STAR_STRINGS = {
            "\u2606\u2606\u2606\u2606\u2606", // 0 stars
            "\u2605\u2606\u2606\u2606\u2606", // 1 star
            "\u2605\u2605\u2606\u2606\u2606", // 2 stars
            "\u2605\u2605\u2605\u2606\u2606", // 3 stars
            "\u2605\u2605\u2605\u2605\u2606", // 4 stars
            "\u2605\u2605\u2605\u2605\u2605", // 5 stars
    };

    // Members related to the conflict resolution chooser of Snapshots.
    final static int MAX_SNAPSHOT_RESOLVE_RETRIES = 3;

    /**
     * You can capture the Snapshot selection intent in the onActivityResult method. The result
     * either indicates a new Snapshot was created (EXTRA_SNAPSHOT_NEW) or was selected.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == RC_SIGN_IN) {
            Log.d(TAG, "onActivityResult with requestCode == RC_SIGN_IN, responseCode="
                    + resultCode + ", intent=" + intent);
            mSignInClicked = false;
            mResolvingConnectionFailure = false;
            if (resultCode == RESULT_OK) {
                mGoogleApiClient.connect();
            } else {
                BaseGameUtils.showActivityResultError(this, requestCode, resultCode, R.string.signin_other_error);
            }
        }
        // the standard snapshot selection intent
        else if (requestCode == RC_LIST_SAVED_GAMES) {
            if (intent != null) {
                if (intent.hasExtra(Snapshots.EXTRA_SNAPSHOT_METADATA)) {
                    // Load a snapshot.
                    SnapshotMetadata snapshotMetadata =
                            intent.getParcelableExtra(Snapshots.EXTRA_SNAPSHOT_METADATA);
                    currentSaveName = snapshotMetadata.getUniqueName();
                    loadFromSnapshot(snapshotMetadata);
                } else if (intent.hasExtra(Snapshots.EXTRA_SNAPSHOT_NEW)) {
                    // Create a new snapshot named with a unique string
                    // TODO: check for existing snapshot, for now, add garbage text.
                    String unique = new BigInteger(281, new Random()).toString(13);
                    currentSaveName = "snapshotTemp-" + unique;
                    saveSnapshot(null);
                }
            }
        }
        // the example use of Snapshot.load() which displays a custom list of snapshots.
        else if (requestCode == RC_SELECT_SNAPSHOT) {
            Log.d(TAG, "Selected a snapshot!");
            if (resultCode == RESULT_OK) {
                if (intent != null && intent.hasExtra(SelectSnapshotActivity.SNAPSHOT_METADATA)) {
                    // Load a snapshot.
                    SnapshotMetadata snapshotMetadata =
                            intent.getParcelableExtra(SelectSnapshotActivity.SNAPSHOT_METADATA);
                    currentSaveName = snapshotMetadata.getUniqueName();
                    Log.d(TAG, "ok - loading " + currentSaveName);
                    loadFromSnapshot(snapshotMetadata);
                } else {
                    Log.w(TAG, "Expected snapshot metadata but found none.");
                }
            }
        }
        // loading a snapshot into the game.
        else if (requestCode == RC_LOAD_SNAPSHOT) {
            Log.d(TAG,"Loading a snapshot resultCode = " + resultCode);
            if (resultCode == RESULT_OK) {
                if (intent != null && intent.hasExtra(SelectSnapshotActivity.SNAPSHOT_METADATA)) {
                    // Load a snapshot.
                    String conflictId = intent.getStringExtra(SelectSnapshotActivity.CONFLICT_ID);
                    int retryCount = intent.getIntExtra(SelectSnapshotActivity.RETRY_COUNT,
                            MAX_SNAPSHOT_RESOLVE_RETRIES);
                    SnapshotMetadata snapshotMetadata =
                            intent.getParcelableExtra(SelectSnapshotActivity.SNAPSHOT_METADATA);
                    if (conflictId == null) {
                        loadFromSnapshot(snapshotMetadata);
                    } else {
                        Log.d(TAG,"resolving " + snapshotMetadata);
                        resolveSnapshotConflict(requestCode, conflictId, retryCount,
                                snapshotMetadata);
                    }
                }
            }

        }
        // saving the game into a snapshot.
        else if (requestCode == RC_SAVE_SNAPSHOT) {
            if (resultCode == RESULT_OK) {
                if (intent != null && intent.hasExtra(SelectSnapshotActivity.SNAPSHOT_METADATA)) {
                    // Load a snapshot.
                    String conflictId = intent.getStringExtra(SelectSnapshotActivity.CONFLICT_ID);
                    int retryCount = intent.getIntExtra(SelectSnapshotActivity.RETRY_COUNT,
                            MAX_SNAPSHOT_RESOLVE_RETRIES);
                    SnapshotMetadata snapshotMetadata =
                            intent.getParcelableExtra(SelectSnapshotActivity.SNAPSHOT_METADATA);
                    if (conflictId == null) {
                        saveSnapshot(snapshotMetadata);
                    } else {
                        Log.d(TAG,"resolving " + snapshotMetadata);
                        resolveSnapshotConflict(requestCode, conflictId, retryCount, snapshotMetadata);
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, intent);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        log("onCreate.");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create the Google Api Client with access to Plus and Games
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API).addScope(Plus.SCOPE_PLUS_LOGIN)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .addApi(Drive.API).addScope(Drive.SCOPE_APPFOLDER)
                .build();

        for (int id : LEVEL_BUTTON_IDS) {
            findViewById(id).setOnClickListener(this);
        }
        findViewById(R.id.button_next_world).setOnClickListener(this);
        findViewById(R.id.button_prev_world).setOnClickListener(this);
        findViewById(R.id.button_sign_in).setOnClickListener(this);
        findViewById(R.id.button_sign_out).setOnClickListener(this);
        ((RatingBar) findViewById(R.id.gameplay_rating)).setOnRatingBarChangeListener(this);
        mSaveGame = new SaveGame();
        updateUi();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Player wants to force save or load.
        // NOTE: this button exists in this sample for debug purposes and so that you can
        // see the effects immediately. A game probably shouldn't have a "Load/Save"
        // button (or at least not one that's so prominently displayed in the UI).
        if (item.getItemId() == R.id.menu_sync) {
            loadFromSnapshot(null);
            return true;
        }
        if (item.getItemId() == R.id.menu_save) {
            saveSnapshot(null);
            return true;
        }
        if (item.getItemId() == R.id.menu_select) {
            selectSnapshot();
        }
        return false;
    }


    @Override
    protected void onStart() {
        updateUi();
        super.onStart();
    }


    @Override
    protected void onStop() {
        if (mLoadingDialog != null) {
            mLoadingDialog.dismiss();
            mLoadingDialog = null;
        }
        super.onStop();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public void onConnected(Bundle connectionHint) {

        // Sign-in worked!
        log("Sign-in successful! Loading game state from cloud.");
        showSignOutBar();
        if (!mAlreadyLoadedState) {
            showSnapshots(getString(R.string.title_load_game), false, false);
        } else {
            updateUi();
        }
    }


    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "onConnectionSuspended() called. Cause: " + cause);
        // onConnected will automatically be called when the client reconnects.
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


    @Override
    public void onBackPressed() {
        if (mInLevel) {
            updateUi();
            findViewById(R.id.screen_gameplay).setVisibility(View.GONE);
            findViewById(R.id.screen_main).setVisibility(View.VISIBLE);
            mInLevel = false;
        } else {
            super.onBackPressed();
        }
    }

    /** Called when the "sign in" or "sign out" button is clicked. */
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
                mSignInClicked = false;
                Games.signOut(mGoogleApiClient);
                mGoogleApiClient.disconnect();
                showSignInBar();
                mSaveGame = new SaveGame();
                mAlreadyLoadedState = false;
                updateUi();
                break;
            case R.id.button_next_world:
                if (isConnected()) {
                    BaseGameUtils.makeSimpleDialog(this, getString(R.string.please_sign_in)).show();
                    return;
                }
                if (mWorld < WORLD_MAX) {
                    mWorld++;
                    updateUi();
                }
                break;
            case R.id.button_prev_world:
                if (isConnected()) {
                    BaseGameUtils.makeSimpleDialog(this, getString(R.string.please_sign_in)).show();
                    return;
                }
                if (mWorld > WORLD_MIN) {
                    mWorld--;
                    updateUi();
                }
                break;
            default:
                if (isConnected()) {
                    BaseGameUtils.makeSimpleDialog(this, getString(R.string.please_sign_in)).show();
                    return;
                }
                for (int i = 0; i < LEVEL_BUTTON_IDS.length; ++i) {
                    if (view.getId() == LEVEL_BUTTON_IDS[i]) {
                        launchLevel(i + 1);
                        return;
                    }
                }
        }
    }

    private boolean isConnected() {
        return mGoogleApiClient == null || !mGoogleApiClient.isConnected();
    }

    /**
     * Gets a screenshot to use with snapshots. Note that in practice you probably do not want to
     * use this approach because tablet screen sizes can become pretty large and because the image
     * will contain any UI and layout surrounding the area of interest.
     */
    Bitmap getScreenShot() {
        View root = findViewById(R.id.screen_main);
        Bitmap coverImage;
        try {
            root.setDrawingCacheEnabled(true);
            Bitmap base = root.getDrawingCache();
            coverImage = base.copy(base.getConfig(), false /* isMutable */);
        } catch (Exception ex) {
            Log.i(TAG, "Failed to create screenshot", ex);
            coverImage = null;
        } finally {
            root.setDrawingCacheEnabled(false);
        }
        return coverImage;
    }


    /** Shows the user's snapshots. */
    void showSnapshots(String title, boolean allowAdd, boolean allowDelete) {
        int maxNumberOfSavedGamesToShow = 5;
        Intent snapshotIntent = Games.Snapshots.getSelectSnapshotIntent(
                mGoogleApiClient, title, allowAdd, allowDelete, maxNumberOfSavedGamesToShow);
        startActivityForResult(snapshotIntent, RC_LIST_SAVED_GAMES);
    }

    /**
     * Loads a Snapshot from the user's synchronized storage.
     */
    void loadFromSnapshot(final SnapshotMetadata snapshotMetadata) {
        if (mLoadingDialog == null) {
            mLoadingDialog = new ProgressDialog(this);
            mLoadingDialog.setMessage(getString(R.string.loading_from_cloud));
        }

        mLoadingDialog.show();

        AsyncTask<Void, Void, Integer> task = new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                 Snapshots.OpenSnapshotResult result;
                if (snapshotMetadata != null && snapshotMetadata.getUniqueName() != null) {
                    Log.i(TAG, "Opening snapshot by metadata: " + snapshotMetadata);
                    result = Games.Snapshots.open(mGoogleApiClient,snapshotMetadata).await();
                } else {
                    Log.i(TAG, "Opening snapshot by name: " + currentSaveName);
                    result = Games.Snapshots.open(mGoogleApiClient, currentSaveName, true).await();
                }

                int status = result.getStatus().getStatusCode();

                Snapshot snapshot = null;
                if (status == GamesStatusCodes.STATUS_OK) {
                    snapshot = result.getSnapshot();
                } else if (status == GamesStatusCodes.STATUS_SNAPSHOT_CONFLICT) {

                    // if there is a conflict  - then resolve it.
                    snapshot = processSnapshotOpenResult(RC_LOAD_SNAPSHOT, result, 0);

                    // if it resolved OK, change the status to Ok
                    if (snapshot != null) {
                        status = GamesStatusCodes.STATUS_OK;
                    }
                    else {
                        Log.w(TAG,"Conflict was not resolved automatically");
                    }
                } else {
                    Log.e(TAG, "Error while loading: " + status);
                }

                if (snapshot != null) {
                    try {
                        readSavedGame(snapshot);
                    } catch (IOException e) {
                        Log.e(TAG, "Error while reading snapshot contents: " + e.getMessage());
                    }
                }
                return status;
            }

            @Override
            protected void onPostExecute(Integer status) {
                Log.i(TAG, "Snapshot loaded: " + status);

                // Note that showing a toast is done here for debugging. Your application should
                // resolve the error appropriately to your app.
                if (status == GamesStatusCodes.STATUS_SNAPSHOT_NOT_FOUND) {
                    Log.i(TAG, "Error: Snapshot not found");
                    Toast.makeText(getBaseContext(), "Error: Snapshot not found",
                            Toast.LENGTH_SHORT).show();
                } else if (status == GamesStatusCodes.STATUS_SNAPSHOT_CONTENTS_UNAVAILABLE) {
                    Log.i(TAG, "Error: Snapshot contents unavailable");
                    Toast.makeText(getBaseContext(), "Error: Snapshot contents unavailable",
                            Toast.LENGTH_SHORT).show();
                } else if (status == GamesStatusCodes.STATUS_SNAPSHOT_FOLDER_UNAVAILABLE) {
                    Log.i(TAG, "Error: Snapshot folder unavailable");
                    Toast.makeText(getBaseContext(), "Error: Snapshot folder unavailable.",
                            Toast.LENGTH_SHORT).show();
                }

                if (mLoadingDialog != null && mLoadingDialog.isShowing()) {
                    mLoadingDialog.dismiss();
                    mLoadingDialog = null;
                }
                hideAlertBar();
                updateUi();
            }
        };

        task.execute();
    }

    private void readSavedGame(Snapshot snapshot) throws IOException {
        mSaveGame = new SaveGame(snapshot.getSnapshotContents().readFully());
        mAlreadyLoadedState = true;
    }

    /**
     * Conflict resolution for when Snapshots are opened.
     *
     * @param requestCode - the request currently being processed.  This is used to forward on the
     *                    information to another activity, or to send the result intent.
     * @param result The open snapshot result to resolve on open.
     * @param retryCount - the current iteration of the retry.  The first retry should be 0.
     * @return The opened Snapshot on success; otherwise, returns null.
     */
    Snapshot processSnapshotOpenResult(int requestCode, Snapshots.OpenSnapshotResult result,
            int retryCount) {

        retryCount++;
        int status = result.getStatus().getStatusCode();

        Log.i(TAG, "Save Result status: " + status);

        if (status == GamesStatusCodes.STATUS_OK) {
            return result.getSnapshot();
        } else if (status == GamesStatusCodes.STATUS_SNAPSHOT_CONTENTS_UNAVAILABLE) {
            return result.getSnapshot();
        } else if (status == GamesStatusCodes.STATUS_SNAPSHOT_CONFLICT) {
            final Snapshot snapshot = result.getSnapshot();
            final Snapshot conflictSnapshot = result.getConflictingSnapshot();

            ArrayList<Snapshot> snapshotList = new ArrayList<Snapshot>(2);
            snapshotList.add(snapshot);
            snapshotList.add(conflictSnapshot);

            selectSnapshotItem(requestCode, snapshotList, result.getConflictId(), retryCount);
            // display both to the user and allow them to select on
        }
        // Fail, return null.
        return null;
    }

    /**
     *  Handles resolving the snapshot conflict asynchronously.
     *
     * @param requestCode - the request currently being processed.  This is used to forward on the
     *                    information to another activity, or to send the result intent.
     * @param conflictId - the id of the conflict being resolved.
     * @param retryCount - the current iteration of the retry.  The first retry should be 0.
     * @param snapshotMetadata - the metadata of the snapshot that is selected to resolve the conflict.
     */
    private void resolveSnapshotConflict(final int requestCode, final String conflictId,
            final int retryCount,
            final SnapshotMetadata snapshotMetadata) {

        Log.i(TAG,"Resolving conflict retry count = " + retryCount + " conflictid = " + conflictId);
        AsyncTask<Void, Void, Snapshots.OpenSnapshotResult> task =
                new AsyncTask<Void, Void, Snapshots.OpenSnapshotResult>() {
                    @Override
                    protected Snapshots.OpenSnapshotResult doInBackground(Void... voids) {

                        Snapshots.OpenSnapshotResult result;
                        if (snapshotMetadata.getUniqueName() != null) {
                            Log.d(TAG,"Opening unique name " + snapshotMetadata.getUniqueName());
                            result = Games.Snapshots.open(mGoogleApiClient, snapshotMetadata)
                                    .await();
                        }
                        else {
                            Log.d(TAG,"Opening current save name " + currentSaveName);
                            result = Games.Snapshots.open(mGoogleApiClient, currentSaveName, true)
                                    .await();
                        }

                        Log.d(TAG,"opening from metadata - result is " + result.getStatus() +
                                " snapshot is " + result.getSnapshot());

                       return Games.Snapshots
                                .resolveConflict(mGoogleApiClient, conflictId, result.getSnapshot())
                                .await();
                    }

                    @Override
                    protected void onPostExecute(Snapshots.OpenSnapshotResult openSnapshotResult) {
                        Snapshot snapshot = processSnapshotOpenResult(requestCode,
                                openSnapshotResult,
                                retryCount);
                        Log.d(TAG,"resolved snapshot conflict - snapshot is " + snapshot);
                        // if there is a snapshot returned, then pass it along to onActivityResult.
                        // otherwise, another activity will be used to resolve the conflict so we
                        // don't need to do anything here.
                        if (snapshot != null) {
                            Intent intent = new Intent("");
                            intent.putExtra(SelectSnapshotActivity.SNAPSHOT_METADATA,
                                    snapshot.getMetadata().freeze());
                            onActivityResult(requestCode, RESULT_OK, intent);
                        }
                    }
                };

        task.execute();
    }


    /**
     * Prepares saving Snapshot to the user's synchronized storage, conditionally resolves errors,
     * and stores the Snapshot.
     */
    void saveSnapshot(final SnapshotMetadata snapshotMetadata) {
        AsyncTask<Void, Void, Snapshots.OpenSnapshotResult> task =
                new AsyncTask<Void, Void, Snapshots.OpenSnapshotResult>() {
                    @Override
                    protected Snapshots.OpenSnapshotResult doInBackground(Void... params) {
                        if (snapshotMetadata == null) {
                            Log.i(TAG, "Calling open with " + currentSaveName);
                            return Games.Snapshots.open(mGoogleApiClient, currentSaveName, true)
                                    .await();
                        }
                        else {
                            Log.i(TAG, "Calling open with " + snapshotMetadata);
                            return Games.Snapshots.open(mGoogleApiClient, snapshotMetadata)
                                    .await();
                        }
                    }

                    @Override
                    protected void onPostExecute(Snapshots.OpenSnapshotResult result) {
                        Snapshot toWrite = processSnapshotOpenResult(RC_SAVE_SNAPSHOT, result, 0);
                        if (toWrite != null) {
                            Log.i(TAG, writeSnapshot(toWrite));
                        }
                        else {
                            Log.e(TAG, "Error opening snapshot: " + result.toString());
                        }
                    }
                };

        task.execute();
    }

    /**
     * Generates metadata, takes a screenshot, and performs the write operation for saving a
     * snapshot.
     */
    private String writeSnapshot(Snapshot snapshot) {
        // Set the data payload for the snapshot.
        snapshot.getSnapshotContents().writeBytes(mSaveGame.toBytes());

        // Save the snapshot.
        SnapshotMetadataChange metadataChange = new SnapshotMetadataChange.Builder()
                .setCoverImage(getScreenShot())
                .setDescription("Modified data at: " + Calendar.getInstance().getTime())
                .build();
        Games.Snapshots.commitAndClose(mGoogleApiClient, snapshot, metadataChange);
        return snapshot.toString();
    }


    /** Shows the "sign in" bar (explanation and button). */
    private void showSignInBar() {
        findViewById(R.id.sign_in_bar).setVisibility(View.VISIBLE);
        findViewById(R.id.sign_out_bar).setVisibility(View.GONE);
    }

    /** Shows the "sign out" bar (explanation and button). */
    private void showSignOutBar() {
        findViewById(R.id.sign_in_bar).setVisibility(View.GONE);
        findViewById(R.id.sign_out_bar).setVisibility(View.VISIBLE);
    }


    /** Updates the game UI. */
    private void updateUi() {
        ((TextView) findViewById(R.id.world_display)).setText(getString(R.string.world)
                + " " + mWorld);
        for (int i = 0; i < LEVELS_PER_WORLD; i++) {
            int levelNo = i + 1; // levels are numbered from 1
            Button b = (Button) findViewById(LEVEL_BUTTON_IDS[i]);
            int stars = mSaveGame.getLevelStars(mWorld, levelNo);
            b.setTextColor(getResources().getColor(stars > 0 ? R.color.ClearedLevelColor :
                    R.color.UnclearedLevelColor));
            b.setText(String.valueOf(mWorld) + "-" + String.valueOf(levelNo) + "\n" +
                    STAR_STRINGS[stars]);
        }
        // disable world changing if we are at the end of the list.
        Button button;

        button = (Button) findViewById(R.id.button_next_world);
        button.setEnabled(mWorld < WORLD_MAX);

        button = (Button) findViewById(R.id.button_prev_world);
        button.setEnabled(mWorld > WORLD_MIN);
    }


    /**
     * Loads the specified level state.
     *
     * @param level - level to load.
     */
    private void launchLevel(int level) {
        mLevel = level;
        ((TextView) findViewById(R.id.gameplay_level_display)).setText(
                getString(R.string.level) + " " + mWorld + "-" + mLevel);
        ((RatingBar) findViewById(R.id.gameplay_rating)).setRating(
                mSaveGame.getLevelStars(mWorld, mLevel));
        findViewById(R.id.screen_gameplay).setVisibility(View.VISIBLE);
        findViewById(R.id.screen_main).setVisibility(View.GONE);
        mInLevel = true;
    }


    @Override
    public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
        mSaveGame.setLevelStars(mWorld, mLevel, (int) rating);
        updateUi();
        findViewById(R.id.screen_gameplay).setVisibility(View.GONE);
        findViewById(R.id.screen_main).setVisibility(View.VISIBLE);

        mInLevel = false;
        // save new data to cloud
        saveSnapshot(null);
    }

    /** Prints a log message (convenience method). */
    void log(String message) {
        Log.d(TAG, message);
    }


    /** Shows an alert message. */
    private void showAlertBar(int resId) {
        ((TextView) findViewById(R.id.alert_bar)).setText(getString(resId));
        findViewById(R.id.alert_bar).setVisibility(View.VISIBLE);
    }


    /** Dismisses the previously displayed alert message. */
    private void hideAlertBar() {
        View alertBar = findViewById(R.id.alert_bar);
        if (alertBar != null && alertBar.getVisibility() != View.GONE) {
            alertBar.setVisibility(View.GONE);
        }
    }


    /**
     * This is an example of how to call Games.Snapshots.load(). It displays another
     * activity to allow the user to select the snapshot.  It is recommended to use the
     * standard selection intent, Games.Snapshots.getSelectSnapshotIntent().
     */
    private void selectSnapshot() {
        if (mLoadingDialog == null) {
            mLoadingDialog = new ProgressDialog(this);
            mLoadingDialog.setMessage(getString(R.string.loading_from_cloud));
        }
        mLoadingDialog.show();

        //Start an asynchronous task to read this snapshot and load it.
        AsyncTask<Void, Void, Snapshots.LoadSnapshotsResult> task =
                new AsyncTask<Void, Void, Snapshots.LoadSnapshotsResult>() {
                    @Override
                    protected Snapshots.LoadSnapshotsResult doInBackground(Void... params) {

                        Log.i(TAG, "Listing snapshots");
                        return Games.Snapshots.load(mGoogleApiClient, false).await();
                    }

                    @Override
                    protected void onPostExecute(Snapshots.LoadSnapshotsResult snapshotResults) {

                        if (mLoadingDialog != null && mLoadingDialog.isShowing()) {
                            mLoadingDialog.dismiss();
                            mLoadingDialog = null;
                        }
                        int status = snapshotResults.getStatus().getStatusCode();

                        // Note that showing a toast is done here for debugging. Your application should
                        // resolve the error appropriately to your app.
                        if (status == GamesStatusCodes.STATUS_SNAPSHOT_NOT_FOUND) {
                            Log.i(TAG, "Error: Snapshot not found");
                            Toast.makeText(getBaseContext(), "Error: Snapshot not found",
                                    Toast.LENGTH_SHORT).show();
                        } else if (status
                                == GamesStatusCodes.STATUS_SNAPSHOT_CONTENTS_UNAVAILABLE) {
                            Log.i(TAG, "Error: Snapshot contents unavailable");
                            Toast.makeText(getBaseContext(), "Error: Snapshot contents unavailable",
                                    Toast.LENGTH_SHORT).show();
                        } else if (status == GamesStatusCodes.STATUS_SNAPSHOT_FOLDER_UNAVAILABLE) {
                            Log.i(TAG, "Error: Snapshot folder unavailable");
                            Toast.makeText(getBaseContext(), "Error: Snapshot folder unavailable.",
                                    Toast.LENGTH_SHORT).show();
                        }

                        ArrayList<SnapshotMetadata> items = new ArrayList<SnapshotMetadata>();
                        for (SnapshotMetadata m : snapshotResults.getSnapshots()) {
                            items.add(m);
                        }
                        selectSnapshotItem(RC_SELECT_SNAPSHOT, items);

                    }
                };

        task.execute();
    }

    private void selectSnapshotItem(int requestCode, ArrayList<Snapshot> items,
            String conflictId, int retryCount) {

        ArrayList<SnapshotMetadata> snapshotList = new ArrayList<SnapshotMetadata>(items.size());
        for (Snapshot m : items) {
            snapshotList.add(m.getMetadata().freeze());
        }
        Intent intent = new Intent(this, SelectSnapshotActivity.class);
        intent.putParcelableArrayListExtra(SelectSnapshotActivity.SNAPSHOT_METADATA_LIST,
                snapshotList);

        intent.putExtra(SelectSnapshotActivity.CONFLICT_ID, conflictId);
        intent.putExtra(SelectSnapshotActivity.RETRY_COUNT, retryCount);

        Log.d(TAG, "Starting activity to select snapshot");
        startActivityForResult(intent, requestCode);
    }

    private void selectSnapshotItem(int requestCode, ArrayList<SnapshotMetadata> items) {

        ArrayList<SnapshotMetadata> metadataArrayList =
                new ArrayList<SnapshotMetadata>(items.size());
        for (SnapshotMetadata m : items) {
            metadataArrayList.add(m.freeze());
        }
        Intent intent = new Intent(this, SelectSnapshotActivity.class);
        intent.putParcelableArrayListExtra(SelectSnapshotActivity.SNAPSHOT_METADATA_LIST,
                metadataArrayList);

        startActivityForResult(intent, requestCode);
    }
}
