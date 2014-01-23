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

package com.google.example.eightbitartist;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.View;
import android.widget.TextView;

// StateManager keeps track of the state of the interface and manages
// the transitions between them. 
public class StateManager {

    public static final int STATE_SIGNED_OUT = -1;

    // Need guessing state.
    public static final int STATE_MATCHUP = 1;
    public static final int STATE_GAME_START = 2;
    public static final int STATE_REPLAY_METADATA = 3;
    public static final int STATE_REPLAY = 4;
    public static final int STATE_GUESSING_METADATA = 5;
    public static final int STATE_GUESSING = 6;
    public static final int STATE_NEW_TURN_METADATA = 7;
    public static final int STATE_NEW_TURN = 8;
    public static final int STATE_SENDING = 9;

    public static final int ACTION_REPLAY = 0;
    public static final int ACTION_GUESS = 1;
    public static final int ACTION_SEND = 2;
    public static final int ACTION_DRAW = 3;
    public static final int ACTION_CLEAR = 4;

    public static final long mShortAnimationDuration = 500;

    public int state = STATE_SIGNED_OUT;

    public DrawingActivity mActivity;

    public int getState() {
        return state;
    }

    public void fadeIn(View theNew) {
        fadeIn(theNew, -1);
    }

    public void fadeIn(View theNew, int theAction) {
        final View newView = theNew;
        final int action = theAction;

        newView.setAlpha(0f);
        newView.setVisibility(View.VISIBLE);

        // Animate the content view to 100% opacity, and clear any animation
        // listener set on the view.
        newView.animate().alpha(1f).setDuration(mShortAnimationDuration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (action == ACTION_REPLAY) {
                            mActivity.beginOldTurnPlayback();
                        } else if (action == ACTION_SEND) {
                            mActivity.beginSend();
                        } else if (action == ACTION_GUESS) {
                            mActivity.beginGuessingTurnPlayback();
                        } else if (action == ACTION_DRAW) {
                            mActivity.beginArtistTurn();
                        } else if (action == ACTION_CLEAR) {
                            mActivity.mDrawView.clear(false);
                        }
                    }
                });
    }

    public void squish(View theOld) {
        final View oldView = theOld;

        // Animate the loading view to 0% opacity. After the animation ends,
        // set its visibility to GONE as an optimization step (it won't
        // participate in layout passes, etc.)
        oldView.animate().alpha(0f).setDuration(mShortAnimationDuration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        oldView.setVisibility(View.GONE);
                        oldView.setAlpha(1.0f);
                    }
                });
    }

    public void transitionState(int newState) {

        switch (newState) {
            case STATE_MATCHUP:
                squish(mActivity.findViewById(R.id.login_layout));
                squish(mActivity.findViewById(R.id.gameplay_layout));
                squish(mActivity.findViewById(R.id.metadata_layout));
                fadeIn(mActivity.findViewById(R.id.matchup_layout), ACTION_CLEAR);
                mActivity.refreshTurnsPending();
                break;
            case STATE_SIGNED_OUT:
                fadeIn(mActivity.findViewById(R.id.login_layout));
                mActivity.findViewById(R.id.sign_in_button).setVisibility(
                        View.VISIBLE);

                squish(mActivity.findViewById(R.id.gameplay_layout));
                squish(mActivity.findViewById(R.id.metadata_layout));
                squish(mActivity.findViewById(R.id.matchup_layout));
                break;
            case STATE_GAME_START:
                squish(mActivity.findViewById(R.id.login_layout));
                fadeIn(mActivity.findViewById(R.id.gameplay_layout), ACTION_DRAW);
                squish(mActivity.findViewById(R.id.metadata_layout));
                squish(mActivity.findViewById(R.id.matchup_layout));
                break;
            case STATE_REPLAY_METADATA:
                squish(mActivity.findViewById(R.id.login_layout));
                squish(mActivity.findViewById(R.id.matchup_layout));
                squish(mActivity.findViewById(R.id.gameplay_layout));
                fadeIn(mActivity.findViewById(R.id.metadata_layout), ACTION_CLEAR);
                mActivity.findViewById(R.id.continue_button).setVisibility(
                        View.VISIBLE);
                ((TextView) mActivity.findViewById(R.id.metadataText))
                        .setText("Time to watch your opponent guess on turn "
                                + mActivity.mTurnData.replayTurn.turnNumber
                                + ".");

                mActivity.mMyRole = DrawingActivity.ROLE_NOTHING;
                mActivity.mInstructionsView.setText("Watch your opponent guess.");
                break;
            case STATE_REPLAY:
                squish(mActivity.findViewById(R.id.login_layout));
                fadeIn(mActivity.findViewById(R.id.gameplay_layout), ACTION_REPLAY);
                squish(mActivity.findViewById(R.id.matchup_layout));
                squish(mActivity.findViewById(R.id.metadata_layout));
                mActivity.setReplayUI();

                break;
            case STATE_GUESSING_METADATA:
                squish(mActivity.findViewById(R.id.login_layout));
                squish(mActivity.findViewById(R.id.gameplay_layout));
                squish(mActivity.findViewById(R.id.matchup_layout));
                fadeIn(mActivity.findViewById(R.id.metadata_layout), ACTION_CLEAR);
                mActivity.findViewById(R.id.continue_button).setVisibility(
                        View.VISIBLE);
                ((TextView) mActivity.findViewById(R.id.metadataText))
                        .setText("Now, it's your turn to guess on turn "
                                + mActivity.mTurnData.guessingTurn.turnNumber
                                + ".");
                mActivity.mInstructionsView.setText("Touch a word to guess.");

                mActivity.mMyRole = DrawingActivity.ROLE_GUESSER;
                break;
            case STATE_GUESSING:
                squish(mActivity.findViewById(R.id.login_layout));
                fadeIn(mActivity.findViewById(R.id.gameplay_layout), ACTION_GUESS);
                squish(mActivity.findViewById(R.id.matchup_layout));
                squish(mActivity.findViewById(R.id.metadata_layout));
                mActivity.setGuessingUI();

                break;
            case STATE_NEW_TURN_METADATA:
                squish(mActivity.findViewById(R.id.login_layout));
                squish(mActivity.findViewById(R.id.gameplay_layout));
                squish(mActivity.findViewById(R.id.matchup_layout));
                fadeIn(mActivity.findViewById(R.id.metadata_layout), ACTION_CLEAR);
                mActivity.findViewById(R.id.continue_button).setVisibility(
                        View.VISIBLE);

                ((TextView) mActivity.findViewById(R.id.metadataText))
                        .setText("Time for you to draw on turn "
                                + mActivity.mTurnData.artistTurn.turnNumber
                                + ".");
                mActivity.mInstructionsView.setText("Draw the word on the right.");

                mActivity.mMyRole = DrawingActivity.ROLE_ARTIST;

                break;
            case STATE_NEW_TURN:
                squish(mActivity.findViewById(R.id.login_layout));
                fadeIn(mActivity.findViewById(R.id.gameplay_layout), ACTION_DRAW);
                squish(mActivity.findViewById(R.id.metadata_layout));
                squish(mActivity.findViewById(R.id.matchup_layout));
                mActivity.setArtistUI();
                break;
            case STATE_SENDING:
                squish(mActivity.findViewById(R.id.login_layout));
                squish(mActivity.findViewById(R.id.gameplay_layout));
                squish(mActivity.findViewById(R.id.matchup_layout));
                fadeIn(mActivity.findViewById(R.id.metadata_layout), ACTION_SEND);
                mActivity.findViewById(R.id.continue_button).setVisibility(
                        View.INVISIBLE);

                ((TextView) mActivity.findViewById(R.id.metadataText))
                        .setText("Sending turn: "
                                + mActivity.mTurnData.artistTurn.turnNumber);

                break;
        }

        state = newState;
    }

    // Factory for creating singleton/
    public static StateManager init(DrawingActivity activity) {
        StateManager stateManager = new StateManager();
        stateManager.mActivity = activity;

        return stateManager;
    }

}
