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

import android.app.NativeActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.widget.Toast;

import com.google.android.gms.games.GamesClient;
import com.google.example.games.basegameutils.GameHelper;

/**
 * Base activity for the game. This class extends NativeActivity to implement
 * the necessary callbacks to use the Google Play Games API. Particularly, it
 * implements the GameHelper.GameHelperListener interface and manages the
 * GameHelper object through its lifecycle.
 */
public class BaseGameNativeActivity extends NativeActivity
            implements GameHelper.GameHelperListener {

    // Result code we use with startActivityForResult() when invoking the
    // game UIs like achievements and leaderboard
    private static final int RC_UNUSED = 11999;

    // the GameHelper object we use to access the Play Games API
    GameHelper mHelper;

    // We call this method to report the sign in state to the native code
    private native void native_ReportSignInState(boolean isSignedIn, boolean isInProgress);

    // Load the game's native code:
    static {
        System.loadLibrary("game");
    }


    /**
     * Called when Activity is created. Here, we create and set up the GameHelper
     * object.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // create the GameHelper
        mHelper = new GameHelper(this);

        // always enable debug logs, since this is a sample. For a production application, this
        // might not be a good approach.
        mHelper.enableDebugLog(true, "EndlessTunnel:GameHelper");

        // setup the helper. We request the GamesClient and the AppStateClient since
        // we're using the games API and also the Cloud Save API. If we also needed
        // Google+, we could add GameHelper.CLIENT_PLUS.
        mHelper.setup(this, GameHelper.CLIENT_GAMES | GameHelper.CLIENT_APPSTATE);
    }

    /**
     * Called when Activity is started. Every time the Activity is started, we need
     * to restart the sign-in process. So we report this fact to native code and
     * begin the sign-in process via GameHelper.
     */
    @Override
    public void onStart() {
        super.onStart();

        // report that sign in is in progress (remember that every time the Activity gets
        // onStart(), the sign in process has to start again).
        native_ReportSignInState(false, true);

        mHelper.onStart(this);
    }


    /**
     * Called when Activity is stopped. Here, we inform the GameHelper that the Activity
     * has stopped, and report this fact to the native code as well.
     */
    @Override
    public void onStop() {
        super.onStop();

        // no longer signed in, but we're in process of recovering it (and will be until
        // the app returns to the foreground)
        native_ReportSignInState(false, true);

        mHelper.onStop();
    }


    /**
     * Called when Activity gets a result. We simply pass it along to GameHelper.
     * This is necessary because GameHelper uses secondary activities during the
     * sign-in flow, and needs to get the results for those.
     */
    @Override
    public void onActivityResult(int request, int response, Intent data) {
        super.onActivityResult(request, response, data);
        mHelper.onActivityResult(request, response, data);
    }


    /**
     * Called when sign in succeeds. From this point on, we can make calls to the
     * Play Games API. We report the success to native code.
     */
    @Override
    public void onSignInSucceeded() {
        Log.d("BaseGameNativeActivity", "Sign-in succeeded. Reporting to native code.");

        // report that sign-in has succeeded
        native_ReportSignInState(true, false);
    }

    /**
     * Called when sign in fails. From this point on, we cannot make calls to the
     * Play Games API. We report the failure to native code.
     */
    @Override
    public void onSignInFailed() {
        Log.d("BaseGameNativeActivity", "Sign-in failed. Reporting to native code.");

        // report that sign-in has failed
        native_ReportSignInState(false, false);
    }


    /**
     * Starts the sign-in process in the UI thread. This method can be called from any
     * thread (in particular, it will typically be called from the games thread). It
     * starts the sign in process (i.e. showing dialogs, etc).
     */
    public void postStartSignIn() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                native_ReportSignInState(false, true);
                mHelper.beginUserInitiatedSignIn();
            }
        });
    }


    /**
     * Starts the sign-out process in the UI thread. This method can be called from any
     * thread (in particular, it will typically be called from the games thread). It
     * starts the sign out process (i.e. showing dialogs, etc).
     */
    public void postStartSignOut() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mHelper.signOut();
            }
        });
    }


    /**
     * Shows the built-in achievements screen from the UI thread. This method can be called
     * from any thread (in particular, it will typically be called from the games thread).
     */
    public void postShowAchievements() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mHelper.isSignedIn()) {
                    GamesClient c = mHelper.getGamesClient();
                    startActivityForResult(c.getAchievementsIntent(), RC_UNUSED);
                }
            }
        });
    }


    /**
     * Shows the built-in leaderboards screen from the UI thread. This method can be called
     * from any thread (in particular, it will typically be called from the games thread).
     */
    public void postShowLeaderboards() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mHelper.isSignedIn()) {
                    GamesClient c = mHelper.getGamesClient();
                    startActivityForResult(c.getAllLeaderboardsIntent(), RC_UNUSED);
                }
            }
        });
    }


    /**
     * Shows the built-in leaderboard screen from the UI thread. This method can be called
     * from any thread (in particular, it will typically be called from the games thread).
     *
     * @param lbId the ID of the leaderboard to show.
     */
    public void postShowLeaderboard(final String lbId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mHelper.isSignedIn()) {
                    GamesClient c = mHelper.getGamesClient();
                    startActivityForResult(c.getLeaderboardIntent(lbId), RC_UNUSED);
                }
            }
        });
    }


    /**
     * Submits a score to a leaderboard. This method can be called from any thread.
     *
     * @param lbId the ID of the leaderboard to show.
     * @param score the score to submit.
     */
    public void postSubmitScore(final String lbId, final long score) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mHelper.isSignedIn()) {
                    GamesClient c = mHelper.getGamesClient();
                    c.submitScore(lbId, score);
                }
            }
        });
    }


    /**
     * Unlocks an achievement. This method can be called from any thread.
     *
     * @param achId the ID of the achievement to unlock.
     */
    public void postUnlockAchievement(final String achId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mHelper.isSignedIn()) {
                    GamesClient c = mHelper.getGamesClient();
                    c.unlockAchievement(achId);
                }
            }
        });
    }


    /**
     * Increments an achievement. This method can be called from any thread.
     *
     * @param achId the ID of the achievement to increment.
     * @param stpes the number of steps to increment.
     */
    public void postIncrementAchievement(final String achId, final int steps) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mHelper.isSignedIn()) {
                    GamesClient c = mHelper.getGamesClient();
                    c.incrementAchievement(achId, steps);
                }
            }
        });
    }


    // These methods return a given's device motion range on the X and Y axis.
    public float getDeviceMotionRangeMinX(int deviceId, int source) {
        return InputDevice.getDevice(deviceId).getMotionRange(MotionEvent.AXIS_X, source).getMin();
    }
    public float getDeviceMotionRangeMaxX(int deviceId, int source) {
        return InputDevice.getDevice(deviceId).getMotionRange(MotionEvent.AXIS_X, source).getMax();
    }
    public float getDeviceMotionRangeMinY(int deviceId, int source) {
        return InputDevice.getDevice(deviceId).getMotionRange(MotionEvent.AXIS_Y, source).getMin();
    }
    public float getDeviceMotionRangeMaxY(int deviceId, int source) {
        return InputDevice.getDevice(deviceId).getMotionRange(MotionEvent.AXIS_Y, source).getMax();
    }

    // Returns the API level
    public int getApiLevel() {
        return android.os.Build.VERSION.SDK_INT;
    }
}

