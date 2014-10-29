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
import com.google.android.gms.appstate.AppStateManager.*;
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
import com.google.android.gms.games.snapshot.Snapshots.*;
import com.google.android.gms.plus.Plus;
import com.google.example.games.basegameutils.BaseGameUtils;


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

        // Build API client with access to Plus, Games, AppState, and SavedGames.
        // It is very important to add Drive or the SavedGames API will not work
        // Make sure to also go to console.developers.google.com and enable the Drive API for your
        // project
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API).addScope(Plus.SCOPE_PLUS_LOGIN)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .addApi(AppStateManager.API).addScope(AppStateManager.SCOPE_APP_STATE) // AppState
                .addApi(Drive.API).addScope(Drive.SCOPE_APPFOLDER) // SavedGames
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
                BaseGameUtils.showActivityResultError(this, requestCode, resultCode,
                        R.string.signin_failure, R.string.signin_other_error);
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
                        displayMessage("Failed to select Saved Game data.", true);
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
                displayMessage("No Saved Game selected.", true);
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
                savedGamesLoad();
                break;
            case R.id.button_saved_games_update:
                savedGamesUpdate();
                break;
            case R.id.button_saved_games_select:
                savedGamesSelect();
                break;
        }
    }

    private void beginUserInitiatedSignIn() {
        Log.d(TAG, "beginUserInitiatedSignIn");
        mSignInClicked = true;
        mGoogleApiClient.connect();
    }

    /**
     * Async load AppState from Cloud Save.  This will load using stateKey APP_STATE_KEY.  After load,
     * the AppState data and metadata will be displayed.
     */
    private void cloudSaveLoad() {
        PendingResult<StateResult> pendingResult = AppStateManager.load(
                mGoogleApiClient, APP_STATE_KEY);

        showProgressDialog("Loading Cloud Save");
        ResultCallback<StateResult> callback = new ResultCallback<StateResult>() {
            @Override
            public void onResult(StateResult stateResult) {
                if (stateResult.getStatus().isSuccess()) {
                    // Successfully loaded data from App State
                    displayMessage("Loaded from Cloud Save", false);
                    byte[] data = stateResult.getLoadedResult().getLocalData();
                    setData(new String(data));
                    displayAppStateMetadata(stateResult.getLoadedResult().getStateKey());
                } else {
                    // Failed to load data from App State
                    displayMessage("Failed to load from Cloud Save", true);
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
        PendingResult<StateResult> pendingResult = AppStateManager.updateImmediate(
                mGoogleApiClient, APP_STATE_KEY, data);

        showProgressDialog("Updating Cloud Save");
        ResultCallback<StateResult> callback = new ResultCallback<StateResult>() {
            @Override
            public void onResult(StateResult stateResult) {
                if (stateResult.getStatus().isSuccess()) {
                    displayMessage("Saved to Cloud Save", false);
                } else {
                    displayMessage("Failed to save to Cloud Save", true);
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

        // Compute SnapshotMetadata fields based on the information available from AppState.  In
        // this case there is no data available to auto-generate a description, cover image, or
        // playedTime.  It is strongly recommended that you generate unique values for these
        // fields based on the data in your app.
        final String snapshotName = makeSnapshotName(APP_STATE_KEY);
        final String description = "Saved game #" + APP_STATE_KEY;
        final long playedTimeMillis = 60 * 60 * 1000;
        final byte[] data = getData().getBytes();
        final Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);

        AsyncTask<Void, Void, Boolean> migrateTask = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onPreExecute() {
                showProgressDialog("Migrating");
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                // Open the snapshot, creating if necessary
                OpenSnapshotResult open = Games.Snapshots.open(
                        mGoogleApiClient, snapshotName, createIfMissing).await();

                if (!open.getStatus().isSuccess()) {
                    Log.w(TAG, "Could not open Snapshot for migration.");
                    return false;
                }

                // Write the new data to the snapshot
                Snapshot snapshot = open.getSnapshot();
                snapshot.writeBytes(data);

                // Change metadata
                SnapshotMetadataChange metadataChange = new SnapshotMetadataChange.Builder()
                        .fromMetadata(snapshot.getMetadata())
                        .setCoverImage(bitmap)
                        .setDescription(description)
                        .setPlayedTimeMillis(playedTimeMillis)
                        .build();

                CommitSnapshotResult commit = Games.Snapshots.commitAndClose(
                        mGoogleApiClient, snapshot, metadataChange).await();

                if (!commit.getStatus().isSuccess()) {
                    Log.w(TAG, "Failed to commit Snapshot.");
                    return false;
                }

                // TODO(samstern): Delete data from appstate?

                // No failures
                return true;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result) {
                    displayMessage("Migrated to Saved Games.", false);
                } else {
                    displayMessage("Failed to migrate to Saved Games.", true);
                }

                dismissProgressDialog();
                clearDataUI();
            }
        };
        migrateTask.execute();
    }

    /**
     * See {@link com.google.example.games.savedgames.MainActivity#savedGamesLoad(String)}.
     */
    private void savedGamesLoad() {
        final String snapshotName = makeSnapshotName(APP_STATE_KEY);
        savedGamesLoad(snapshotName);
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
        ResultCallback<OpenSnapshotResult> callback = new ResultCallback<OpenSnapshotResult>() {
            @Override
            public void onResult(OpenSnapshotResult openSnapshotResult) {
                if (openSnapshotResult.getStatus().isSuccess()) {
                    displayMessage("Loaded from Saved Games", false);
                    byte[] data = openSnapshotResult.getSnapshot().readFully();
                    setData(new String(data));
                    displaySnapshotMetadata(openSnapshotResult.getSnapshot().getMetadata());
                } else {
                    displayMessage("Failed to load from Saved Games", true);
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
                OpenSnapshotResult open = Games.Snapshots.open(
                        mGoogleApiClient, snapshotName, createIfMissing).await();

                if (!open.getStatus().isSuccess()) {
                    Log.w(TAG, "Could not open Snapshot for update.");
                    return false;
                }

                // Change data but leave existing metadata
                Snapshot snapshot = open.getSnapshot();
                snapshot.writeBytes(data);

                CommitSnapshotResult commit = Games.Snapshots.commitAndClose(
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
                    displayMessage("Saved to Saved Games", false);
                } else {
                    displayMessage("Failed to save to Saved Games", true);
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

    private void updateUI() {
        // Show signed in or signed out view
        if (isSignedIn()) {
            findViewById(R.id.layout_signed_in).setVisibility(View.VISIBLE);
            findViewById(R.id.layout_signed_out).setVisibility(View.GONE);
            displayMessage("Signed in.", false);
        } else {
            findViewById(R.id.layout_signed_in).setVisibility(View.GONE);
            findViewById(R.id.layout_signed_out).setVisibility(View.VISIBLE);
            displayMessage(getString(R.string.message_sign_in), false);
        }
    }

    private void setData(String data) {
        EditText dataEditText = (EditText) findViewById(R.id.edit_game_data);

        if (data == null) {
            dataEditText.setText("");
        } else {
            dataEditText.setText(data);
        }
    }

    private String getData() {
        EditText dataEditText = (EditText) findViewById(R.id.edit_game_data);
        return dataEditText.getText().toString();
    }

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

    private void displayAppStateMetadata(int stateKey) {
        TextView metaDataView = (TextView) findViewById(R.id.text_metadata);
        metaDataView.setText("");

        String metadataStr = "Source: Cloud Save" + '\n'
                + "State Key: " + stateKey;
        metaDataView.setText(metadataStr);
    }

    private void displaySnapshotMetadata(SnapshotMetadata metadata) {
        TextView metaDataView = (TextView) findViewById(R.id.text_metadata);
        metaDataView.setText("");

        if (metadata == null) {
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

    private void clearDataUI() {
        // Clear the Game Data field and the Metadata field
        EditText dataEditText = (EditText) findViewById(R.id.edit_game_data);
        TextView metaDataView = (TextView) findViewById(R.id.text_metadata);

        dataEditText.setText("");
        metaDataView.setText("");
    }

    private boolean isSignedIn() {
        return (mGoogleApiClient != null && mGoogleApiClient.isConnected());
    }

    private void showProgressDialog(String msg) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setIndeterminate(true);
        }

        mProgressDialog.setMessage(msg);
        mProgressDialog.show();
    }

    private void dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }
}
