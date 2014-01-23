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

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Handler;
import android.util.Log;

// This represents a single drawing, guessable words, your guess, and so forth.
public class TurnSegment {

    public static final String TAG = "TURN";

    public ArrayList<Stroke> strokes;
    public int wordIndex;
    public ArrayList<String> words;
    public int turnNumber;
    public int guessedWord;

    public String artistParticipantId;

    public TurnSegment(int turnNumber, String artistParticipantId) {
        strokes = new ArrayList<Stroke>();
        words = new ArrayList<String>();
        wordIndex = -1;
        guessedWord = -1;
        this.turnNumber = turnNumber;
        this.artistParticipantId = artistParticipantId;
    }

    Handler mHandler = new Handler();
    Boolean showGuessResult = false;

    DrawView drawView;
    DrawingActivity activity;
    boolean keepPlaying = false;

    int currentPlaybackStrokeIndex = 0;

    public void doEndOfReplay() {
        activity.onPlaybackEnded();
        if (showGuessResult) {
            activity.createOpponentGuessDialog();
        }
    }

    // This is the animation thread that steps through each stroke 50ms per pixel.
    // If it hits the end of a stroke, it pauses slightly.
    public Runnable r = new Runnable() {
        @Override
        public void run() {
            if (keepPlaying == false) {
                return;
            }
            if (strokes.get(currentPlaybackStrokeIndex).step(drawView)) {
                currentPlaybackStrokeIndex++;

                if (currentPlaybackStrokeIndex >= strokes.size()) {
                    Log.d(TAG, "Replay over");
                    doEndOfReplay();
                    return;
                }

                // Pause slightly longer between strokes for a dramatic flourish.
                mHandler.postDelayed(r, 100);

                return;
            }

            mHandler.postDelayed(r, 50);
        }
    };

    public void playback(DrawingActivity activity, boolean showGuessResult) {
        // Don't double-play
        if (keepPlaying) {
            return;
        }
        this.showGuessResult = showGuessResult;
        this.activity = activity;
        drawView = activity.mDrawView;
        currentPlaybackStrokeIndex = 0;

        keepPlaying = true;

        if (strokes.size() == 0) {
            doEndOfReplay();
            return;
        }

        for (Stroke stroke : strokes) {
            stroke.reset();
        }
        activity.onPlaybackStarted();

        mHandler.post(r);
    }

    public void skip() {
        // Don't skip if you're not playing
        if (!keepPlaying) {
            return;
        }
        keepPlaying = false;

        for (int i = currentPlaybackStrokeIndex; i < strokes.size(); i++) {
            while (!strokes.get(i).step(drawView)) {
                Log.d(TAG, "Turboing");
            }
        }
        doEndOfReplay();

        activity.onPlaybackEnded();
    }

    public JSONObject persist() {
        JSONObject o = new JSONObject();

        try {
            JSONArray strokesArray = new JSONArray();

            int count = 0;
            for (Stroke s : strokes) {
                strokesArray.put(count++, s.persist());
            }

            JSONArray wordsArray = new JSONArray();
            count = 0;
            for (String w : words) {
                wordsArray.put(count++, w);
            }

            o.put("strokes", strokesArray);
            o.put("words", wordsArray);
            o.put("wordIndex", wordIndex);
            o.put("turnNumber", turnNumber);
            o.put("guessedWord", guessedWord);
            o.put("artistParticipantId", artistParticipantId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String st = o.toString();

        Log.d(TAG, "=== PERSISTING TURN\n" + st);

        return o;
    }

    public static TurnSegment unpersist(JSONObject o) {

        TurnSegment retVal = new TurnSegment(-1, null);

        try {
            JSONArray strokesArray = o.getJSONArray("strokes");

            for (int i = 0; i < strokesArray.length(); i++) {
                JSONObject strokeObj = strokesArray.getJSONObject(i);
                retVal.strokes.add(Stroke.unpersist(strokeObj));
            }

            retVal.wordIndex = o.getInt("wordIndex");

            JSONArray wordsArray = o.getJSONArray("words");

            for (int i = 0; i < wordsArray.length(); i++) {
                retVal.words.add(wordsArray.getString(i));
            }

            retVal.turnNumber = o.getInt("turnNumber");
            retVal.guessedWord = o.getInt("guessedWord");

            if (o.has("artistParticipantId")) {
                retVal.artistParticipantId = o.getString("artistParticipantId");
            }

            Log.d(TAG,
                    "Guessed word = " + retVal.guessedWord + ", "
                            + o.getInt("guessedWord"));

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return retVal;
    }

}
