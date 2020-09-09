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

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.games.AchievementsClient;
import com.google.android.gms.games.AnnotatedData;
import com.google.android.gms.games.EventsClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.LeaderboardsClient;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.PlayersClient;
import com.google.android.gms.games.event.Event;
import com.google.android.gms.games.event.EventBuffer;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

/**
 * Our main activity for the game.
 * <p>
 * IMPORTANT: Before attempting to run this sample, please change
 * the package name to your own package name (not com.android.*) and
 * replace the IDs on res/values/ids.xml by your own IDs (you must
 * create a game in the developer console to get those IDs).
 * <p>
 * This is a very simple game where the user selects "easy mode" or
 * "hard mode" and then the "gameplay" consists of inputting the
 * desired score (0 to 9999). In easy mode, you get the score you
 * request; in hard mode, you get half.
 *
 * @author Bruno Oliveira
 */
public class MainActivity extends FragmentActivity implements
    MainMenuFragment.Listener,
    GameplayFragment.Callback,
    WinFragment.Listener,
    FriendsFragment.Listener {

  // Fragments
  private MainMenuFragment mMainMenuFragment;
  private GameplayFragment mGameplayFragment;
  private WinFragment mWinFragment;
  public FriendsFragment mFriendsFragment;

  // Client used to sign in with Google APIs
  private GoogleSignInClient mGoogleSignInClient;

  // Client variables
  private AchievementsClient mAchievementsClient;
  private LeaderboardsClient mLeaderboardsClient;
  private EventsClient mEventsClient;
  private PlayersClient mPlayersClient;

  // request codes we use when invoking an external activity
  private static final int RC_UNUSED = 5001;
  private static final int RC_SIGN_IN = 9001;

  // tag for debug logging
  private static final String TAG = "TanC";

  // playing on hard mode?
  private boolean mHardMode = false;

  // The diplay name of the signed in user.
  private String mDisplayName = "";

  // achievements and scores we're pending to push to the cloud
  // (waiting for the user to sign in, for instance)
  private final AccomplishmentsOutbox mOutbox = new AccomplishmentsOutbox();

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_main);

    // Create the client used to sign in to Google services.
    mGoogleSignInClient = GoogleSignIn.getClient(this,
        new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN).build());

    // Create the fragments used by the UI.
    mMainMenuFragment = new MainMenuFragment();
    mGameplayFragment = new GameplayFragment();
    mWinFragment = new WinFragment();
    mFriendsFragment = new FriendsFragment();

    // Set the listeners and callbacks of fragment events.
    mMainMenuFragment.setListener(this);
    mGameplayFragment.setCallback(this);
    mWinFragment.setListener(this);
    mFriendsFragment.setListener(this);

    // Add initial Main Menu fragment.
    // IMPORTANT: if this Activity supported rotation, we'd have to be
    // more careful about adding the fragment, since the fragment would
    // already be there after rotation and trying to add it again would
    // result in overlapping fragments. But since we don't support rotation,
    // we don't deal with that for code simplicity.
    getSupportFragmentManager().beginTransaction().add(R.id.fragment_container,
        mMainMenuFragment).commit();

    checkPlaceholderIds();
  }

  // Check the sample to ensure all placeholder ids are are updated with real-world values.
  // This is strictly for the purpose of the samples; you don't need this in a production
  // application.
  private void checkPlaceholderIds() {
    StringBuilder problems = new StringBuilder();

    if (getPackageName().startsWith("com.google.")) {
      problems.append("- Package name start with com.google.*\n");
    }

    for (Integer id : new Integer[]{
        R.string.app_id,
        R.string.achievement_prime,
        R.string.achievement_really_bored,
        R.string.achievement_bored,
        R.string.achievement_humble,
        R.string.achievement_arrogant,
        R.string.achievement_leet,
        R.string.leaderboard_easy,
        R.string.leaderboard_hard,
        R.string.event_start,
        R.string.event_number_chosen,}) {

      String value = getString(id);

      if (value.startsWith("YOUR_")) {
        // needs replacing
        problems.append("- Placeholders(YOUR_*) in ids.xml need updating\n");
        break;
      }
    }

    if (problems.length() > 0) {
      problems.insert(0, "The following problems were found:\n\n");

      problems.append("\nThese problems may prevent the app from working properly.");
      problems.append("\n\nSee the TODO window in Android Studio for more information");
      (new AlertDialog.Builder(this)).setMessage(problems.toString())
          .setNeutralButton(android.R.string.ok, null).create().show();
    }
  }

  private void loadAndPrintEvents() {

    final MainActivity mainActivity = this;

    mEventsClient.load(true)
        .addOnSuccessListener(new OnSuccessListener<AnnotatedData<EventBuffer>>() {
          @Override
          public void onSuccess(AnnotatedData<EventBuffer> eventBufferAnnotatedData) {
            EventBuffer eventBuffer = eventBufferAnnotatedData.get();

            int count = 0;
            if (eventBuffer != null) {
              count = eventBuffer.getCount();
            }

            Log.i(TAG, "number of events: " + count);

            for (int i = 0; i < count; i++) {
              Event event = eventBuffer.get(i);
              Log.i(TAG, "event: "
                  + event.getName()
                  + " -> "
                  + event.getValue());
            }
          }
        })
        .addOnFailureListener(new OnFailureListener() {
          @Override
          public void onFailure(@NonNull Exception e) {
            handleException(e, getString(R.string.achievements_exception));
          }
        });
  }

  // Switch UI to the given fragment
  private void switchToFragment(Fragment newFrag) {
    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, newFrag)
        .commit();
  }

  private boolean isSignedIn() {
    return GoogleSignIn.getLastSignedInAccount(this) != null;
  }

  private void signInSilently() {
    Log.d(TAG, "signInSilently()");

    mGoogleSignInClient.silentSignIn().addOnCompleteListener(this,
        new OnCompleteListener<GoogleSignInAccount>() {
          @Override
          public void onComplete(@NonNull Task<GoogleSignInAccount> task) {
            if (task.isSuccessful()) {
              Log.d(TAG, "signInSilently(): success");
              onConnected(task.getResult());
            } else {
              Log.d(TAG, "signInSilently(): failure", task.getException());
              onDisconnected();
            }
          }
        });
  }

  private void startSignInIntent() {
    startActivityForResult(mGoogleSignInClient.getSignInIntent(), RC_SIGN_IN);
  }

  @Override
  protected void onResume() {
    super.onResume();
    Log.d(TAG, "onResume()");

    // Since the state of the signed in user can change when the activity is not active
    // it is recommended to try and sign in silently from when the app resumes.
    signInSilently();
  }

  private void signOut() {
    Log.d(TAG, "signOut()");

    if (!isSignedIn()) {
      Log.w(TAG, "signOut() called, but was not signed in!");
      return;
    }

    mGoogleSignInClient.signOut().addOnCompleteListener(this,
        new OnCompleteListener<Void>() {
          @Override
          public void onComplete(@NonNull Task<Void> task) {
            boolean successful = task.isSuccessful();
            Log.d(TAG, "signOut(): " + (successful ? "success" : "failed"));

            onDisconnected();
          }
        });
  }

  @Override
  public void onStartGameRequested(boolean hardMode) {
    startGame(hardMode);
  }

  @Override
  public void onShowAchievementsRequested() {
    mAchievementsClient.getAchievementsIntent()
        .addOnSuccessListener(new OnSuccessListener<Intent>() {
          @Override
          public void onSuccess(Intent intent) {
            startActivityForResult(intent, RC_UNUSED);
          }
        })
        .addOnFailureListener(new OnFailureListener() {
          @Override
          public void onFailure(@NonNull Exception e) {
            handleException(e, getString(R.string.achievements_exception));
          }
        });
  }

  @Override
  public void onShowLeaderboardsRequested() {
    mLeaderboardsClient.getAllLeaderboardsIntent()
        .addOnSuccessListener(new OnSuccessListener<Intent>() {
          @Override
          public void onSuccess(Intent intent) {
            startActivityForResult(intent, RC_UNUSED);
          }
        })
        .addOnFailureListener(new OnFailureListener() {
          @Override
          public void onFailure(@NonNull Exception e) {
            handleException(e, getString(R.string.leaderboards_exception));
          }
        });
  }

  private void handleException(Exception e, String details) {
    int status = 0;

    if (e instanceof ApiException) {
      ApiException apiException = (ApiException) e;
      status = apiException.getStatusCode();
    }

    String message = getString(R.string.status_exception_error, details, status, e);

    new AlertDialog.Builder(MainActivity.this)
        .setMessage(message)
        .setNeutralButton(android.R.string.ok, null)
        .show();
  }

  /**
   * Start gameplay. This means updating some status variables and switching
   * to the "gameplay" screen (the screen where the user types the score they want).
   *
   * @param hardMode whether to start gameplay in "hard mode".
   */
  private void startGame(boolean hardMode) {
    mHardMode = hardMode;
    switchToFragment(mGameplayFragment);
    mEventsClient.increment(getString(R.string.event_start), 1);
  }

  @Override
  public void onEnteredScore(int requestedScore) {
    // Compute final score (in easy mode, it's the requested score; in hard mode, it's half)
    int finalScore = mHardMode ? requestedScore / 2 : requestedScore;

    mWinFragment.setScore(finalScore);
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

    mEventsClient.increment(getString(R.string.event_number_chosen), 1);
  }

  // Checks if n is prime. We don't consider 0 and 1 to be prime.
  // This is not an implementation we are mathematically proud of, but it gets the job done.
  private boolean isPrime(int n) {
    int i;
    if (n == 0 || n == 1) {
      return false;
    }
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
   * @param finalScore     the score the user got.
   */
  private void checkForAchievements(int requestedScore, int finalScore) {
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

  private void achievementToast(String achievement) {
    // Only show toast if not signed in. If signed in, the standard Google Play
    // toasts will appear, so we don't need to show our own.
    if (!isSignedIn()) {
      Toast.makeText(this, getString(R.string.achievement) + ": " + achievement,
          Toast.LENGTH_LONG).show();
    }
  }

  private void pushAccomplishments() {
    if (!isSignedIn()) {
      // can't push to the cloud, try again later
      return;
    }
    if (mOutbox.mPrimeAchievement) {
      mAchievementsClient.unlock(getString(R.string.achievement_prime));
      mOutbox.mPrimeAchievement = false;
    }
    if (mOutbox.mArrogantAchievement) {
      mAchievementsClient.unlock(getString(R.string.achievement_arrogant));
      mOutbox.mArrogantAchievement = false;
    }
    if (mOutbox.mHumbleAchievement) {
      mAchievementsClient.unlock(getString(R.string.achievement_humble));
      mOutbox.mHumbleAchievement = false;
    }
    if (mOutbox.mLeetAchievement) {
      mAchievementsClient.unlock(getString(R.string.achievement_leet));
      mOutbox.mLeetAchievement = false;
    }
    if (mOutbox.mBoredSteps > 0) {
      mAchievementsClient.increment(getString(R.string.achievement_really_bored),
          mOutbox.mBoredSteps);
      mAchievementsClient.increment(getString(R.string.achievement_bored),
          mOutbox.mBoredSteps);
      mOutbox.mBoredSteps = 0;
    }
    if (mOutbox.mEasyModeScore >= 0) {
      mLeaderboardsClient.submitScore(getString(R.string.leaderboard_easy),
          mOutbox.mEasyModeScore);
      mOutbox.mEasyModeScore = -1;
    }
    if (mOutbox.mHardModeScore >= 0) {
      mLeaderboardsClient.submitScore(getString(R.string.leaderboard_hard),
          mOutbox.mHardModeScore);
      mOutbox.mHardModeScore = -1;
    }
  }

  public PlayersClient getPlayersClient() {
    return mPlayersClient;
  }

  public String getDisplayName() {
    return mDisplayName;
  }

  /**
   * Update leaderboards with the user's score.
   *
   * @param finalScore The score the user got.
   */
  private void updateLeaderboards(int finalScore) {
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
      Task<GoogleSignInAccount> task =
          GoogleSignIn.getSignedInAccountFromIntent(intent);

      try {
        GoogleSignInAccount account = task.getResult(ApiException.class);
        onConnected(account);
      } catch (ApiException apiException) {
        String message = apiException.getMessage();
        if (message == null || message.isEmpty()) {
          message = getString(R.string.signin_other_error);
        }

        onDisconnected();

        new AlertDialog.Builder(this)
            .setMessage(message)
            .setNeutralButton(android.R.string.ok, null)
            .show();
      }
    }
  }

  private void onConnected(GoogleSignInAccount googleSignInAccount) {
    Log.d(TAG, "onConnected(): connected to Google APIs");

    mAchievementsClient = Games.getAchievementsClient(this, googleSignInAccount);
    mLeaderboardsClient = Games.getLeaderboardsClient(this, googleSignInAccount);
    mEventsClient = Games.getEventsClient(this, googleSignInAccount);
    mPlayersClient = Games.getPlayersClient(this, googleSignInAccount);

    // Show sign-out button on main menu
    mMainMenuFragment.setShowSignInButton(false);

    // Show "you are signed in" message on win screen, with no sign in button.
    mWinFragment.setShowSignInButton(false);

    // Set the greeting appropriately on main menu
    mPlayersClient.getCurrentPlayer()
        .addOnCompleteListener(new OnCompleteListener<Player>() {
          @Override
          public void onComplete(@NonNull Task<Player> task) {
            String displayName;
            if (task.isSuccessful()) {
              displayName = task.getResult().getDisplayName();
            } else {
              Exception e = task.getException();
              handleException(e, getString(R.string.players_exception));
              displayName = "???";
            }
            mDisplayName = displayName;
            mMainMenuFragment.setGreeting("Hello, " + displayName);
          }
        });


    // if we have accomplishments to push, push them
    if (!mOutbox.isEmpty()) {
      pushAccomplishments();
      Toast.makeText(this, getString(R.string.your_progress_will_be_uploaded),
          Toast.LENGTH_LONG).show();
    }

    loadAndPrintEvents();
  }

  private void onDisconnected() {
    Log.d(TAG, "onDisconnected()");

    mAchievementsClient = null;
    mLeaderboardsClient = null;
    mPlayersClient = null;

    // Show sign-in button on main menu
    mMainMenuFragment.setShowSignInButton(true);

    // Show sign-in button on win screen
    mWinFragment.setShowSignInButton(true);

    mMainMenuFragment.setGreeting(getString(R.string.signed_out_greeting));
  }

  @Override
  public void onSignInButtonClicked() {
    startSignInIntent();
  }

  @Override
  public void onSignOutButtonClicked() {
    signOut();
  }

  @Override
  public void onShowFriendsButtonClicked() {
    switchToFragment(mFriendsFragment);
  }

  @Override
  public void onBackButtonClicked() {
    switchToFragment(mMainMenuFragment);
  }

  private class AccomplishmentsOutbox {
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

  }
}
