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

import com.google.android.gms.common.images.ImageManager;
import com.google.android.gms.games.snapshot.Snapshot;
import com.google.android.gms.games.snapshot.SnapshotMetadata;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Activity to select a snapshot from a list of snapshots or snapshot metadata.  The intended
 * use of this activity is to present a choice of two conflicting snapshots to the user so
 * they can select the "best" snapshot.
 *
 * There is also a code path that loads all the saved games and presents this list, which is not
 * expected to be "production" code, but rather demonstrate how the Snapshots.load() method works.
 *
 * @author Clayton Wilkinson (Google)
 */
public class SelectSnapshotActivity extends Activity implements AdapterView.OnItemClickListener {

    private static final String TAG = "CollectAllTheStars2.SelectSnapshotActivity";
    // intent data which is a snapshot metadata
    public static final String SNAPSHOT_METADATA = "snapshotmeta";

    // intent data that is a list of snapshot metadatas.
    public static final String SNAPSHOT_METADATA_LIST = "snapshotmetaList";

    // intent data that is the conflict id.  used when resolving a conflict.
    public static final String CONFLICT_ID = "conflictId";

    // intent data that is the retry count for retrying the conflict resolution.
    public static final String RETRY_COUNT = "retrycount";

    // keep these variables for the current activity and return them in the result.
    private String mConflictId;

    private int mRetryCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_snapshot);

        //expect the intent to include the list of snapshots or snapshot metadatas to display
        Intent intent = getIntent();
        if (intent != null) {
            ArrayList<SnapshotMetadata> snapshotMetadataList;
            ListView vw = (ListView) findViewById(R.id.snapshot_list);

            snapshotMetadataList = intent.getParcelableArrayListExtra(SNAPSHOT_METADATA_LIST);
            // set a custom list adapter that can display the image and other
            // information about a snapshot.
            vw.setAdapter(
                    new SnapshotListAdapter<SnapshotMetadata>(this, snapshotMetadataList));

            mConflictId = intent.getStringExtra(CONFLICT_ID);
            mRetryCount = intent.getIntExtra(RETRY_COUNT, 0);

            // register this class as the listener for when an item is selected
            vw.setOnItemClickListener(this);
        }
    }


    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long listId) {

        SnapshotMetadata selected = (SnapshotMetadata) adapterView.getItemAtPosition(position);
        Intent intent = new Intent(Intent.ACTION_DEFAULT);

        intent.putExtra(SNAPSHOT_METADATA, selected.freeze());

        if (mConflictId != null) {
            intent.putExtra(CONFLICT_ID, mConflictId);
            intent.putExtra(RETRY_COUNT, mRetryCount);
        }

        Log.d(TAG, "Finishing item at position " + position + " clicked");
        setResult(RESULT_OK, intent);
        finish();
    }

    /**
     * Custom mSnapshotMetadataArrayList adapter which holds the snapshot metadata.  This is used
     * to
     * display the image and information for each snapshot.
     */
    static class SnapshotListAdapter<T> extends ArrayAdapter<T> {


        public SnapshotListAdapter(Activity activity, ArrayList<T> data) {
            super(activity, R.layout.snapshotlayout, data);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            // load the view used for each item in the mSnapshotMetadataArrayList
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.snapshotlayout, parent, false);

            // get the elements of the view which display the specific data for the snapshot.
            TextView textView = (TextView) rowView.findViewById(R.id.label);
            TextView ageView = (TextView) rowView.findViewById(R.id.age);
            ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);

            SnapshotMetadata snapshotMetadata;
            T item = getItem(position);
            if (item instanceof Snapshot) {
                snapshotMetadata = ((Snapshot) item).getMetadata();
            } else {
                snapshotMetadata = (SnapshotMetadata) item;
            }
            textView.setText(snapshotMetadata.getDescription());
            ageView.setText(getAge(snapshotMetadata.getLastModifiedTimestamp()));
            ImageManager.create(getContext())
                    .loadImage(imageView, snapshotMetadata.getCoverImageUri());

            return rowView;

        }


        private static final long MILLIS_PER_MINUTE = 60 * 1000;

        private static final long MILLIS_PER_HOUR = 60 * MILLIS_PER_MINUTE;

        private static final long MILLIS_PER_DAY = 24 * MILLIS_PER_HOUR;

        /**
         * Helper function to convert the time difference into a string like "3 days ago"
         *
         * @param timestamp -  the time to convert.
         * @return localized string, never null.
         */
        private String getAge(long timestamp) {
            long delta = System.currentTimeMillis() - timestamp;
            int days = (int) (delta / MILLIS_PER_DAY);

            delta = delta % MILLIS_PER_DAY;
            int hours = (int) (delta / MILLIS_PER_HOUR);

            delta = delta % MILLIS_PER_HOUR;
            int minutes = (int) (delta / MILLIS_PER_MINUTE);

            delta = (int) (delta % MILLIS_PER_MINUTE);
            int seconds = (int) delta / 1000;

            if (days > 0) {
                return getContext().getString(R.string.format_days_ago, days);
            }
            if (hours > 0) {
                return getContext().getString(R.string.format_hours_ago, hours);
            }
            if (minutes > 0) {
                return getContext().getString(R.string.format_minutes_ago, minutes);
            }
            if (seconds > 0) {
                return getContext().getString(R.string.format_seconds_ago, seconds);
            }
            return getContext().getString(R.string.moments_ago);
        }
    }
}
