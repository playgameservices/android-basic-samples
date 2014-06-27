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
 *
 */
public class WinFragment extends Fragment implements OnClickListener {
    String mExplanation = "";
    int mScore = 0;
    boolean mShowSignIn = false;

    public interface Listener {
        public void onWinScreenDismissed();
        public void onWinScreenSignInClicked();
    }

    Listener mListener = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_win, container, false);
        v.findViewById(R.id.win_ok_button).setOnClickListener(this);
        v.findViewById(R.id.win_screen_sign_in_button).setOnClickListener(this);
        return v;
    }

    public void setFinalScore(int i) {
        mScore = i;
    }

    public void setExplanation(String s) {
        mExplanation = s;
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
        TextView scoreTv = (TextView) getActivity().findViewById(R.id.score_display);
        TextView explainTv = (TextView) getActivity().findViewById(R.id.scoreblurb);

        if (scoreTv != null) scoreTv.setText(String.valueOf(mScore));
        if (explainTv != null) explainTv.setText(mExplanation);

        getActivity().findViewById(R.id.win_screen_sign_in_bar).setVisibility(
                mShowSignIn ? View.VISIBLE : View.GONE);
        getActivity().findViewById(R.id.win_screen_signed_in_bar).setVisibility(
                mShowSignIn ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.win_screen_sign_in_button) {
            mListener.onWinScreenSignInClicked();
        }
        mListener.onWinScreenDismissed();
    }

    public void setShowSignInButton(boolean showSignIn) {
        mShowSignIn = showSignIn;
        updateUi();
    }
}
