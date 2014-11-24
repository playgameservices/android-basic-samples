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

package com.google.example.games.tanc;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.Player;
import com.google.android.gms.plus.Plus;
import com.google.example.games.basegameutils.BaseGameUtils;

/**
 * Our main activity for the game.
 *
 * IMPORTANT: Before attempting to run this sample, please change
 * the package name to your own package name (not com.android.*) and
 * replace the IDs on res/values/ids.xml by your own IDs (you must
 * create a game in the developer console to get those IDs).
 *
 * This is a very simple game where the user selects "easy mode" or
 * "hard mode" and then the "gameplay" consists of inputting the
 * desired score (0 to 9999). In easy mode, you get the score you
 * request; in hard mode, you get half.
 *
 * @author Bruno Oliveira
 */
public class MainActivity extends FragmentActivity
        implements MainMenuFragment.Listener,
        GameplayFragment.Listener, WinFragment.Listener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    // Fragments
    MainMenuFragment mMainMenuFragment;
    GameplayFragment mGameplayFragment;
    WinFragment mWinFragment;

    // Client used to interact with Google APIs
    private GoogleApiClient mGoogleApiClient;

    // Are we currently resolving a connection failure?
    private boolean mResolvingConnectionFailure = false;

    // Has the user clicked the sign-in button?
    private boolean mSignInClicked = false;

    // Automatically start the sign-in flow when the Activity starts
    private boolean mAutoStartSignInFlow = true;

    // request codes we use when invoking an external activity
    private static final int RC_RESOLVE = 5000;
    private static final int RC_UNUSED = 5001;
    private static final int RC_SIGN_IN = 9001;

    // tag for debug logging
    final boolean ENABLE_DEBUG = true;
    final String TAG = "TanC";

    // playing on hard mode?
    boolean mHardMode = false;

    // achievements and scores we're pending to push to the cloud
    // (waiting for the user to sign in, for instance)
    AccomplishmentsOutbox mOutbox = new AccomplishmentsOutbox();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create the Google API Client with access to Plus and Games
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API).addScope(Plus.SCOPE_PLUS_LOGIN)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .build();

        // create fragments
        mMainMenuFragment = new MainMenuFragment();
        mGameplayFragment = new GameplayFragment();
        mWinFragment = new WinFragment();

        // listen to fragment events
        mMainMenuFragment.setListener(this);
        mGameplayFragment.setListener(this);
        mWinFragment.setListener(this);

        // add initial fragment (welcome fragment)
        getSupportFragmentManager().beginTransaction().add(R.id.fragment_container,
                mMainMenuFragment).commit();

        // IMPORTANT: if this Activity supported rotation, we'd have to be
        // more careful about adding the fragment, since the fragment would
        // already be there after rotation and trying to add it again would
        // result in overlapping fragments. But since we don't support rotation,
        // we don't deal with that for code simplicity.

        // load outbox from file
        mOutbox.loadLocal(this);
    }

    // Switch UI to the given fragment
    void switchToFragment(Fragment newFrag) {
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, newFrag)
                .commit();
    }

    private boolean isSignedIn() {
        return (mGoogleApiClient != null && mGoogleApiClient.isConnected());
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart(): connecting");
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop(): disconnecting");
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onStartGameRequested(boolean hardMode) {
        startGame(hardMode);
    }

    @Override
    public void onShowAchievementsRequested() {
        if (isSignedIn()) {
            startActivityForResult(Games.Achievements.getAchievementsIntent(mGoogleApiClient),
                    RC_UNUSED);
        } else {
            BaseGameUtils.makeSimpleDialog(this, getString(R.string.achievements_not_available)).show();
        }
    }

    @Override
    public void onShowLeaderboardsRequested() {
        if (isSignedIn()) {
            startActivityForResult(Games.Leaderboards.getAllLeaderboardsIntent(mGoogleApiClient),
                    RC_UNUSED);
        } else {
            BaseGameUtils.makeSimpleDialog(this, getString(R.string.leaderboards_not_available)).show();
        }
    }

    /**
     * Start gameplay. This means updating some status variables and switching
     * to the "gameplay" screen (the screen where the user types the score they want).
     *
     * @param hardMode whether to start gameplay in "hard mode".
     */
    void startGame(boolean hardMode) {
        mHardMode = hardMode;
        switchToFragment(mGameplayFragment);
    }

    @Override
    public void onEnteredScore(int requestedScore) {
        // Compute final score (in easy mode, it's the requested score; in hard mode, it's half)
        int finalScore = mHardMode ? requestedScore / 2 : requestedScore;

        mWinFragment.setFinalScore(finalScore);
        mWinFragment.setExplanation(mHardMode ? getString(R.string.hard_mode_explanation) :
                getString(R.string.easy_mode_explanation));

        // check for achievements
        checkForAchievements(requestedScore, finalScore);

        // update leaderboards
        updateLeaderboards(finalScore);

        // push those accomplishments to the cloud, if signed in
        pushAccomplishments();

        // switch to the exciting "you won" screen
        switchToFragment(mWinFragment);
    }

    // Checks if n is prime. We don't consider 0 and 1 to be prime.
    // This is not an implementation we are mathematically proud of, but it gets the job done.
    boolean isPrime(int n) {
        int i;
        if (n == 0 || n == 1) return false;
        for (i = 2; i <= n / 2; i++) {
            if (n % i == 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check for achievements and unlock the appropriate ones.
     *
     * @param requestedScore the score the user requested.
     * @param finalScore the score the user got.
     */
    void checkForAchievements(int requestedScore, int finalScore) {
        // Check if each condition is met; if so, unlock the corresponding
        // achievement.
        if (isPrime(finalScore)) {
            mOutbox.mPrimeAchievement = true;
            achievementToast(getString(R.string.achievement_prime_toast_text));
        }
        if (requestedScore == 9999) {
            mOutbox.mArrogantAchievement = true;
            achievementToast(getString(R.string.achievement_arrogant_toast_text));
        }
        if (requestedScore == 0) {
            mOutbox.mHumbleAchievement = true;
            achievementToast(getString(R.string.achievement_humble_toast_text));
        }
        if (finalScore == 1337) {
            mOutbox.mLeetAchievement = true;
            achievementToast(getString(R.string.achievement_leet_toast_text));
        }
        mOutbox.mBoredSteps++;
    }

    void unlockAchievement(int achievementId, String fallbackString) {
        if (isSignedIn()) {
            Games.Achievements.unlock(mGoogleApiClient, getString(achievementId));
        } else {
            Toast.makeText(this, getString(R.string.achievement) + ": " + fallbackString,
                    Toast.LENGTH_LONG).show();
        }
    }

    void achievementToast(String achievement) {
        // Only show toast if not signed in. If signed in, the standard Google Play
        // toasts will appear, so we don't need to show our own.
        if (!isSignedIn()) {
            Toast.makeText(this, getString(R.string.achievement) + ": " + achievement,
                    Toast.LENGTH_LONG).show();
        }
    }

    void pushAccomplishments() {
        if (!isSignedIn()) {
            // can't push to the cloud, so save locally
            mOutbox.saveLocal(this);
            return;
        }
        if (mOutbox.mPrimeAchievement) {
            Games.Achievements.unlock(mGoogleApiClient, getString(R.string.achievement_prime));
            mOutbox.mPrimeAchievement = false;
        }
        if (mOutbox.mArrogantAchievement) {
            Games.Achievements.unlock(mGoogleApiClient, getString(R.string.achievement_arrogant));
            mOutbox.mArrogantAchievement = false;
        }
        if (mOutbox.mHumbleAchievement) {
            Games.Achievements.unlock(mGoogleApiClient, getString(R.string.achievement_humble));
            mOutbox.mHumbleAchievement = false;
        }
        if (mOutbox.mLeetAchievement) {
            Games.Achievements.unlock(mGoogleApiClient, getString(R.string.achievement_leet));
            mOutbox.mLeetAchievement = false;
        }
        if (mOutbox.mBoredSteps > 0) {
            Games.Achievements.increment(mGoogleApiClient, getString(R.string.achievement_really_bored),
                    mOutbox.mBoredSteps);
            Games.Achievements.increment(mGoogleApiClient, getString(R.string.achievement_bored),
                    mOutbox.mBoredSteps);
        }
        if (mOutbox.mEasyModeScore >= 0) {
            Games.Leaderboards.submitScore(mGoogleApiClient, getString(R.string.leaderboard_easy),
                    mOutbox.mEasyModeScore);
            mOutbox.mEasyModeScore = -1;
        }
        if (mOutbox.mHardModeScore >= 0) {
            Games.Leaderboards.submitScore(mGoogleApiClient, getString(R.string.leaderboard_hard),
                    mOutbox.mHardModeScore);
            mOutbox.mHardModeScore = -1;
        }
        mOutbox.saveLocal(this);
    }

    /**
     * Update leaderboards with the user's score.
     *
     * @param finalScore The score the user got.
     */
    void updateLeaderboards(int finalScore) {
        if (mHardMode && mOutbox.mHardModeScore < finalScore) {
            mOutbox.mHardModeScore = finalScore;
        } else if (!mHardMode && mOutbox.mEasyModeScore < finalScore) {
            mOutbox.mEasyModeScore = finalScore;
        }
    }

    @Override
    public void onWinScreenDismissed() {
        switchToFragment(mMainMenuFragment);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == RC_SIGN_IN) {
            mSignInClicked = false;
            mResolvingConnectionFailure = false;
            if (resultCode == RESULT_OK) {
                mGoogleApiClient.connect();
            } else {
                BaseGameUtils.showActivityResultError(this, requestCode, resultCode, R.string.signin_other_error);
            }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected(): connected to Google APIs");
        // Show sign-out button on main menu
        mMainMenuFragment.setShowSignInButton(false);

        // Show "you are signed in" message on win screen, with no sign in button.
        mWinFragment.setShowSignInButton(false);

        // Set the greeting appropriately on main menu
        Player p = Games.Players.getCurrentPlayer(mGoogleApiClient);
        String displayName;
        if (p == null) {
            Log.w(TAG, "mGamesClient.getCurrentPlayer() is NULL!");
            displayName = "???";
        } else {
            displayName = p.getDisplayName();
        }
        mMainMenuFragment.setGreeting("Hello, " + displayName);


        // if we have accomplishments to push, push them
        if (!mOutbox.isEmpty()) {
            pushAccomplishments();
            Toast.makeText(this, getString(R.string.your_progress_will_be_uploaded),
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended(): attempting to connect");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed(): attempting to resolve");
        if (mResolvingConnectionFailure) {
            Log.d(TAG, "onConnectionFailed(): already resolving");
            return;
        }

        if (mSignInClicked || mAutoStartSignInFlow) {
            mAutoStartSignInFlow = false;
            mSignInClicked = false;
            mResolvingConnectionFailure = true;
            if (!BaseGameUtils.resolveConnectionFailure(this, mGoogleApiClient, connectionResult,
                    RC_SIGN_IN, getString(R.string.signin_other_error))) {
                mResolvingConnectionFailure = false;
            }
        }

        // Sign-in failed, so show sign-in button on main menu
        mMainMenuFragment.setGreeting(getString(R.string.signed_out_greeting));
        mMainMenuFragment.setShowSignInButton(true);
        mWinFragment.setShowSignInButton(true);
    }

    @Override
    public void onSignInButtonClicked() {
        // Check to see the developer who's running this sample code read the instructions :-)
        // NOTE: this check is here only because this is a sample! Don't include this
        // check in your actual production app.
        if(!BaseGameUtils.verifySampleSetup(this, R.string.app_id,
                R.string.achievement_prime, R.string.leaderboard_easy)) {
            Log.w(TAG, "*** Warning: setup problems detected. Sign in may not work!");
        }

        // start the sign-in flow
        mSignInClicked = true;
        mGoogleApiClient.connect();
    }

    @Override
    public void onSignOutButtonClicked() {
        mSignInClicked = false;
        Games.signOut(mGoogleApiClient);
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }

        mMainMenuFragment.setGreeting(getString(R.string.signed_out_greeting));
        mMainMenuFragment.setShowSignInButton(true);
        mWinFragment.setShowSignInButton(true);
    }

    class AccomplishmentsOutbox {
        boolean mPrimeAchievement = false;
        boolean mHumbleAchievement = false;
        boolean mLeetAchievement = false;
        boolean mArrogantAchievement = false;
        int mBoredSteps = 0;
        int mEasyModeScore = -1;
        int mHardModeScore = -1;

        boolean isEmpty() {
            return !mPrimeAchievement && !mHumbleAchievement && !mLeetAchievement &&
                    !mArrogantAchievement && mBoredSteps == 0 && mEasyModeScore < 0 &&
                    mHardModeScore < 0;
        }

        public void saveLocal(Context ctx) {
            /* TODO: This is left as an exercise. To make it more difficult to cheat,
             * this data should be stored in an encrypted file! And remember not to
             * expose your encryption key (obfuscate it by building it from bits and
             * pieces and/or XORing with another string, for instance). */
        }

        public void loadLocal(Context ctx) {
            /* TODO: This is left as an exercise. Write code here that loads data
             * from the file you wrote in saveLocal(). */
        }
    }

    @Override
    public void onWinScreenSignInClicked() {
        mSignInClicked = true;
        mGoogleApiClient.connect();
    }
}
