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

package com.google.example.games.tq;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.gms.games.Games;
import com.google.example.games.basegameutils.BaseGameActivity;
import com.google.example.games.tq.R;

/**
 * Trivial quest. A sample game that sets up the Google Play game services
 * API and allows the user to click a button to win (yes, incredibly epic).
 * Even though winning the game is fun, the purpose of this sample is to
 * illustrate the simplest possible game that uses the API.
 *
 * @author Bruno Oliveira (Google)
 *
 */
public class MainActivity extends BaseGameActivity implements View.OnClickListener {
    private static boolean DEBUG_ENABLED = true;
    private static final String TAG = "TrivialQuest";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        enableDebugLog(DEBUG_ENABLED, TAG);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        findViewById(R.id.button_sign_in).setOnClickListener(this);
        findViewById(R.id.button_sign_out).setOnClickListener(this);
        findViewById(R.id.button_win).setOnClickListener(this);
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
        showSignInBar();
    }

    /**
     * Called to notify us that sign in succeeded. We react by loading the loot from the
     * cloud and updating the UI to show a sign-out button.
     */
    @Override
    public void onSignInSucceeded() {
        // Sign-in worked!
        showSignOutBar();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
        case R.id.button_sign_in:
            // Check to see the developer who's running this sample code read the instructions :-)
            // NOTE: this check is here only because this is a sample! Don't include this
            // check in your actual production app.
            if (!verifyPlaceholderIdsReplaced()) {
                showAlert("Error: sample not correctly set up. See README!");
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
        case R.id.button_win:
            // win!
            showAlert(getString(R.string.victory), getString(R.string.you_won));
            if (getApiClient().isConnected()) {
                // unlock the "Trivial Victory" achievement.
                Games.Achievements.unlock(getApiClient(), getString(R.string.trivial_victory_achievement_id));
            }
            break;
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
            R.string.app_id, R.string.trivial_victory_achievement_id
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
