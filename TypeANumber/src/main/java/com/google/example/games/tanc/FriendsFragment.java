package com.google.example.games.tanc;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.common.data.DataBufferUtils;
import com.google.android.gms.games.AnnotatedData;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.PlayerBuffer;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import java.util.ArrayList;

/** Fragment that shows the list of friends. */
public class FriendsFragment extends Fragment {
  private static final String TAG = "FriendsFragment";
  private static final int PAGE_SIZE = 200;

  interface Listener {
    // called when the user presses the `Back` button
    void onBackButtonClicked();
  }

  private View mView;
  private ListView mListView;
  private View mSpinner;
  private ArrayAdapter<Player> mAdapter;
  private MainActivity mActivity;
  private ActivityResultLauncher<IntentSenderRequest> resolveLauncherFriendsConsent;
  private ActivityResultLauncher<Intent> resolveLauncherCompareProfile;

  private Listener mListener = null;
  private OnCompleteListener<AnnotatedData<PlayerBuffer>> onCompleteListener =
      new OnCompleteListener<AnnotatedData<PlayerBuffer>>() {
        @Override
        public void onComplete(@NonNull Task<AnnotatedData<PlayerBuffer>> task) {
          if (task.isSuccessful()) {
            if (task.getResult() == null) {
              mListener.onBackButtonClicked();
            }
            PlayerBuffer playerBuffer = task.getResult().get();
            try {
              if (DataBufferUtils.hasNextPage(playerBuffer)) {
                mActivity
                    .getPlayersClient()
                    .loadMoreFriends(PAGE_SIZE)
                    .addOnCompleteListener(mActivity, onCompleteListener);
              } else {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                mAdapter = getAdapter(playerBuffer, inflater);
                mListView.setAdapter(mAdapter);
                mSpinner.setVisibility(View.GONE);
                mListView.setVisibility(View.VISIBLE);
              }
            } finally {
              playerBuffer.release();
            }
          } else {
            Log.e(TAG, "Getting friends failed with exception: " + task.getException());
            try {
              if (task.getException() instanceof ResolvableApiException) {
                PendingIntent pendingIntent =
                    ((ResolvableApiException) task.getException()).getResolution();
                resolveLauncherFriendsConsent.launch(
                    new IntentSenderRequest.Builder(pendingIntent).build());
              }
            } catch (Exception e) {
              Log.e(TAG, "Getting consent failed with exception: " + e);
              mListener.onBackButtonClicked();
            }
          }
        }
      };

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
    mView = inflater.inflate(R.layout.friends_screen, container, /* attachToRoot= */ false);
    mListView = mView.findViewById(R.id.load_friends_game_list);
    mSpinner = mView.findViewById(R.id.progress_bar);
    mActivity = (MainActivity) getActivity();
    mView
        .findViewById(R.id.back_button)
        .setOnClickListener(
            new View.OnClickListener() {
              @Override
              public void onClick(View view) {
                mListener.onBackButtonClicked();
             }
            });
     resolveLauncherFriendsConsent =
        registerForActivityResult(
            new ActivityResultContracts.StartIntentSenderForResult(),
            new ActivityResultCallback<ActivityResult>() {
              @Override
              public void onActivityResult(ActivityResult result) {
                if (result.getResultCode() == Activity.RESULT_OK) {
                  refreshFriends();
                } else {
                  mListener.onBackButtonClicked();
                }
             }
             });
    resolveLauncherCompareProfile =
        registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
              @Override
              public void onActivityResult(ActivityResult result) {
                if (result.getResultCode() == Activity.RESULT_OK) {
                  refreshFriends();
                }
              }
            });
    refreshFriends();
    return mView;
  }

  void setListener(Listener listener) {
    mListener = listener;
  }

  @NonNull
  private ArrayAdapter<Player> getAdapter(
      PlayerBuffer playerBuffer, final LayoutInflater inflater) {
    ArrayList<Player> players = new ArrayList<>();
    for (int i = 0; i < playerBuffer.getCount(); i++) {
      players.add(playerBuffer.get(i).freeze());
    }
    return new ArrayAdapter<Player>(mActivity, R.layout.friends_row, players) {
      @Override
      public View getView(int position, View convertView, ViewGroup viewGroup) {
        View rowView = inflater.inflate(R.layout.friends_row, viewGroup, /* attachToRoot= */ false);
        final Player player = getItem(position);
        TextView textView = rowView.findViewById(R.id.friend_name);
        textView.setText(player.getDisplayName());
        ImageButton showProfileButton = rowView.findViewById(R.id.show_profile);
        showProfileButton.setOnClickListener(
            new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                mActivity
                    .getPlayersClient()
                    .getCompareProfileIntentWithAlternativeNameHints(
                        player.getPlayerId(), player.getDisplayName(), mActivity.getDisplayName())
                    .addOnSuccessListener(
                        mActivity,
                        new OnSuccessListener<Intent>() {
                          @Override
                          public void onSuccess(Intent intent) {
                            resolveLauncherCompareProfile.launch(intent);
                         }
                        });
              }
             });
        return rowView;
      }
    };
  }

  void refreshFriends() {
    mListView.setAdapter(null);
    mSpinner.setVisibility(View.VISIBLE);
    mActivity
        .getPlayersClient()
        .loadFriends(PAGE_SIZE, /* forceReload= */ false)
        .addOnCompleteListener(mActivity, onCompleteListener);
  }
}