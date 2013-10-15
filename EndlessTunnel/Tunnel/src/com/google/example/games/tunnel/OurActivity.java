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

package com.google.example.games.tunnel;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.InputDevice;
import android.widget.Toast;

import com.google.android.gms.appstate.AppStateClient;
import com.google.android.gms.appstate.OnStateLoadedListener;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.leaderboard.LeaderboardBuffer;
import com.google.android.gms.games.leaderboard.LeaderboardScore;
import com.google.android.gms.games.leaderboard.LeaderboardScoreBuffer;
import com.google.android.gms.games.leaderboard.LeaderboardVariant;
import com.google.android.gms.games.leaderboard.OnLeaderboardScoresLoadedListener;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class OurActivity extends BaseGameNativeActivity
        implements OnLeaderboardScoresLoadedListener, OnStateLoadedListener {

    // when updating this, don't forget to also update src/tunnel/Tunnel/jni/game_ids.hpp
    private static final String LEADERBOARD_ID = "CgkItLL7uJAZEAIQAg";

    // debug tag
    private static final String TAG = "EndlessTunnel";

    // score entries we have cached (scores are loaded with on onSignInSucceeded)
    private ArrayList<ScoreEntry> mScoreEntries = new ArrayList<ScoreEntry>();

    // maximum number of scores to load
    private static final int MAX_SCORES_TO_LOAD = 25;

    // displacement of the encouragement toast
    private static final int TOAST_NUDGE_Y = 0;

    // base score used to determine which encouragement toasts to show
    private int mBaseScore = 0;

    // Google+ ID of the currently signed-in user
    private String mMyId = null;

    // game level according to the cloud save data we loaded
    private int mCloudLevel = -1;

    // did we do our one-time post-sign-in initialization (loading leaderboards, etc)?
    private boolean mSignInInitDone = false;

    // the cloud save slot # we use (this game is very simple, so we only use one slot)
    private static final int OUR_STATE_KEY = 0;

    // reference to our input device listener, on API >= 16. We declare it as Object
    // because the actual class is only available on API >= 16 and we don't want to break
    // compatibility
    private Object mInputDeviceListener = null;

    private boolean mInputDevicesScanned = false;

    // we call this method to inform native code of the result of the cloud load operation
    private native void native_ReportCloudLoadResult(boolean success, int savedLevel);

    // we call this method to inform native code of the presence of a joystick
    private native void native_ReportJoystickPresent(boolean present);

    /**
     * Represents a leaderboard entry we've loaded. We use these to show the encouragement
     * toasts like "You've just beat Xyz".
     */
    private class ScoreEntry implements Comparable<ScoreEntry> {
        String name;
        String id;
        int score;
        boolean shown;
        ScoreEntry(String id, String name, int score) {
            this.id = id;
            this.shown = (score <= mBaseScore);
            this.name = name;
            this.score = score;
        }

        void update(int score) {
            this.score = score;
            this.shown = (score <= mBaseScore);
        }

        void update() {
            this.shown = (score <= mBaseScore);
        }

        @Override
        public int compareTo(ScoreEntry other) {
            return this.score - other.score;
        }
    }


    /** Returns the path were small files for this game should be saved (internal memory). */
    public String getSavePath() {
        String savePath = getFilesDir().getAbsolutePath();
        Log.d(TAG, "Save path (internal memory): " + savePath);
        return savePath;
    }


    /**
     * Called when sign in succeeds. We react by loading the leaderboards we are interested
     * in, and also loading the cloud save data.
     */
    @Override
    public void onSignInSucceeded() {
        super.onSignInSucceeded();

        // Only do the setup the first time we sign in successfully for a given user
        // (we reset this flag when we sign out)
        if (!mSignInInitDone) {
            mSignInInitDone = true;

            GamesClient gamesClient = mHelper.getGamesClient();

            mMyId = gamesClient.getCurrentPlayerId();

            // load scores (top scores and player-centered scores for public & social leaderboards)
            gamesClient.loadPlayerCenteredScores(this, LEADERBOARD_ID,
                    LeaderboardVariant.TIME_SPAN_ALL_TIME, LeaderboardVariant.COLLECTION_PUBLIC,
                    MAX_SCORES_TO_LOAD);
            gamesClient.loadPlayerCenteredScores(this, LEADERBOARD_ID,
                    LeaderboardVariant.TIME_SPAN_ALL_TIME, LeaderboardVariant.COLLECTION_SOCIAL,
                    MAX_SCORES_TO_LOAD);
            gamesClient.loadTopScores(this, LEADERBOARD_ID, LeaderboardVariant.TIME_SPAN_ALL_TIME,
                    LeaderboardVariant.COLLECTION_PUBLIC, MAX_SCORES_TO_LOAD);
            gamesClient.loadTopScores(this, LEADERBOARD_ID, LeaderboardVariant.TIME_SPAN_ALL_TIME,
                    LeaderboardVariant.COLLECTION_SOCIAL, MAX_SCORES_TO_LOAD);

            // load cloud save data
            Log.d(TAG, "Loading state from cloud.");
            mHelper.getAppStateClient().loadState(this, OUR_STATE_KEY);
        }
    }


    /** Returns the ScoreEntry with the given id, or null if not found. */
    private ScoreEntry findScoreEntry(String id) {
        int i;
        for (i = 0; i < mScoreEntries.size(); i++) {
            if (mScoreEntries.get(i).id.equals(id)) {
                return mScoreEntries.get(i);
            }
        }
        return null;
    }


    /**
     * Called when leaderboard scores are loaded. We store them in our local data
     * structure for later use when showing the encouragement popups to the user.
     */
    @Override
    public void onLeaderboardScoresLoaded(int statusCode, LeaderboardBuffer leaderboard,
            LeaderboardScoreBuffer scores) {

        // STATUS_OK means we got fresh data from the server;
        // NETWORK_ERROR_STALE_DATA means we got locally cached data. Both are
        // good enough for our purposes.
        boolean success = (statusCode == GamesClient.STATUS_OK || statusCode ==
                GamesClient.STATUS_NETWORK_ERROR_STALE_DATA);

        if (!success) {
            // something went wrong...
            Log.w(TAG, "Error loading leaderboards, status code " + statusCode);
            return;
        }
        Log.d(TAG, "Loaded " + scores.getCount() + " scores. Processing.");

        int i;
        for (i = 0; i < scores.getCount(); i++) {
            LeaderboardScore thisScore = scores.get(i);
            String playerId = thisScore.getScoreHolder().getPlayerId();
            ScoreEntry entry = findScoreEntry(playerId);
            int score = (int) thisScore.getRawScore();
            String name = thisScore.getScoreHolderDisplayName();

            if (playerId.equals(mMyId)) {
                // don't add my own score to the list
                continue;
            }

            if (entry == null) {
                mScoreEntries.add(new ScoreEntry(playerId, name, score));
            } else if (entry.score < score) {
                entry.update(score);
            }
        }
        Collections.sort(mScoreEntries);
    }


    /**
     * Shows encouragement toasts based on the given score. Encouragement toasts are
     * the toasts that say "You've just beat John Xyz".
     */
    void showEncouragementToasts(int score) {
        String pre = getResources().getString(R.string.you_beat);
        for (ScoreEntry entry : mScoreEntries) {
            if (entry.score < score && !entry.shown) {
                Toast t = Toast.makeText(this, pre + " " + entry.name + " (" + entry.score + ")",
                        Toast.LENGTH_LONG);
                t.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, TOAST_NUDGE_Y);
                t.getView().setBackgroundResource(0);
                t.show();
                entry.shown = true;
            }
        }
    }


    /**
     * Reset the encouragement toasts. This will reset the flags that indicate which
     * toasts have been shown, so they can be shown again (for example, when restarting
     * a level)
     *
     * @param score The starting score the player has. Toasts for lower or equal scores
     * will not be shown; toasts for greater scores will be shown when their scores
     * are reached.
     */
    void resetEncouragementToasts(int baseScore) {
        mBaseScore = baseScore;
        for (ScoreEntry entry : mScoreEntries) {
            entry.update();
        }
    }


    /** Convenience method that calls showEncouragementToasts on the UI thread. */
    void postShowEncouragementToasts(final int score) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showEncouragementToasts(score);
            }
        });
    }


    /** Convenience method that calls resetEncouragementToasts on the UI thread. */
    void postResetEncouragementToasts(final int score) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                resetEncouragementToasts(score);
            }
        });
    }


    /** Encode a level number as a byte array for cloud save. */
    byte[] encodeLevel(int level) {
        // Our serialized format is really simple:
        byte[] data = new byte[2];
        data[0] = 0x01;
        data[1] = (byte)level;
        return data;
    }


    /** Decode a byte array from cloud save into a level number. */
    int decodeLevel(byte[] data) {
        if (data[0] != 0x01) {
            Log.e(TAG, "Wrong data format byte on cloud-save data, " + (int)data[0]);
            return -1;
        }
        return (int)data[1];
    }


    /**
     * Called when state is loaded from the cloud. We analyze the response code and
     * report appropriately to native code.
     */
    @Override
    public void onStateLoaded(int statusCode, int stateKey, byte[] data) {
        int level = 0;
        Log.d(TAG, "State loaded, status = " + statusCode);
        switch (statusCode) {
            case AppStateClient.STATUS_OK:
            case AppStateClient.STATUS_NETWORK_ERROR_STALE_DATA:
                // we have good data -- which is either it's fresh (STATUS_OK) or
                // from the local cache (STALE_DATA), but either way it's good enough for us:
                level = decodeLevel(data);
                // fall through
            case AppStateClient.STATUS_STATE_KEY_NOT_FOUND:
                // KEY_NOT_FOUND means we never saved anything, so this is equivalent to level 0
                Log.d(TAG, "Level loaded from cloud data: " + level);
                mCloudLevel = level;
                native_ReportCloudLoadResult(true, level);
                break;
            case AppStateClient.STATUS_NETWORK_ERROR_OPERATION_DEFERRED:
                // This is returned after saving state to the cloud if the state could not
                // be pushed to the server immediately. For the purposes of this game,
                // no action is necessary in this case.
                break;
            case AppStateClient.STATUS_NETWORK_ERROR_NO_DATA:
                // There was a network error and no local data was available.
                Log.w(TAG, "*** Cloud save data can't be loaded (network error).");
                showCloudLoadError(true);
                break;
            default:
                // There was an error loading data. Warn the user.
                Log.e(TAG, "*** Cloud save data can't be loaded, status code " + statusCode);
                showCloudLoadError(false);
                break;
        }
    }


    /**
     * Called when there's a cloud conflict. Our conflict resolution logic is pretty
     * straightforward: highest level wins.
     */
    @Override
    public void onStateConflict(int stateKey, String resolvedVersion, byte[] localData,
            byte[] serverData) {
        int localLevel = decodeLevel(localData);
        int serverLevel = decodeLevel(serverData);

        Log.d(TAG, "Cloud save conflict. Local level " + localLevel + ", server " + serverLevel);

        // our conflict resolution algorithm is pretty simple: highest level wins!
        int resolvedLevel = localLevel > serverLevel ? localLevel : serverLevel;
        if (resolvedLevel < 0) {
            resolvedLevel = 0;
        }
        Log.d(TAG, "Resolving cloud save conflict. Level = " + resolvedLevel);
        mHelper.getAppStateClient().resolveState(this, stateKey, resolvedVersion,
                encodeLevel(resolvedLevel));
    }


    /**
     * Shows the cloud error dialog. This is an ugly standard Android dialog and is in
     * no way recommended for games! We just use it because it's easy to implement for
     * the purposes of this sample.
     *
     * @param isNetwork Indicates whether or not the error is due to a network issue.
     */
    private void showCloudLoadError(boolean isNetwork) {
        int msg = isNetwork ? R.string.cloud_load_error_network : R.string.cloud_load_error_other;
        new AlertDialog.Builder(this)
            .setMessage(getResources().getString(msg))
            .setCancelable(false)
            .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // report error to native code
                    native_ReportCloudLoadResult(false, 0);
                }
            })
            .create().show();
    }


    /**
     * Requests that data be saved to the cloud. Can be called from any thread. This method
     * is called from native code to save the level to the cloud.
     *
     * @param level The level to save to the cloud.
     */
    public void postSaveState(final int level) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AppStateClient client = mHelper.getAppStateClient();
                if (client.isConnected()) {
                    Log.d(TAG, "Saving level to cloud: " + level);
                    client.updateState(OUR_STATE_KEY, encodeLevel(level));
                }
            }
        });
    }


    /** Called from native code to start the sign-out process. */
    @Override
    public void postStartSignOut() {
        // when we sign out, we want to re-do the initialization (loading leaderboards, etc)
        // on sign-in, so we reset this flag to false:
        mSignInInitDone = false;

        super.postStartSignOut();
    }


    @Override
    public void onStart() {
        super.onStart();

        // if we haven't scanned the input devices yet, do it now.
        if (!mInputDevicesScanned) {
            mInputDevicesScanned = true;
            if (android.os.Build.VERSION.SDK_INT >= 16) {
                scanInputDevicesJB();
            } else {
                Log.d(TAG, "Can't scan input devices because API < 16.");
            }
        }
    }


    /**
     * Scans input devices to determine if user has a joystick. This method must only
     * be called if the API level is 16+ (Jellybean and above), since it uses InputManager.
     */
    private void scanInputDevicesJB() {
        Log.d(TAG, "Scanning input devices.");

        // get the InputManager
        android.hardware.input.InputManager inputManager =
                (android.hardware.input.InputManager) getSystemService(Context.INPUT_SERVICE);
        if (inputManager == null) {
            Log.e(TAG, "Failed to get InputManager even though API is >= 16.");
            return;
        }

        // get the list of input device IDs
        int[] ids = inputManager.getInputDeviceIds();
        Log.d(TAG, "# input devices: " + ids.length);
        boolean hasJoy = false;
        for (int id : ids) {
            InputDevice dev = inputManager.getInputDevice(id);
            int sources = dev.getSources();
            int c = sources & InputDevice.SOURCE_CLASS_MASK;
            boolean isJoy = (c & InputDevice.SOURCE_CLASS_JOYSTICK) != 0;
            Log.d(TAG, "Input device id=" + id + ", sources=" + sources + ", joy=" + isJoy);
            hasJoy = hasJoy || isJoy;
        }

        // report joystick presence or absence to native code
        Log.d(TAG, "Reporting JOYSTICK " + (hasJoy ? "PRESENT" : "ABSENT"));
        native_ReportJoystickPresent(hasJoy);

        // install a listener so we get notified of subsequent changes
        if (mInputDeviceListener == null) {
            Log.d(TAG, "Installing listener to get notified of input device changes.");
            android.hardware.input.InputManager.InputDeviceListener listener;
            listener = new android.hardware.input.InputManager.InputDeviceListener() {
                @Override
                public void onInputDeviceAdded(int deviceId) {
                    scanInputDevicesJB();
                }
                @Override
                public void onInputDeviceChanged(int deviceId) {
                    scanInputDevicesJB();
                }
                @Override
                public void onInputDeviceRemoved(int deviceId) {
                    scanInputDevicesJB();
                }
            };

            // hold a reference to the listener (so it doesn't get garbage collected --
            // just in case the API is holding a weak reference to it)
            mInputDeviceListener = listener;

            // register the listener
            inputManager.registerInputDeviceListener(listener, null);
        }
    }
}

