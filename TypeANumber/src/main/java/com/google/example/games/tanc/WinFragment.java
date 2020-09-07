/*
 * Copyright (C) 2013 Google Inc.
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
 * Fragment that shows the 'You won' message. Apart from congratulating the user
 * on their heroic number typing deeds, this screen also allows the player to sign
 * in if they are not signed in yet.
 *
 * @author Bruno Oliveira (Google)
 */
public class WinFragment extends Fragment implements OnClickListener {
  private String mExplanation = "";
  private int mScore = 0;
  private boolean mShowSignIn = false;

  // cached views
  private TextView mScoreTextView;
  private TextView mExplanationTextView;
  private View mSignInBar;
  private View mSignedInBar;
  private View mView;

  interface Listener {
    // called when the user presses the `Ok` button
    void onWinScreenDismissed();

    // called when the user presses the `Sign In` button
    void onSignInButtonClicked();
  }

  private Listener mListener = null;

  @Override
  public View onCreateView(LayoutInflater inflater,
                           ViewGroup container,
                           Bundle savedInstanceState) {

    mView = inflater.inflate(R.layout.fragment_win, container, false);

    final int[] clickableIds = {
        R.id.win_ok_button,
        R.id.win_screen_sign_in_button
    };

    for (int clickableId : clickableIds) {
      mView.findViewById(clickableId).setOnClickListener(this);
    }

    // cache views
    mScoreTextView = mView.findViewById(R.id.text_win_score);
    mExplanationTextView = mView.findViewById(R.id.text_explanation);
    mSignInBar = mView.findViewById(R.id.win_sign_in_bar);
    mSignedInBar = mView.findViewById(R.id.signed_in_bar);

    updateUI();

    return mView;
  }

  public void setScore(int score) {
    mScore = score;
    updateUI();
  }

  public void setExplanation(String explanation) {
    mExplanation = explanation;
    updateUI();
  }

  public void setListener(Listener listener) {
    mListener = listener;
  }

  private void updateUI() {
    if (mView == null) {
      // view has not been created yet, do not do anything
      return;
    }

    mScoreTextView.setText(String.valueOf(mScore));
    mExplanationTextView.setText(mExplanation);

    mSignInBar.setVisibility(mShowSignIn ? View.VISIBLE : View.GONE);
    mSignedInBar.setVisibility(mShowSignIn ? View.GONE : View.VISIBLE);
  }

  @Override
  public void onClick(View view) {
    switch (view.getId()) {
      case R.id.win_screen_sign_in_button:
        mListener.onSignInButtonClicked();
        break;
      case R.id.win_ok_button:
        mListener.onWinScreenDismissed();
        break;
    }
  }

  public void setShowSignInButton(boolean showSignIn) {
    mShowSignIn = showSignIn;
    updateUI();
  }
}
