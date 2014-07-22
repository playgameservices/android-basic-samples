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

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.snapshot.Snapshot;
import com.google.android.gms.games.snapshot.SnapshotMetadata;
import com.google.android.gms.games.snapshot.SnapshotMetadataBuffer;
import com.google.android.gms.games.snapshot.SnapshotMetadataChange;
import com.google.android.gms.games.snapshot.Snapshots;
import com.google.example.games.basegameutils.BaseGameActivity;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

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
public class MainActivity extends BaseGameActivity
            implements View.OnClickListener, OnRatingBarChangeListener {
    private static final boolean ENABLE_DEBUG = true;
    private static final String TAG = "CollectAllTheStars2";

    // the state key under which we store our data. Since our data is small and
    // this is a simple example, we only use this one state key, but your app can
    // use multiple keys! Be sure not to exceed the maximum number of keys
    // given by AppStateClient.getMaxNumKeys().
    private static final int OUR_STATE_KEY = 0;

    // current save game
    SaveGame mSaveGame = new SaveGame();

    private String currentSaveName = "snapshotTemp";

    // world we're currently viewing
    int mWorld = 1;
    final int WORLD_MIN = 1, WORLD_MAX = 20;
    final int LEVELS_PER_WORLD = 12;

    // level we're currently "playing"
    int mLevel = 0;

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
    private CountDownLatch mConflictLatch = null;
    final static int MAX_SNAPSHOT_RESOLVE_RETRIES = 3;

    public MainActivity() {
        // request that superclass initialize and manage the AppStateClient for us
        super(BaseGameActivity.CLIENT_ALL);
    }


    /**
     * You can capture the Snapshot selection intent in the onActivityResult method. The result
     * either indicates a new Snapshot was created (EXTRA_SNAPSHOT_NEW) or was selected.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (intent != null) {
            if (intent.hasExtra(Snapshots.EXTRA_SNAPSHOT_METADATA)) {
                // Load a snapshot.
                SnapshotMetadata snapshotMetadata = (SnapshotMetadata)
                        intent.getParcelableExtra(Snapshots.EXTRA_SNAPSHOT_METADATA);
                currentSaveName = snapshotMetadata.getUniqueName();
                loadFromSnapshot();
            } else if (intent.hasExtra(Snapshots.EXTRA_SNAPSHOT_NEW)) {
                // Create a new snapshot named with a unique string
                // TODO: check for existing snapshot, for now, add garbage text.
                String unique = new BigInteger(281, new Random()).toString(13);
                currentSaveName = "snapshotTemp-" + unique;
                saveSnapshot();
            }
        }
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

    /**
     * Shows the user's snapshots.
     */
    void showSnapshots(){
        android.content.Intent snapshotIntent = Games.Snapshots.getSelectSnapshotIntent(
                getApiClient(), "Select a snap", true, true, 5);
        startActivityForResult(snapshotIntent, 0);
    }

    /** Lists a user's snapshots. */
    void listSnapshots(){
        if (mLoadingDialog != null) {
            mLoadingDialog.show();
        }

        AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                Log.i(TAG, "Opening snapshot " + currentSaveName);
                Snapshots.LoadSnapshotsResult lresult = Games.Snapshots.load(getApiClient(), true)
                        .await();

                String resultMessage = "";

                int status = lresult.getStatus().getStatusCode();

                if (status == GamesStatusCodes.STATUS_OK){
                    SnapshotMetadataBuffer smdb = lresult.getSnapshots();
                    Iterator<SnapshotMetadata> snapsIt = smdb.iterator();

                    while(snapsIt.hasNext()){
                        SnapshotMetadata thisSnap = snapsIt.next();
                        resultMessage += thisSnap.getDescription();
                    }
                    smdb.close();
                }else {
                    // Fail and notify the client.
                    Log.e(TAG,"failure while listing snapshots: " + status);
                    resultMessage = "Error: " + status;
                }
                return resultMessage;
            }

            @Override
            protected void onPostExecute(String message){
                Log.i(TAG, "Snapshots loaded.");
                Log.i(TAG, "Details: " + message);
                mLoadingDialog.dismiss();
                hideAlertBar();
            }
        };

        task.execute();
    }

    /**
     * Loads a Snapshot from the user's synchronized storage.
     */
    void loadFromSnapshot() {
        if (mLoadingDialog != null) {
            mLoadingDialog.show();
        }

        AsyncTask<Void, Void, Integer> task = new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                Log.i(TAG, "Opening snapshot " + currentSaveName);
                Snapshots.OpenSnapshotResult result = Games.Snapshots.open(getApiClient(),
                        currentSaveName, true).await();

                int status = result.getStatus().getStatusCode();

                if (status == GamesStatusCodes.STATUS_OK) {
                    Snapshot snapshot = result.getSnapshot();
                    mSaveGame = new SaveGame(snapshot.readFully());
                }else{
                    Log.e(TAG, "Error while loading: " + status);
                }

                return status;
            }

            @Override
            protected void onPostExecute(Integer status){
                Log.i(TAG, "Snapshot loaded: " + status);

                // Note that showing a toast is done here for debugging. Your application should
                // resolve the error appropriately to your app.
                if (status == GamesStatusCodes.STATUS_SNAPSHOT_NOT_FOUND){
                    Log.i(TAG,"Error: Snapshot not found");
                    Toast.makeText(getBaseContext(), "Error: Snapshot not found",
                            Toast.LENGTH_SHORT);
                }else if (status == GamesStatusCodes.STATUS_SNAPSHOT_CONTENTS_UNAVAILABLE) {
                    Log.i(TAG, "Error: Snapshot contents unavailable");
                    Toast.makeText(getBaseContext(), "Error: Snapshot contents unavailable",
                            Toast.LENGTH_SHORT);
                }else if (status == GamesStatusCodes.STATUS_SNAPSHOT_FOLDER_UNAVAILABLE){
                    Log.i(TAG, "Error: Snapshot folder unavailable");
                    Toast.makeText(getBaseContext(), "Error: Snapshot folder unavailable.",
                            Toast.LENGTH_SHORT);
                }

                // Reflect the changes in the UI.
                updateUi();

                mLoadingDialog.dismiss();
                hideAlertBar();
            }
        };

        task.execute();
    }

    /**
     * Conflict resolution for when Snapshots are opened.
     * @param result The open snapshot result to resolve on open.
     * @return The opened Snapshot on success; otherwise, returns null.
     */
    Snapshot processSnapshotOpenResult(Snapshots.OpenSnapshotResult result, int retryCount){
        Snapshot mResolvedSnapshot = null;
        retryCount++;
        int status = result.getStatus().getStatusCode();

        Log.i(TAG, "Save Result status: " + status);

        if (status == GamesStatusCodes.STATUS_OK) {
            return result.getSnapshot();
        } else if (status == GamesStatusCodes.STATUS_SNAPSHOT_CONTENTS_UNAVAILABLE) {
            return result.getSnapshot();
        } else if (status == GamesStatusCodes.STATUS_SNAPSHOT_CONFLICT){
            Snapshot snapshot = result.getSnapshot();
            Snapshot conflictSnapshot = result.getConflictingSnapshot();

            // Resolve between conflicts by selecting the newest of the conflicting snapshots.
            mResolvedSnapshot = snapshot;

            if (snapshot.getMetadata().getLastModifiedTimestamp() <
                    conflictSnapshot.getMetadata().getLastModifiedTimestamp()){
                mResolvedSnapshot = conflictSnapshot;
            }

            Snapshots.OpenSnapshotResult resolveResult = Games.Snapshots.resolveConflict(
                    getApiClient(), result.getConflictId(), mResolvedSnapshot)
                    .await();

            if (retryCount < MAX_SNAPSHOT_RESOLVE_RETRIES){
                return processSnapshotOpenResult(resolveResult, retryCount);
            }else{
                String message = "Could not resolve snapshot conflicts";
                Log.e(TAG, message);
                Toast.makeText(getBaseContext(), message, Toast.LENGTH_LONG);
            }

        }
        // Fail, return null.
        return null;
    }


    /**
     * Prepares saving Snapshot to the user's synchronized storage, conditionally resolves errors,
     * and stores the Snapshot.
     */
    void saveSnapshot() {
        AsyncTask<Void, Void, Snapshots.OpenSnapshotResult> task =
                new AsyncTask<Void, Void, Snapshots.OpenSnapshotResult>() {
                    @Override
                    protected Snapshots.OpenSnapshotResult doInBackground(Void... params) {
                        Snapshots.OpenSnapshotResult result = Games.Snapshots.open(getApiClient(),
                                currentSaveName, true).await();
                        return result;
                    }

                    @Override
                    protected void onPostExecute(Snapshots.OpenSnapshotResult result) {
                        Snapshot toWrite = processSnapshotOpenResult(result, 0);

                        Log.i(TAG, writeSnapshot(toWrite));
                    }
                };

        task.execute();
    }

    /**
     * Generates metadata, takes a screenshot, and performs the write operation for saving a
     * snapshot.
     */
    private String writeSnapshot(Snapshot snapshot){
        // Set the data payload for the snapshot.
        snapshot.writeBytes(mSaveGame.toBytes());

        // Save the snapshot.
        SnapshotMetadataChange metadataChange = new SnapshotMetadataChange.Builder()
                .setCoverImage(getScreenShot())
                .setDescription("Modified data at: " + Calendar.getInstance().getTime())
                .build();
        Games.Snapshots.commitAndClose(getApiClient(), snapshot, metadataChange);
        return snapshot.toString();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        enableDebugLog(ENABLE_DEBUG, TAG);
        log("onCreate.");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        loadLocal();

        for (int id : LEVEL_BUTTON_IDS) {
            findViewById(id).setOnClickListener(this);
        }
        findViewById(R.id.button_next_world).setOnClickListener(this);
        findViewById(R.id.button_prev_world).setOnClickListener(this);
        findViewById(R.id.button_sign_in).setOnClickListener(this);
        findViewById(R.id.button_sign_out).setOnClickListener(this);
        ((RatingBar) findViewById(R.id.gameplay_rating)).setOnRatingBarChangeListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Player wants to force save or load.
        // NOTE: this button exists in this sample for debug purposes and so that you can
        // see the effects immediately. A game probably shouldn't have a "Load/Save"
        // button (or at least not one that's so prominently displayed in the UI).
        if (item.getItemId() == R.id.menu_sync) {
            loadFromSnapshot();
            return true;
        }
        if (item.getItemId() == R.id.menu_save) {
            saveSnapshot();
            return true;
        }
        if (item.getItemId() == R.id.menu_select)
        {
            showSnapshots();
        }
        return false;
    }

    @Override
    protected void onStart() {
        mLoadingDialog = new ProgressDialog(this);
        mLoadingDialog.setMessage(getString(R.string.loading_from_cloud));
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


    private void loadLocal() {
        SharedPreferences sp = getSharedPreferences("gamestate", Context.MODE_PRIVATE);
        mSaveGame = new SaveGame(sp, "gamestate");
    }


    private void saveLocal() {
        SharedPreferences sp = getSharedPreferences("gamestate", Context.MODE_PRIVATE);
        mSaveGame.save(sp, "gamestate");
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

    /** Called when the "sign in" or "sign out" button is clicked. */
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
        case R.id.button_sign_in:
            // Check to see the developer who's running this sample code read the instructions :-)
            // NOTE: this check is here only because this is a sample! Don't include this
            // check in your actual production app.
            if (!verifyPlaceholderIdsReplaced()) {
                complain("Sample not correctly set up. See README!");
                break;
            }

            // start the sign-in flow
            beginUserInitiatedSignIn();
            break;
        case R.id.button_sign_out:
            // sign out.
            signOut();
            showSignInBar();
            mSaveGame = new SaveGame();
            updateUi();
            break;
        case R.id.button_next_world:
            if (mWorld < WORLD_MAX) {
                mWorld++;
                updateUi();
            }
            break;
        case R.id.button_prev_world:
            if (mWorld > WORLD_MIN) {
                mWorld--;
                updateUi();
            }
            break;
        default:
            for (int i = 0; i < LEVEL_BUTTON_IDS.length; ++i) {
                if (view.getId() == LEVEL_BUTTON_IDS[i]) {
                    launchLevel(i + 1);
                    return;
                }
            }
        }
    }


    /** Prints a log message (convenience method). */
    void log(String message) {
        Log.d(TAG, message);
    }


    /** Complains to the user about an error. */
    void complain(String error) {
        showAlert("Error: " + error);
        Log.e(TAG, "*** Error: " + error);
    }


    /**
     * Called to notify us that sign in failed. Notice that a failure in sign in is not
     * necessarily due to an error; it might be that the user never signed in, so our
     * attempt to automatically sign in fails because the user has not gone through
     * the authorization flow. So our reaction to sign in failure is to show the sign in
     * button. When the user clicks that button, the sign in process will start/resume.
     */
    @Override
    public void onSignInFailed() {
        // Sign-in has failed. So show the user the sign-in button
        // so they can click the "Sign-in" button.
        log("Sign-in failed. Showing sign-in button.");
        showSignInBar();
    }


    /**
     * Called to notify us that sign in succeeded. We react by loading the loot from the
     * cloud and updating the UI to show a sign-out button.
     */
    @Override
    public void onSignInSucceeded() {
        // Sign-in worked!
        log("Sign-in successful! Loading game state from cloud.");
        showSignOutBar();
        showAccessToken();
        if (!mAlreadyLoadedState) {
            loadFromSnapshot();
        }
        listSnapshots();
    }

    public void showAccessToken(){
        AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String token = null;

                try {
                    token = GoogleAuthUtil.getToken(
                            MainActivity.this,
                            "class@google.com",
                            "oauth2:" + Scopes.DRIVE_APPFOLDER);
                } catch (IOException transientEx) {
                    // Network or server error, try later
                    Log.e(TAG, transientEx.toString());
                } catch (UserRecoverableAuthException e) {
                    // Recover (with e.getIntent())
                    Log.e(TAG, e.toString());
                    Intent recover = e.getIntent();
                    //startActivityForResult(recover, REQUEST_CODE_TOKEN_AUTH);
                } catch (GoogleAuthException authEx) {
                    // The call is not ever expected to succeed
                    // assuming you have already verified that
                    // Google Play services is installed.
                    Log.e(TAG, authEx.toString());
                }

                return token;
            }

            @Override
            protected void onPostExecute(String token) {
                Log.i(TAG, "Access token retrieved:" + token);
            }
        };

        task.execute();
    }



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
    }

    private void launchLevel(int level) {
        mLevel = level;
        ((TextView) findViewById(R.id.gameplay_level_display)).setText(
                getString(R.string.level) + " " + mWorld + "-" + mLevel);
        ((RatingBar) findViewById(R.id.gameplay_rating)).setRating(
                mSaveGame.getLevelStars(mWorld, mLevel));
        findViewById(R.id.screen_gameplay).setVisibility(View.VISIBLE);
        findViewById(R.id.screen_main).setVisibility(View.GONE);
    }

    @Override
    public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
        mSaveGame.setLevelStars(mWorld, mLevel, (int)rating);
        updateUi();
        saveLocal();
        findViewById(R.id.screen_gameplay).setVisibility(View.GONE);
        findViewById(R.id.screen_main).setVisibility(View.VISIBLE);

        // save new data to cloud
        saveSnapshot();
    }

    private void showAlertBar(int resId) {
        ((TextView) findViewById(R.id.alert_bar)).setText(getString(resId));
        ((TextView) findViewById(R.id.alert_bar)).setVisibility(View.VISIBLE);
    }

    private void hideAlertBar() {
        ((TextView) findViewById(R.id.alert_bar)).setVisibility(View.GONE);
    }


    /**
     * Checks that the developer (that's you!) read the instructions.
     *
     * IMPORTANT: a method like this SHOULD NOT EXIST in your production app!
     * It merely exists here to check that anyone running THIS PARTICULAR SAMPLE
     * did what they were supposed to in order for the sample to work.
     */
    boolean verifyPlaceholderIdsReplaced() {
        final boolean CHECK_PKGNAME = true;  // set to false to disable check
                                             // (not recommended!)

        // Did the developer forget to change the package name?
        if (CHECK_PKGNAME && getPackageName().startsWith("com.google.example.")) {
            Log.e(TAG, "*** Sample setup problem: " +
                    "package name cannot be com.google.example.*. Use your own " +
                    "package name.");
            return false;
        }

        // Did the developer forget to replace a placeholder ID?
        int res_ids[] = new int[] {
                R.string.app_id
        };
        for (int i : res_ids) {
            if (getString(i).equalsIgnoreCase("ReplaceMe")) {
                Log.e(TAG, "*** Sample setup problem: You must replace all " +
                        "placeholder IDs in the ids.xml file by your project's IDs.");
                return false;
            }
        }
        return true;
    }
}
