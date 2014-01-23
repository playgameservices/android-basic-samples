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

import org.json.JSONException;

import android.util.Log;

// Stores a persistable integer point.
// Let's be honest; you could persist much more efficiently, given all the points
// are 0-9, but this way it persists in human-readable format for debugging.
public class EPoint {
    public int x, y;

    public final static String TAG = "FAIL";

    public EPoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public EPoint() {
    }

    public String persist() {
        String st = "" + x + y;

        if (x > 9 || x < 0) {
            Log.e(TAG, "FAIL!");
            x = 0;
        }
        if (y > 9 || y < 0) {
            Log.e(TAG, "FAIL!");
            y = 0;
        }

        assert (st.length() != 2);

        return st;
    }

    public static EPoint unpersist(char x, char y) throws JSONException {
        EPoint retVal = new EPoint();

        retVal.x = x - '0';
        retVal.y = y - '0';

        return retVal;
    }

}
