/* Copyright (C) 2014 Google Inc.
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

package com.google.example.games.catt2;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.SharedPreferences;
import android.os.Parcel;

import com.google.android.gms.drive.Contents;
import com.google.android.gms.games.snapshot.Snapshot;
import com.google.android.gms.games.snapshot.SnapshotMetadata;

/**
 * Represents the player's progress in the game. The player's progress is how many stars
 * they got on each level.
 *
 * @author Bruno Oliveira
 */
public class SaveGame{

    private static final String TAG = "CollectAllTheStars";

    // serialization format version
    private static final String SERIAL_VERSION = "1.1";

    // Maps a level name (like "2-8") to the number of stars the user has in that level.
    // Any key that doesn't exist in this map is considered to be associated to the value 0.
    Map<String,Integer> mLevelStars = new HashMap<String,Integer>();

    // Minimum and maximum stars the player can have on a level
    public static final int MIN_STARS = 0, MAX_STARS = 5;

    /** Constructs an empty SaveGame object. No stars on no levels. */
    public SaveGame() {
    }

    /** Constructs a SaveGame object from serialized data. */
    public SaveGame(byte[] data) {
        if (data == null) return; // default progress
        loadFromJson(new String(data));
    }

    /** Constructs a SaveGame object from a JSON string. */
    public SaveGame(String json) {
        if (json == null) return; // default progress
        loadFromJson(json);
    }

    /** Constructs a SaveGame object by reading from a SharedPreferences. */
    public SaveGame(SharedPreferences sp, String key) {
        loadFromJson(sp.getString(key, ""));
    }

    /** Replaces this SaveGame's content with the content loaded from the given JSON string. */
    public void loadFromJson(String json) {
        zero();
        if (json == null || json.trim().equals("")) return;

        try {
            JSONObject obj = new JSONObject(json);
            String format = obj.getString("version");
            if (!format.equals(SERIAL_VERSION)) {
                throw new RuntimeException("Unexpected loot format " + format);
            }
            JSONObject levels = obj.getJSONObject("levels");
            Iterator<?> iter = levels.keys();

            while (iter.hasNext()) {
                String levelName = (String)iter.next();
                mLevelStars.put(levelName, levels.getInt(levelName));
            }
        }
        catch (JSONException ex) {
            ex.printStackTrace();
            Log.e(TAG, "Save data has a syntax error: " + json, ex);

            // Initializing with empty stars if the game file is corrupt.
            // NOTE: In your game, you want to try recovering from the snapshot payload.
            mLevelStars.clear();
        }
        catch (NumberFormatException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Save data has an invalid number in it: " + json, ex);
        }
    }

    /** Serializes this SaveGame to an array of bytes. */
    public byte[] toBytes() {
        return toString().getBytes();
    }

    /** Serializes this SaveGame to a JSON string. */
    @Override
    public String toString() {
        try {
            JSONObject levels = new JSONObject();
            for (String levelName : mLevelStars.keySet()) {
                levels.put(levelName, mLevelStars.get(levelName));
            }

            JSONObject obj = new JSONObject();
            obj.put("version", SERIAL_VERSION);
            obj.put("levels", levels);
            return obj.toString();
        }
        catch (JSONException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Error converting save data to JSON.", ex);
        }
    }

    /**
     * Computes the union of this SaveGame with the given SaveGame. The union will have any
     * levels present in either operand. If the same level is present in both operands,
     * then the number of stars will be the greatest of the two.
     *
     * @param other The other operand with which to compute the union.
     * @return The result of the union.
     */
    public SaveGame unionWith(SaveGame other) {
        SaveGame result = clone();
        for (String levelName : other.mLevelStars.keySet()) {
            int existingStars = result.getLevelStars(levelName);
            int newStars = other.getLevelStars(levelName);

            // only overwrite if number of stars is greater
            if (newStars > existingStars) {
                result.setLevelStars(levelName, newStars);
            }

            // note that this code doesn't preserve mappings from a level to the value 0,
            // but that is not a problem because, in our semantics, the absence of a mapping
            // is equivalent to mapping to 0 stars.
        }
        return result;
    }

    /** Returns a clone of this SaveGame object. */
    public SaveGame clone() {
        SaveGame result = new SaveGame();
        for (String levelName : mLevelStars.keySet()) {
            result.setLevelStars(levelName, getLevelStars(levelName));
        }
        return result;
    }

    /** Resets this SaveGame object to be empty. Empty means no stars on no levels. */
    public void zero() {
        mLevelStars.clear();
    }

    /** Returns whether or not this SaveGame is empty. Empty means no stars on no levels. */
    public boolean isZero() {
        return mLevelStars.keySet().size() == 0;
    }

    /** Save this SaveGame object to a SharedPreferences. */
    public void save(SharedPreferences sp, String key) {
        SharedPreferences.Editor spe = sp.edit();
        spe.putString(key, toString());
        spe.commit();
    }

    /**
     * Gets how many stars the player has on the given level. If the level does not exist
     * in the save game, will return 0.
     */
    public int getLevelStars(String levelName) {
        Integer r = mLevelStars.get(levelName);
        return r == null ? 0 : r.intValue();
    }

    /**
     * Gets how many stars the player has on the given level. If the level does not exist
     * in the save game, will return 0.
     */
    public int getLevelStars(int world, int level) {
        return getLevelStars(String.valueOf(world) + "-" + String.valueOf(level));
    }

    /** Sets how many stars the player has on the given level. */
    public void setLevelStars(String levelName, int stars) {
        if (stars < MIN_STARS) stars = MIN_STARS;
        if (stars > MAX_STARS) stars = MAX_STARS;
        if (stars == 0) {
            // zero stars means remove it from the map
            if (mLevelStars.containsKey(levelName)) {
                mLevelStars.remove(levelName);
            }
        } else {
            mLevelStars.put(levelName, stars);
        }
    }

    /** Sets how many stars the player has on the given level. */
    public void setLevelStars(int world, int level, int stars) {
        setLevelStars(String.valueOf(world) + "-" + String.valueOf(level), stars);
    }

    /**  Implementation of Snapshot interface.  */
    /*
    @Override
    public byte[] readFully() {
        return new byte[0];
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {

    }

    @Override
    public boolean writeBytes(byte[] bytes) {
        return false;
    }

    @Override
    public void jy() {

    }

    @Override
    public boolean modifyBytes(int i, byte[] bytes, int i2, int i3) {
        return false;
    }

    @Override
    public boolean isDataValid() {
        return false;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public Snapshot freeze() {
        return null;
    }

    @Override
    public SnapshotMetadata getMetadata() {
        return null;
    }

    @Override
    public Contents getContents() {
        return null;
    }*/
}
