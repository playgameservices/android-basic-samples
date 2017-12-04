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

package com.google.example.games.tanc;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Fragment with the main menu for the game. The main menu allows the player
 * to choose a gameplay mode (Easy or Hard), and click the buttons to
 * show view achievements/leaderboards.
 *
 * @author Bruno Oliveira (Google)
 */
public class MainMenuFragment extends Fragment implements OnClickListener {
  private String mGreeting = "Hello, anonymous user (not signed in)";
  private TextView mGreetingTextView;
  private View mSignInBarView;
  private View mSignOutBarView;
  private View mShowAchievementsButton;
  private View mShowLeaderboardsButton;

  interface Listener {
    // called when the user presses the `Easy` or `Okay` button; will pass in which via `hardMode`
    void onStartGameRequested(boolean hardMode);

    // called when the user presses the `Show Achievements` button
    void onShowAchievementsRequested();

    // called when the user presses the `Show Leaderboards` button
    void onShowLeaderboardsRequested();

    // called when the user presses the `Sign In` button
    void onSignInButtonClicked();

    // called when the user presses the `Sign Out` button
    void onSignOutButtonClicked();
  }

  private Listener mListener = null;
  private boolean mShowSignInButton = true;

  @Override
  public View onCreateView(LayoutInflater inflater,
                           ViewGroup container,
                           Bundle savedInstanceState) {

    View view = inflater.inflate(R.layout.fragment_mainmenu, container, false);

    final int[] clickableIds = new int[]{
        R.id.easy_mode_button,
        R.id.hard_mode_button,
        R.id.show_achievements_button,
        R.id.show_leaderboards_button,
        R.id.sign_in_button,
        R.id.sign_out_button
    };

    for (int clickableId : clickableIds) {
      view.findViewById(clickableId).setOnClickListener(this);
    }

    // cache views
    mShowAchievementsButton = view.findViewById(R.id.show_achievements_button);
    mShowLeaderboardsButton = view.findViewById(R.id.show_leaderboards_button);

    mGreetingTextView = view.findViewById(R.id.text_greeting);
    mSignInBarView = view.findViewById(R.id.sign_in_bar);
    mSignOutBarView = view.findViewById(R.id.sign_out_bar);

    updateUI();

    return view;
  }

  public void setListener(Listener listener) {
    mListener = listener;
  }

  public void setGreeting(String greeting) {
    mGreeting = greeting;
    updateUI();
  }

  private void updateUI() {
    mGreetingTextView.setText(mGreeting);
    mShowAchievementsButton.setEnabled(!mShowSignInButton);
    mShowLeaderboardsButton.setEnabled(!mShowSignInButton);
    mSignInBarView.setVisibility(mShowSignInButton ? View.VISIBLE : View.GONE);
    mSignOutBarView.setVisibility(mShowSignInButton ? View.GONE : View.VISIBLE);
  }

  @Override
  public void onClick(View view) {
    switch (view.getId()) {
      case R.id.easy_mode_button:
        mListener.onStartGameRequested(false);
        break;
      case R.id.hard_mode_button:
        mListener.onStartGameRequested(true);
        break;
      case R.id.show_achievements_button:
        mListener.onShowAchievementsRequested();
        break;
      case R.id.show_leaderboards_button:
        mListener.onShowLeaderboardsRequested();
        break;
      case R.id.sign_in_button:
        mListener.onSignInButtonClicked();
        break;
      case R.id.sign_out_button:
        mListener.onSignOutButtonClicked();
        break;
    }
  }

  public void setShowSignInButton(boolean showSignInButton) {
    mShowSignInButton = showSignInButton;
    updateUI();
  }
}
