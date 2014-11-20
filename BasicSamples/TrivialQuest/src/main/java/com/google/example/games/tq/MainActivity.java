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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.plus.Plus;
import com.google.example.games.basegameutils.BaseGameUtils;

/**
 * Trivial quest. A sample game that sets up the Google Play game services
 * API and allows the user to click a button to win (yes, incredibly epic).
 * Even though winning the game is fun, the purpose of this sample is to
 * illustrate the simplest possible game that uses the API.
 *
 * @author Bruno Oliveira (Google)
 */
public class MainActivity extends Activity
    implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
    View.OnClickListener {

  private static final String TAG = "TrivialQuest";

  // Request code used to invoke sign in user interactions.
  private static final int RC_SIGN_IN = 9001;

  // Client used to interact with Google APIs.
  private GoogleApiClient mGoogleApiClient;

  // Are we currently resolving a connection failure?
  private boolean mResolvingConnectionFailure = false;

  // Has the user clicked the sign-in button?
  private boolean mSignInClicked = false;

  // Set to true to automatically start the sign in flow when the Activity starts.
  // Set to false to require the user to click the button in order to sign in.
  private boolean mAutoStartSignInFlow = true;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Log.d(TAG, "onCreate()");

    super.onCreate(savedInstanceState);

    // Create the Google Api Client with access to Plus and Games
    mGoogleApiClient = new GoogleApiClient.Builder(this)
        .addConnectionCallbacks(this)
        .addOnConnectionFailedListener(this)
        .addApi(Plus.API).addScope(Plus.SCOPE_PLUS_LOGIN)
        .addApi(Games.API).addScope(Games.SCOPE_GAMES)
        .build();

    setContentView(R.layout.activity_main);

    // set this class to listen for the button clicks
    findViewById(R.id.button_sign_in).setOnClickListener(this);
    findViewById(R.id.button_sign_out).setOnClickListener(this);
    findViewById(R.id.button_win).setOnClickListener(this);
  }

  protected void onStart() {
    Log.d(TAG, "onStart()");
    super.onStart();
    mGoogleApiClient.connect();
  }

  protected void onStop() {
    Log.d(TAG, "onStop()");
    super.onStop();
    if (mGoogleApiClient.isConnected()) {
      mGoogleApiClient.disconnect();
    }
  }

  // Shows the "sign in" bar (explanation and button).
  private void showSignInBar() {
    Log.d(TAG, "Showing sign in bar");
    findViewById(R.id.sign_in_bar).setVisibility(View.VISIBLE);
    findViewById(R.id.sign_out_bar).setVisibility(View.GONE);
  }

  // Shows the "sign out" bar (explanation and button).
  private void showSignOutBar() {
    Log.d(TAG, "Showing sign out bar");
    findViewById(R.id.sign_in_bar).setVisibility(View.GONE);
    findViewById(R.id.sign_out_bar).setVisibility(View.VISIBLE);
  }

  @Override
  public void onClick(View view) {
    switch (view.getId()) {
      case R.id.button_sign_in:
        // Check to see the developer who's running this sample code read the instructions :-)
        // NOTE: this check is here only because this is a sample! Don't include this
        // check in your actual production app.
        if (!BaseGameUtils.verifySampleSetup(this, R.string.app_id,
            R.string.trivial_victory_achievement_id)) {
          Log.w(TAG, "*** Warning: setup problems detected. Sign in may not work!");
        }

        // start the sign-in flow
        Log.d(TAG, "Sign-in button clicked");
        mSignInClicked = true;
        mGoogleApiClient.connect();
        break;
      case R.id.button_sign_out:
        // sign out.
        Log.d(TAG, "Sign-out button clicked");
        mSignInClicked = false;
        Games.signOut(mGoogleApiClient);
        mGoogleApiClient.disconnect();
        showSignInBar();
        break;
      case R.id.button_win:
        // win!
        Log.d(TAG, "Win button clicked");
        BaseGameUtils.showAlert(this, getString(R.string.you_won));
        if (mGoogleApiClient.isConnected()) {
          // unlock the "Trivial Victory" achievement.
          Games.Achievements.unlock(mGoogleApiClient,
              getString(R.string.trivial_victory_achievement_id));
        }
        break;
    }
  }

  @Override
  public void onConnected(Bundle bundle) {
    Log.d(TAG, "onConnected() called. Sign in successful!");
    showSignOutBar();
  }

  @Override
  public void onConnectionSuspended(int i) {
    Log.d(TAG, "onConnectionSuspended() called. Trying to reconnect.");
    mGoogleApiClient.connect();
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
      mResolvingConnectionFailure = BaseGameUtils.resolveConnectionFailure(this, mGoogleApiClient,
          connectionResult, RC_SIGN_IN, getString(R.string.signin_other_error));
    }
    showSignInBar();
  }

  protected void onActivityResult(int requestCode, int responseCode, Intent intent) {
    if (requestCode == RC_SIGN_IN) {
      Log.d(TAG, "onActivityResult with requestCode == RC_SIGN_IN, responseCode="
          + responseCode + ", intent=" + intent);
      mSignInClicked = false;
      mResolvingConnectionFailure = false;
      if (responseCode == RESULT_OK) {
        mGoogleApiClient.connect();
      } else {
        BaseGameUtils.showActivityResultError(this,requestCode,responseCode,
            R.string.signin_failure, R.string.signin_other_error);
      }
    }
  }
}