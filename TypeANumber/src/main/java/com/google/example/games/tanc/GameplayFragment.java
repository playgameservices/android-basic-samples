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
import android.widget.Button;
import android.widget.TextView;

import java.util.Locale;

/**
 * Fragment for the gameplay portion of the game. It shows the keypad
 * where the user can request their score.
 *
 * @author Bruno Oliveira (Google)
 */
public class GameplayFragment extends Fragment implements OnClickListener {
  private int mRequestedScore = 5000;

  private TextView mScoreTextView;

  interface Callback {
    // called when the user presses the okay button to submit a score
    void onEnteredScore(int score);
  }

  private Callback mCallback = null;

  @Override
  public View onCreateView(LayoutInflater inflater,
                           ViewGroup container,
                           Bundle savedInstanceState) {

    View view = inflater.inflate(R.layout.fragment_gameplay, container, false);

    final int[] clickableIds = {
        R.id.digit_button_0,
        R.id.digit_button_1,
        R.id.digit_button_2,
        R.id.digit_button_3,
        R.id.digit_button_4,
        R.id.digit_button_5,
        R.id.digit_button_6,
        R.id.digit_button_7,
        R.id.digit_button_8,
        R.id.digit_button_9,
        R.id.digit_button_clear,
        R.id.ok_score_button
    };

    for (int clickableId : clickableIds) {
      view.findViewById(clickableId).setOnClickListener(this);
    }

    // cache views
    mScoreTextView = view.findViewById(R.id.text_gameplay_score);

    updateUI();

    return view;
  }

  public void setCallback(Callback callback) {
    mCallback = callback;
  }

  private void setScore(int score) {
    mRequestedScore = score;
    updateUI();
  }

  private void updateUI() {
    mScoreTextView.setText(String.format(Locale.getDefault(), "%04d", mRequestedScore));
  }

  @Override
  public void onClick(View view) {
    switch (view.getId()) {
      case R.id.digit_button_clear:
        setScore(0);
        break;
      case R.id.digit_button_0:
      case R.id.digit_button_1:
      case R.id.digit_button_2:
      case R.id.digit_button_3:
      case R.id.digit_button_4:
      case R.id.digit_button_5:
      case R.id.digit_button_6:
      case R.id.digit_button_7:
      case R.id.digit_button_8:
      case R.id.digit_button_9:
        int x = Integer.parseInt(((Button) view).getText().toString().trim());
        setScore((mRequestedScore * 10 + x) % 10000);
        break;
      case R.id.ok_score_button:
        mCallback.onEnteredScore(mRequestedScore);
        break;
    }
  }
}
