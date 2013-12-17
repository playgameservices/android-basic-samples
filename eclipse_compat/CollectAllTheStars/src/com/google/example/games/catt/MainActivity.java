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

package com.google.example.games.catt;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.RatingBar.OnRatingBarChangeListener;
import android.widget.TextView;

import com.google.android.gms.appstate.AppStateClient;
import com.google.android.gms.appstate.OnStateLoadedListener;
import com.google.example.games.basegameutils.BaseGameActivity;

import com.google.example.games.cas.R;

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
            implements OnStateLoadedListener, View.OnClickListener, OnRatingBarChangeListener {
    private static final boolean ENABLE_DEBUG = true;
    private static final String TAG = "CollectAllTheStars";

    // the state key under which we store our data. Since our data is small and
    // this is a simple example, we only use this one state key, but your app can
    // use multiple keys! Be sure not to exceed the maximum number of keys
    // given by AppStateClient.getMaxNumKeys().
    private static final int OUR_STATE_KEY = 0;

    // current save game
    SaveGame mSaveGame = new SaveGame();

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

    public MainActivity() {
        // request that superclass initialize and manage the AppStateClient for us
        super(BaseGameActivity.CLIENT_APPSTATE);
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
        if (item.getItemId() == R.id.menu_sync) {
            // Player wants to force a resync.
            // NOTE: this button exists in this sample for debug purposes and so that you can
            // see the effects immediately. A game probably shouldn't have a "Force Resync"
            // button (or at least not one that's so prominently displayed in the UI).
            saveToCloud();
            loadFromCloud();
            return true;
        }
        return false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        mLoadingDialog = new ProgressDialog(this);
        mLoadingDialog.setMessage(getString(R.string.loading_from_cloud));
        updateUi();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mLoadingDialog != null) {
            mLoadingDialog.dismiss();
            mLoadingDialog = null;
        }
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

    /**
     * Checks that the developer (that's you!) read the instructions.
     *
     * IMPORTANT: a method like this SHOULD NOT EXIST in your production app!
     * It merely exists here to check that anyone running THIS PARTICULAR SAMPLE
     * did what they were supposed to in order for the sample to work.
     */
    boolean verifyPlaceholderIdsReplaced() {
        final boolean CHECK_PKGNAME = true; // set to false to disable check
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

    /** Prints a log message (convenience method). */
    void log(String message) {
        Log.d(TAG, message);
    }

    /** Complains to the user about an error. */
    void complain(String error) {
        showAlert("Error", error);
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
        if (!mAlreadyLoadedState) {
            loadFromCloud();
        }
    }

    void loadFromCloud() {
        mLoadingDialog.show();
        getAppStateClient().loadState(this, OUR_STATE_KEY);
        // this will trigger a call to onStateConflict or onStateLoaded
    }

    void saveToCloud() {
        getAppStateClient().updateState(OUR_STATE_KEY, mSaveGame.toBytes());
        // Note: this is a fire-and-forget call. It will NOT trigger a call to any callbacks!
    }

    @Override
    public void onStateConflict(int stateKey, String resolvedVersion, byte[] localData,
            byte[] serverData) {
        // Need to resolve conflict between the two states.
        // We do that by taking the union of the two sets of cleared levels,
        // which means preserving the maximum star rating of each cleared
        // level:
        SaveGame localGame = new SaveGame(localData);
        SaveGame serverGame = new SaveGame(serverData);
        SaveGame resolvedGame = localGame.unionWith(serverGame);
        getAppStateClient().resolveState(this, OUR_STATE_KEY, resolvedVersion,
                resolvedGame.toBytes());
    }

    @Override
    public void onStateLoaded(int statusCode, int stateKey, byte[] localData) {
        mLoadingDialog.dismiss();
        switch (statusCode) {
        case AppStateClient.STATUS_OK:
            // Data was successfully loaded from the cloud: merge with local data.
            mSaveGame = mSaveGame.unionWith(new SaveGame(localData));
            mAlreadyLoadedState = true;
            hideAlertBar();
            break;
        case AppStateClient.STATUS_STATE_KEY_NOT_FOUND:
            // key not found means there is no saved data. To us, this is the same as
            // having empty data, so we treat this as a success.
            mAlreadyLoadedState = true;
            hideAlertBar();
            break;
        case AppStateClient.STATUS_NETWORK_ERROR_NO_DATA:
            // can't reach cloud, and we have no local state. Warn user that
            // they may not see their existing progress, but any new progress won't be lost.
            showAlertBar(R.string.no_data_warning);
            break;
        case AppStateClient.STATUS_NETWORK_ERROR_STALE_DATA:
            // can't reach cloud, but we have locally cached data.
            showAlertBar(R.string.stale_data_warning);
            break;
        case AppStateClient.STATUS_CLIENT_RECONNECT_REQUIRED:
            // need to reconnect AppStateClient
            reconnectClients(BaseGameActivity.CLIENT_APPSTATE);
            break;
        default:
            // error
            showAlertBar(R.string.load_error_warning);
            break;
        }

        updateUi();
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
        saveToCloud();
    }

    private void showAlertBar(int resId) {
        ((TextView) findViewById(R.id.alert_bar)).setText(getString(resId));
        ((TextView) findViewById(R.id.alert_bar)).setVisibility(View.VISIBLE);
    }

    private void hideAlertBar() {
        ((TextView) findViewById(R.id.alert_bar)).setVisibility(View.GONE);
    }
}
