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

/**
 * Fragment for the gameplay portion of the game. It shows the keypad
 * where the user can request their score.
 *
 * @author Bruno Oliveira (Google)
 *
 */
public class GameplayFragment extends Fragment implements OnClickListener {
    int mRequestedScore = 5000;

    static int[] MY_BUTTONS = {
        R.id.digit_button_0, R.id.digit_button_1, R.id.digit_button_2,
        R.id.digit_button_3, R.id.digit_button_4, R.id.digit_button_5,
        R.id.digit_button_6, R.id.digit_button_7, R.id.digit_button_8,
        R.id.digit_button_9, R.id.digit_button_clear, R.id.ok_score_button
    };

    public interface Listener {
        public void onEnteredScore(int score);
    }

    Listener mListener = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_gameplay, container, false);
        for (int i : MY_BUTTONS) {
            ((Button) v.findViewById(i)).setOnClickListener(this);
        }
        return v;
    }

    public void setListener(Listener l) {
        mListener = l;
    }

    @Override
    public void onStart() {
        super.onStart();
        updateUi();
    }

    void updateUi() {
        if (getActivity() == null) return;
        TextView scoreInput = ((TextView) getActivity().findViewById(R.id.score_input));
        if (scoreInput != null) scoreInput.setText(String.format("%04d", mRequestedScore));
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
        case R.id.digit_button_clear:
            mRequestedScore = 0;
            updateUi();
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
            int x = Integer.parseInt(((Button)view).getText().toString().trim());
            mRequestedScore = (mRequestedScore * 10 + x) % 10000;
            updateUi();
            break;
        case R.id.ok_score_button:
            mListener.onEnteredScore(mRequestedScore);
            break;
        }
    }
}
