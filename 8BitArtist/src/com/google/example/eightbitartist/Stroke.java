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

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

// Keeps track of a single drawing stroke in a persistable way.
public class Stroke {

    public static final String TAG = "Stroke";

    ArrayList<EPoint> points;
    short color;
    int currentStep;
    boolean isClear;

    public Stroke(short color) {
        points = new ArrayList<EPoint>();
        this.color = color;
    }

    public JSONObject persist() throws JSONException {

        JSONObject me = new JSONObject();
        me.put("color", color);
        me.put("isClear", isClear);

        StringBuilder sb = new StringBuilder();

        for (EPoint point : points) {
            sb.append(point.persist());
        }

        me.put("points", sb.toString());

        return me;
    }

    public boolean step(DrawView dv) {
        if (isClear) {
            dv.clear(false);
            return true;
        }

        if (currentStep >= points.size()) {
            return true;
        }

        dv.setMacroPixel(points.get(currentStep).x, points.get(currentStep).y,
                color);

        currentStep++;

        if (currentStep >= points.size()) {
            return true;
        }

        return false;
    }

    public void reset() {
        currentStep = 0;
    }

    public static Stroke unpersist(JSONObject o) {
        short colorIndex;
        try {
            colorIndex = (short) o.getInt("color");

            Stroke retVal = new Stroke(colorIndex);

            if (o.has("isClear")) {
                retVal.isClear = o.getBoolean("isClear");
            }

            String points = o.getString("points");

            for (int i = 0; i < points.length(); i += 2) {
                if (i + 1 >= points.length()) {
                    Log.e(TAG, "Can't unpersist " + points);
                    break;
                }
                EPoint e = EPoint.unpersist(points.charAt(i),
                        points.charAt(i + 1));
                retVal.points.add(e);
            }

            return retVal;
        } catch (JSONException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();

            return null;
        }
    }

}
