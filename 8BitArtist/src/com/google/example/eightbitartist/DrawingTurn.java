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

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

/**
 * Keeps track of turns.  There are three parts to any given turn:
 *   replayTurn: the guess to show on replay
 *   guessingTurn: the turn by your opponent that needs guessing
 *   artistTurn: the turn you just drew.
 * @author wolff
 * 
 */
public class DrawingTurn {

    // Turn to be replayed
    public TurnSegment replayTurn;
    // Turn to be replayed
    public TurnSegment guessingTurn;
    // Turn to be drawn
    public TurnSegment artistTurn;

    public static final String TAG = "DrawingTurn";

    // Ideally, you'd keep this on the server so you don't
    // repeat across games, but we have no server, so here
    // we are.
    public ArrayList<String> seenWords = new ArrayList<String>();

    public DrawingTurn() {
    }

    public byte[] persist() {
        JSONObject retVal = new JSONObject();

        try {
            if (artistTurn != null) {
                retVal.put("artistTurn", artistTurn.persist());
            }
            if (guessingTurn != null) {
                retVal.put("guessingTurn", guessingTurn.persist());
            }
            if (replayTurn != null) {
                retVal.put("replayTurn", replayTurn.persist());
            }

            JSONArray seenWordsObj = new JSONArray();
            for (String word : seenWords) {
                seenWordsObj.put(word);
            }
            retVal.put("seenWords", seenWordsObj);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        String st = retVal.toString();

        Log.d(TAG, "==== PERSISTING\n" + st);

        return st.getBytes(Charset.forName("UTF-16"));
    }

    static public DrawingTurn unpersist(byte[] byteArray) {

        if (byteArray == null) {
            Log.d(TAG, "Empty array---possible bug.");
            return new DrawingTurn();
        }

        String st = null;
        try {
            st = new String(byteArray, "UTF-16");
        } catch (UnsupportedEncodingException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();

            return null;
        }

        Log.d(TAG, "====UNPERSIST \n" + st);

        DrawingTurn retVal = new DrawingTurn();

        try {
            JSONObject obj = new JSONObject(st);

            if (obj.has("artistTurn")) {
                retVal.artistTurn = TurnSegment.unpersist(obj
                        .getJSONObject("artistTurn"));
            }

            if (obj.has("correct")) {
                retVal.artistTurn = TurnSegment.unpersist(obj
                        .getJSONObject("artistTurn"));
            }

            if (obj.has("guessingTurn")) {
                retVal.guessingTurn = TurnSegment.unpersist(obj
                        .getJSONObject("guessingTurn"));
            }

            if (obj.has("replayTurn")) {
                retVal.replayTurn = TurnSegment.unpersist(obj
                        .getJSONObject("replayTurn"));
            }

            if (obj.has("seenWords")) {
                JSONArray seenWordsObj = obj.getJSONArray("seenWords");

                for (int i = 0; i < seenWordsObj.length(); i++) {
                    retVal.seenWords.add(seenWordsObj.getString(i));
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return retVal;
    }
}
