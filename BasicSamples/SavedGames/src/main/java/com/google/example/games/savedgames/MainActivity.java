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

package com.google.example.games.savedgames;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.appstate.AppStateManager;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.snapshot.Snapshot;
import com.google.android.gms.games.snapshot.SnapshotMetadata;
import com.google.android.gms.games.snapshot.SnapshotMetadataChange;
import com.google.android.gms.games.snapshot.Snapshots;
import com.google.example.games.basegameutils.BaseGameUtils;

import java.io.IOException;

/**
 * SavedGames.  A sample that demonstrates how to migrate from the Cloud Save (AppState) API to the
 * newer Saved Games (Snapshots) API.  The app allows load/update to both services as well as an
 * example of migrating data from AppState to Snapshots.
 *
 * @author Sam Stern (samstern@google.com)
 */
public class MainActivity extends Activity implements View.OnClickListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public static final String TAG = "SavedGames";

    // The AppState slot we are editing.  For simplicity this sample only manipulates a single
    // Cloud Save slot and a corresponding Snapshot entry,  This could be changed to any integer
    // 0-3 without changing functionality (Cloud Save has four slots, numbered 0-3).
    private static final int APP_STATE_KEY = 0;

    // Request code used to invoke sign-in UI.
    private static final int RC_SIGN_IN = 9001;

    // Request code used to invoke Snapshot selection UI.
    private static final int RC_SELECT_SNAPSHOT = 9002;

    /// Client used to interact with Google APIs.
    private GoogleApiClient mGoogleApiClient;

    // Progress Dialog used to display loading messages.
    private ProgressDialog mProgressDialog;

    // True when the application is attempting to resolve a sign-in error that has a possible
    // resolution,
    private boolean mIsResolving = false;

    // True immediately after the user clicks the sign-in button/
    private boolean mSignInClicked = false;

    // True if we want to automatically attempt to sign in the user at application start.
    private boolean mAutoStartSignIn = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Build API client with access to Games, AppState, and SavedGames.
        // It is very important to add Drive or the SavedGames API will not work
        // Make sure to also go to console.developers.google.com and enable the Drive API for your
        // project
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES) // Games
                .addApi(AppStateManager.API).addScope(AppStateManager.SCOPE_APP_STATE) // AppState
                .addScope(Drive.SCOPE_APPFOLDER) // SavedGames
                .build();

        // Set up button listeners
        findViewById(R.id.button_sign_in).setOnClickListener(this);

        findViewById(R.id.button_cloud_save_load).setOnClickListener(this);
        findViewById(R.id.button_cloud_save_update).setOnClickListener(this);
        findViewById(R.id.button_cloud_save_migrate).setOnClickListener(this);

        findViewById(R.id.button_saved_games_load).setOnClickListener(this);
        findViewById(R.id.button_saved_games_update).setOnClickListener(this);
        findViewById(R.id.button_saved_games_select).setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
        updateUI();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        dismissProgressDialog();

        if (requestCode == RC_SIGN_IN) {
            Log.d(TAG, "onActivityResult: RC_SIGN_IN, resultCode = " + resultCode);
            mSignInClicked = false;
            mIsResolving = false;

            if (resultCode == RESULT_OK) {
                // Sign-in was successful, connect the API Client
                Log.d(TAG, "onActivityResult: RC_SIGN_IN (OK)");
                mGoogleApiClient.connect();
            } else {
                // There was an error during sign-in, display a Dialog with the appropriate message
                // to the user.
                Log.d(TAG, "onActivityResult: RC_SIGN_IN (Error)");
                BaseGameUtils.showActivityResultError(this, requestCode, resultCode, R.string.signin_other_error);
            }
        } else if (requestCode == RC_SELECT_SNAPSHOT) {
            Log.d(TAG, "onActivityResult: RC_SELECT_SNAPSHOT, resultCode = " + resultCode);
            if (resultCode == RESULT_OK) {
                // Successfully returned from Snapshot selection UI
                if (data != null) {
                    Bundle bundle = data.getExtras();
                    SnapshotMetadata selected = Games.Snapshots.getSnapshotFromBundle(bundle);
                    if (selected == null) {
                        // No snapshot in the Intent bundle, display error message
                        displayMessage(getString(R.string.saved_games_select_failure), true);
                        setData(null);
                        displaySnapshotMetadata(null);
                    } else {
                        // Found Snapshot Metadata in Intent bundle.  Load Snapshot by name.
                        String snapshotName = selected.getUniqueName();
                        savedGamesLoad(snapshotName);
                    }
                }
            } else {
                // User canceled the select intent or it failed for some other reason
                displayMessage(getString(R.string.saved_games_select_cancel), true);
                setData(null);
                displaySnapshotMetadata(null);
            }
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "onConnected");
        updateUI();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended: " + i);
        mGoogleApiClient.connect();
        updateUI();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed");
        if (mIsResolving) {
            // The application is attempting to resolve this connection failure already.
            Log.d(TAG, "onConnectionFailed: already resolving");
            return;
        }

        if (mSignInClicked || mAutoStartSignIn) {
            mSignInClicked = false;
            mAutoStartSignIn = false;

            // Attempt to resolve the connection failure.
            Log.d(TAG, "onConnectionFailed: begin resolution.");
            mIsResolving = BaseGameUtils.resolveConnectionFailure(this, mGoogleApiClient,
                    connectionResult, RC_SIGN_IN, getString(R.string.signin_other_error));
        }

        updateUI();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_sign_in:
                beginUserInitiatedSignIn();
                break;
            case R.id.button_cloud_save_load:
                cloudSaveLoad();
                break;
            case R.id.button_cloud_save_update:
                cloudSaveUpdate();
                break;
            case R.id.button_cloud_save_migrate:
                cloudSaveMigrate();
                break;
            case R.id.button_saved_games_load:
                savedGamesLoad(makeSnapshotName(APP_STATE_KEY));
                break;
            case R.id.button_saved_games_update:
                savedGamesUpdate();
                break;
            case R.id.button_saved_games_select:
                savedGamesSelect();
                break;
        }
    }

    /**
     * Start the sign-in process after the user clicks the sign-in button.
     */
    private void beginUserInitiatedSignIn() {
        Log.d(TAG, "beginUserInitiatedSignIn");
        // Check to see the developer who's running this sample code read the instructions :-)
        // NOTE: this check is here only because this is a sample! Don't include this
        // check in your actual production app.
        if (!BaseGameUtils.verifySampleSetup(this, R.string.app_id)) {
            Log.w(TAG, "*** Warning: setup problems detected. Sign in may not work!");
        }

        showProgressDialog("Signing in.");
        mSignInClicked = true;
        mGoogleApiClient.connect();
    }

    /**
     * Async load AppState from Cloud Save.  This will load using stateKey APP_STATE_KEY.  After load,
     * the AppState data and metadata will be displayed.
     */
    private void cloudSaveLoad() {
        PendingResult<AppStateManager.StateResult> pendingResult = AppStateManager.load(
                mGoogleApiClient, APP_STATE_KEY);

        showProgressDialog("Loading Cloud Save");
        ResultCallback<AppStateManager.StateResult> callback =
                new ResultCallback<AppStateManager.StateResult>() {
            @Override
            public void onResult(AppStateManager.StateResult stateResult) {
                if (stateResult.getStatus().isSuccess()) {
                    // Successfully loaded data from App State
                    displayMessage(getString(R.string.cloud_save_load_success), false);
                    byte[] data = stateResult.getLoadedResult().getLocalData();
                    setData(new String(data));
                    displayAppStateMetadata(stateResult.getLoadedResult().getStateKey());
                } else {
                    // Failed to load data from App State
                    displayMessage(getString(R.string.cloud_save_load_failure), true);
                    clearDataUI();
                }

                dismissProgressDialog();
            }
        };
        pendingResult.setResultCallback(callback);
    }

    /**
     * Async update AppState data in Cloud Save.  This will use stateKey APP_STATE_KEY. After save,
     * the UI will be cleared and the data will be available to load from Cloud Save.
     */
    private void cloudSaveUpdate() {
        // Use the data from the EditText as AppState data
        byte[] data = getData().getBytes();

        // Use updateImmediate to update the AppState data.  This is used for diagnostic purposes
        // so that the app can display the result of the update, however it is generally recommended
        // to use AppStateManager.update(...) in order to reduce performance and battery impact.
        PendingResult<AppStateManager.StateResult> pendingResult = AppStateManager.updateImmediate(
                mGoogleApiClient, APP_STATE_KEY, data);

        showProgressDialog("Updating Cloud Save");
        ResultCallback<AppStateManager.StateResult> callback =
                new ResultCallback<AppStateManager.StateResult>() {
            @Override
            public void onResult(AppStateManager.StateResult stateResult) {
                if (stateResult.getStatus().isSuccess()) {
                    displayMessage(getString(R.string.cloud_save_update_success), false);
                } else {
                    displayMessage(getString(R.string.cloud_save_update_failure), true);
                }

                dismissProgressDialog();
                clearDataUI();
            }
        };
        pendingResult.setResultCallback(callback);
    }

    /**
     * Async migrate the data in Cloud Save (stateKey APP_STATE_KEY) to a Snapshot in the Saved
     * Games service with unique snap 'Snapshot-{APP_STATE_KEY}'.  If no such Snapshot exists,
     * create a Snapshot and populate all fields.  If the Snapshot already exists, update the
     * appropriate data and metadata.  After migrate, the UI will be cleared and the data will be
     * available to load from Snapshots.
     */
    private void cloudSaveMigrate() {
        final boolean createIfMissing = true;

        // Note: when migrating your users from Cloud Save to Saved Games, you will need to perform
        // the migration process at most once per device.  You should keep track of the migration
        // status locally for each AppState data slot (using SharedPreferences or similar)
        // to avoid repeating network calls or migrating the same AppState data multiple times.

        // Compute SnapshotMetadata fields based on the information available from AppState.  In
        // this case there is no data available to auto-generate a description, cover image, or
        // playedTime.  It is strongly recommended that you generate unique and meaningful
        // values for these fields based on the data in your app.
        final String snapshotName = makeSnapshotName(APP_STATE_KEY);
        final String description = "Saved game #" + APP_STATE_KEY;
        final long playedTimeMillis = 60 * 60 * 1000;
        final Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);

        AsyncTask<Void, Void, Boolean> migrateTask = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                showProgressDialog("Migrating");
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                // Get AppState Data
                AppStateManager.StateResult load = AppStateManager.load(
                        mGoogleApiClient, APP_STATE_KEY).await();

                if (!load.getStatus().isSuccess()) {
                    Log.w(TAG, "Could not load App State for migration.");
                    return false;
                }

                // Get Data from AppState
                byte[] data = load.getLoadedResult().getLocalData();

                // Open the snapshot, creating if necessary
                Snapshots.OpenSnapshotResult open = Games.Snapshots.open(
                        mGoogleApiClient, snapshotName, createIfMissing).await();

                if (!open.getStatus().isSuccess()) {
                    Log.w(TAG, "Could not open Snapshot for migration.");
                    // TODO: Handle Snapshot conflicts
                    // Note: one reason for failure to open a Snapshot is conflicting saved games.
                    // This is outside the scope of this sample, however you should resolve such
                    // conflicts in your own app by following the steps outlined here:
                    // https://developers.google.com/games/services/android/savedgames#handling_saved_game_conflicts
                    return false;
                }

                // Write the new data to the snapshot
                Snapshot snapshot = open.getSnapshot();
                snapshot.getSnapshotContents().writeBytes(data);

                // Change metadata
                SnapshotMetadataChange metadataChange = new SnapshotMetadataChange.Builder()
                        .fromMetadata(snapshot.getMetadata())
                        .setCoverImage(bitmap)
                        .setDescription(description)
                        .setPlayedTimeMillis(playedTimeMillis)
                        .build();

                Snapshots.CommitSnapshotResult commit = Games.Snapshots.commitAndClose(
                        mGoogleApiClient, snapshot, metadataChange).await();

                if (!commit.getStatus().isSuccess()) {
                    Log.w(TAG, "Failed to commit Snapshot.");
                    return false;
                }

                // No failures
                return true;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result) {
                    displayMessage(getString(R.string.cloud_save_migrate_success), false);
                } else {
                    displayMessage(getString(R.string.cloud_save_migrate_failure), true);
                }

                dismissProgressDialog();
                clearDataUI();
            }
        };
        migrateTask.execute();
    }

    /**
     * Load a Snapshot from the Saved Games service based on its unique name.  After load, the UI
     * will update to display the Snapshot data and SnapshotMetadata.
     * @param snapshotName the unique name of the Snapshot.
     */
    private void savedGamesLoad(String snapshotName) {
        PendingResult<Snapshots.OpenSnapshotResult> pendingResult = Games.Snapshots.open(
                mGoogleApiClient, snapshotName, false);

        showProgressDialog("Loading Saved Game");
        ResultCallback<Snapshots.OpenSnapshotResult> callback =
                new ResultCallback<Snapshots.OpenSnapshotResult>() {
            @Override
            public void onResult(Snapshots.OpenSnapshotResult openSnapshotResult) {
                if (openSnapshotResult.getStatus().isSuccess()) {
                    displayMessage(getString(R.string.saved_games_load_success), false);
                    byte[] data = new byte[0];
                    try {
                        data = openSnapshotResult.getSnapshot().getSnapshotContents().readFully();
                    } catch (IOException e) {
                        displayMessage("Exception reading snapshot: " + e.getMessage(), true);
                    }
                    setData(new String(data));
                    displaySnapshotMetadata(openSnapshotResult.getSnapshot().getMetadata());
                } else {
                    displayMessage(getString(R.string.saved_games_load_failure), true);
                    clearDataUI();
                }

                dismissProgressDialog();
            }
        };
        pendingResult.setResultCallback(callback);
    }

    /**
     * Launch the UI to select a Snapshot from the user's Saved Games.  The result of this
     * selection will be returned to onActivityResult.
     */
    private void savedGamesSelect() {
        final boolean allowAddButton = false;
        final boolean allowDelete = false;
        Intent intent = Games.Snapshots.getSelectSnapshotIntent(
                mGoogleApiClient, "Saved Games", allowAddButton, allowDelete,
                Snapshots.DISPLAY_LIMIT_NONE);

        showProgressDialog("Loading");
        startActivityForResult(intent, RC_SELECT_SNAPSHOT);
    }

    /**
     * Update the Snapshot in the Saved Games service with new data.  Metadata is not affected,
     * however for your own application you will likely want to update metadata such as cover image,
     * played time, and description with each Snapshot update.  After update, the UI will
     * be cleared.
     */
    private void savedGamesUpdate() {
        final String snapshotName = makeSnapshotName(APP_STATE_KEY);
        final boolean createIfMissing = false;

        // Use the data from the EditText as the new Snapshot data.
        final byte[] data = getData().getBytes();

        AsyncTask<Void, Void, Boolean> updateTask = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                showProgressDialog("Updating Saved Game");
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                Snapshots.OpenSnapshotResult open = Games.Snapshots.open(
                        mGoogleApiClient, snapshotName, createIfMissing).await();

                if (!open.getStatus().isSuccess()) {
                    Log.w(TAG, "Could not open Snapshot for update.");
                    return false;
                }

                // Change data but leave existing metadata
                Snapshot snapshot = open.getSnapshot();
                snapshot.getSnapshotContents().writeBytes(data);

                Snapshots.CommitSnapshotResult commit = Games.Snapshots.commitAndClose(
                        mGoogleApiClient, snapshot, SnapshotMetadataChange.EMPTY_CHANGE).await();

                if (!commit.getStatus().isSuccess()) {
                    Log.w(TAG, "Failed to commit Snapshot.");
                    return false;
                }

                // No failures
                return true;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result) {
                    displayMessage(getString(R.string.saved_games_update_success), false);
                } else {
                    displayMessage(getString(R.string.saved_games_update_failure), true);
                }

                dismissProgressDialog();
                clearDataUI();
            }
        };
        updateTask.execute();
    }

    /**
     * Generate a unique Snapshot name from an AppState stateKey.
     * @param appStateKey the stateKey for the Cloud Save data.
     * @return a unique Snapshot name that maps to the stateKey.
     */
    private String makeSnapshotName(int appStateKey) {
        return "Snapshot-" + String.valueOf(appStateKey);
    }

    /**
     * Display either the signed-in or signed-out view, depending on the user's state.
     */
    private void updateUI() {
        // Show signed in or signed out view
        if (isSignedIn()) {
            findViewById(R.id.layout_signed_in).setVisibility(View.VISIBLE);
            findViewById(R.id.layout_signed_out).setVisibility(View.GONE);
            displayMessage(getString(R.string.message_signed_in), false);
        } else {
            findViewById(R.id.layout_signed_in).setVisibility(View.GONE);
            findViewById(R.id.layout_signed_out).setVisibility(View.VISIBLE);
            displayMessage(getString(R.string.message_sign_in), false);
        }
    }

    /**
     * Replace the data displaying in the EditText.
     * @param data the String to display.
     */
    private void setData(String data) {
        EditText dataEditText = (EditText) findViewById(R.id.edit_game_data);

        if (data == null) {
            dataEditText.setText("");
        } else {
            dataEditText.setText(data);
        }
    }

    /**
     * Get the data from the EditText.
     * @return the String in the EditText, or "" if empty.
     */
    private String getData() {
        EditText dataEditText = (EditText) findViewById(R.id.edit_game_data);
        return dataEditText.getText().toString();
    }

    /**
     * Display a status message for the last operation at the bottom of the screen.
     * @param msg the message to display.
     * @param error true if an error occurred, false otherwise.
     */
    private void displayMessage(String msg, boolean error) {
        // Set text
        TextView messageView = (TextView) findViewById(R.id.text_message);
        messageView.setText(msg);

        // Set text color
        if (error) {
            messageView.setTextColor(Color.RED);
        } else {
            messageView.setTextColor(Color.BLACK);
        }
    }

    /**
     * Display metadata about AppState save data,
     * @param stateKey the slot stateKey of the AppState.
     */
    private void displayAppStateMetadata(int stateKey) {
        TextView metaDataView = (TextView) findViewById(R.id.text_metadata);

        String metadataStr = "Source: Cloud Save" + '\n'
                + "State Key: " + stateKey;
        metaDataView.setText(metadataStr);
    }

    /**
     * Display metadata about Snapshot save data.
     * @param metadata the SnapshotMetadata associated with the saved game.
     */
    private void displaySnapshotMetadata(SnapshotMetadata metadata) {
        TextView metaDataView = (TextView) findViewById(R.id.text_metadata);

        if (metadata == null) {
            metaDataView.setText("");
            return;
        }

        String metadataStr = "Source: Saved Games" + '\n'
                + "Description: " + metadata.getDescription() + '\n'
                + "Name: " + metadata.getUniqueName() + '\n'
                + "Last Modified: " + String.valueOf(metadata.getLastModifiedTimestamp()) + '\n'
                + "Played Time: " + String.valueOf(metadata.getPlayedTime()) + '\n'
                + "Cover Image URL: " + metadata.getCoverImageUrl();
        metaDataView.setText(metadataStr);
    }

    /**
     * Clear the data and metadata displays.
     */
    private void clearDataUI() {
        // Clear the Game Data field and the Metadata field
        EditText dataEditText = (EditText) findViewById(R.id.edit_game_data);
        TextView metaDataView = (TextView) findViewById(R.id.text_metadata);

        dataEditText.setText("");
        metaDataView.setText("");
    }

    /**
     * Determine if the Google API Client is signed in and ready to access Games APIs.
     * @return true if client exits and is signed in, false otherwise.
     */
    private boolean isSignedIn() {
        return (mGoogleApiClient != null && mGoogleApiClient.isConnected());
    }

    /**
     * Show a progress dialog for asynchronous operations.
     * @param msg the message to display.
     */
    private void showProgressDialog(String msg) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setIndeterminate(true);
        }

        mProgressDialog.setMessage(msg);
        mProgressDialog.show();
    }

    /**
     * Hide the progress dialog, if it was showing.
     */
    private void dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }
}
