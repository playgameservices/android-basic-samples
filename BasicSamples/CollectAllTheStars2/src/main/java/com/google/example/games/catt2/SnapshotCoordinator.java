// Copyright 2016 Google Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.example.games.catt2;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.games.AnnotatedData;
import com.google.android.gms.games.SnapshotsClient;
import com.google.android.gms.games.snapshot.Snapshot;
import com.google.android.gms.games.snapshot.SnapshotContents;
import com.google.android.gms.games.snapshot.SnapshotMetadata;
import com.google.android.gms.games.snapshot.SnapshotMetadataBuffer;
import com.google.android.gms.games.snapshot.SnapshotMetadataChange;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

/**
 * The SnapshotCoordinator is used to overcome some dangerous behavior when using Saved Game API
 * (aka Snapshots).  The problem is caused by the not having way to prevent having the same file
 * opened multiple overlapping times.  Also there is no way to stop the incorrect reusing of
 * the Snapshot contained in the OpenSnapshotResult object.  It should be used once to write or be
 * closed.  Multiple commits using the same snapshot will result in unrecoverable conflicts.
 * <p/>
 * This class is used to encapsulate the access to the Snapshots API and enforce these semantics of
 * exclusive file access and one-time committing of metadata.
 * <p/>
 * How to use this class
 * <p/>
 * This class can be used as a drop-in replacement for SnapshotsClient.  If the usage of the API
 * is inconsistent with enforced rules (any file can be open only once before closing it, and
 * snapshot data can only be committed once per open), then an IllegalStateException is thrown.
 * <p/>
 * To make it easier to use Snapshots correctly, you should call SnapshotCoordinator.waitForClosed()
 * to obtain a Task which will be resolved when the file is ready to be opened again.
 */
public class SnapshotCoordinator {

  private static final SnapshotCoordinator theInstance = new SnapshotCoordinator();

  private static final String TAG = "SnapshotCoordinator";

  /**
   * Singleton for coordinating the Snapshots API.  This is important since
   * we need to coordinate all operations through the same instance in order to
   * detect usages that would cause data corruption.
   *
   * @return the singleton
   */
  public static SnapshotCoordinator getInstance() {
    return theInstance;
  }

  // Sets to keep track of the files that are opened or in the process of closing.
  private final Map<String, CountDownLatch> opened;
  private final Set<String> closing;

  private SnapshotCoordinator() {
    opened = new HashMap<>();
    closing = new HashSet<>();
  }

  /**
   * Returns true if the named file is already opened.  This is a synchronized
   * operation since it is highly likely that multiple threads are involved via AsyncTasks.
   *
   * @param filename - the filename to check
   * @return true if opened.
   */
  public synchronized boolean isAlreadyOpen(String filename) {
    return opened.containsKey(filename);
  }

  /**
   * Returns true if the named file is in the process of closing.  This is a synchronized
   * operation since it is highly likely that multiple threads are involved via AsyncTasks.
   *
   * @param filename - the filename to check
   * @return true if closing.
   */
  public synchronized boolean isAlreadyClosing(String filename) {
    return closing.contains(filename);
  }

  /**
   * Records the fact that the named file is closing (which also includes committing the data).
   * This is a synchronized operation since it is highly likely that multiple threads
   * are involved via AsyncTasks.
   *
   * @param filename - the filename of interest.
   */
  private synchronized void setIsClosing(String filename) {
    closing.add(filename);
  }

  /**
   * Records the fact that the named file is closed.
   * This is a synchronized operation since it is highly likely that multiple threads
   * are involved via AsyncTasks.
   *
   * @param filename - the filename of interest.
   */
  private synchronized void setClosed(String filename) {
    closing.remove(filename);
    CountDownLatch l = opened.remove(filename);
    if (l != null) {
      l.countDown();
    }
  }

  /**
   * Records the fact that the named file is opening.
   * This is a synchronized operation since it is highly likely that multiple threads
   * are involved via AsyncTasks.
   *
   * @param filename - the filename of interest.
   */
  private synchronized void setIsOpening(String filename) {
    opened.put(filename, new CountDownLatch(1));
  }

  /**
   * Returns a task that will complete when given file is closed.  Returns immediately if the
   * file is not open.
   *
   * @param filename - the file name in question.
   */
  public Task<Result> waitForClosed(String filename) {
    final TaskCompletionSource<Result> taskCompletionSource = new TaskCompletionSource<>();

    final CountDownLatch latch;
    synchronized (this) {
      latch = opened.get(filename);
    }

    if (latch == null) {
      taskCompletionSource.setResult(null);

      return taskCompletionSource.getTask();
    }

    new AsyncTask<Void, Void, Void>() {
      @Override
      protected Void doInBackground(Void... voids) {
        Result result = new CountDownTask(latch).await();
        taskCompletionSource.setResult(result);

        return null;
      }
    }.execute();

    return taskCompletionSource.getTask();
  }

    /*
        Many operations on the Snapshots API do not affect specific files.  These methods are
        passed directly through to the client API.
     */

  public Task<Integer> getMaxDataSize(SnapshotsClient snapshotsClient) {
    return snapshotsClient.getMaxDataSize();
  }

  public Task<Integer> getMaxCoverImageSize(SnapshotsClient snapshotsClient) {
    return snapshotsClient.getMaxCoverImageSize();
  }

  public Task<Intent> getSelectSnapshotIntent(SnapshotsClient snapshotsClient,
                                              String title,
                                              boolean allowAddButton,
                                              boolean allowDelete,
                                              int maxSnapshots) {
    return snapshotsClient.getSelectSnapshotIntent(title,
        allowAddButton, allowDelete, maxSnapshots);
  }

  public Task<AnnotatedData<SnapshotMetadataBuffer>> load(SnapshotsClient snapshotsClient,
                                                          boolean forceReload) {
    return snapshotsClient.load(forceReload);
  }

  public SnapshotMetadata getSnapshotFromBundle(Bundle bundle) {
    return SnapshotsClient.getSnapshotFromBundle(bundle);
  }

  public Task<SnapshotsClient.DataOrConflict<Snapshot>> resolveConflict(SnapshotsClient snapshotsClient,
                                                                        String conflictId, String snapshotId,
                                                                        SnapshotMetadataChange snapshotMetadataChange,
                                                                        SnapshotContents snapshotContents) {
    // Since the unique name of the snapshot is unknown, this resolution method cannot be safely
    // used.  Please use another method of resolution.
    throw new IllegalStateException("resolving conflicts with ids is not supported.");
  }

  public Task<Void> discardAndClose(final SnapshotsClient snapshotsClient, final Snapshot snapshot) {

    final String filename = snapshot.getMetadata().getUniqueName();

    return setIsClosingTask(filename).continueWithTask(new Continuation<Void, Task<Void>>() {
      @Override
      public Task<Void> then(@NonNull Task<Void> task) throws Exception {
        return snapshotsClient.discardAndClose(snapshot)
            .addOnCompleteListener(new OnCompleteListener<Void>() {
              @Override
              public void onComplete(@NonNull Task<Void> task) {
                Log.d(TAG, "Closed " + snapshot.getMetadata().getUniqueName());
                setClosed(snapshot.getMetadata().getUniqueName());
              }
            });
      }
    });
  }

  @NonNull
  private OnCompleteListener<SnapshotsClient.DataOrConflict<Snapshot>> createOpenListener(final String filename) {
    return new OnCompleteListener<SnapshotsClient.DataOrConflict<Snapshot>>() {
      @Override
      public void onComplete(@NonNull Task<SnapshotsClient.DataOrConflict<Snapshot>> task) {
        // if open failed, set the file to closed, otherwise, keep it open.
        if (!task.isSuccessful()) {
          Exception e = task.getException();
          Log.e(TAG, "Open was not a success for filename " + filename, e);
          setClosed(filename);
        } else {
          SnapshotsClient.DataOrConflict<Snapshot> result
              = task.getResult();
          if (result.isConflict()) {
            Log.d(TAG, "Open successful: " + filename + ", but with a conflict");
          } else {
            Log.d(TAG, "Open successful: " + filename);
          }
        }
      }
    };
  }

  @NonNull
  private Task<Void> setIsOpeningTask(String filename) {
    TaskCompletionSource<Void> source = new TaskCompletionSource<>();

    if (isAlreadyOpen(filename)) {
      source.setException(new IllegalStateException(filename + " is already open!"));
    } else if (isAlreadyClosing(filename)) {
      source.setException(new IllegalStateException(filename + " is current closing!"));
    } else {
      setIsOpening(filename);
      source.setResult(null);
    }
    return source.getTask();
  }

  @NonNull
  private Task<Void> setIsClosingTask(String filename) {
    TaskCompletionSource<Void> source = new TaskCompletionSource<>();

    if (!isAlreadyOpen(filename)) {
      source.setException(new IllegalStateException(filename + " is already closed!"));
    } else if (isAlreadyClosing(filename)) {
      source.setException(new IllegalStateException(filename + " is current closing!"));
    } else {
      setIsClosing(filename);
      source.setResult(null);
    }
    return source.getTask();
  }

  public Task<SnapshotsClient.DataOrConflict<Snapshot>> open(final SnapshotsClient snapshotsClient,
                                                             final String filename,
                                                             final boolean createIfNotFound) {

    return setIsOpeningTask(filename).continueWithTask(new Continuation<Void, Task<SnapshotsClient.DataOrConflict<Snapshot>>>() {
      @Override
      public Task<SnapshotsClient.DataOrConflict<Snapshot>> then(@NonNull Task<Void> task) throws Exception {
        return snapshotsClient.open(filename, createIfNotFound)
            .addOnCompleteListener(createOpenListener(filename));
      }
    });
  }

  public Task<SnapshotsClient.DataOrConflict<Snapshot>> open(final SnapshotsClient snapshotsClient,
                                                             final String filename,
                                                             final boolean createIfNotFound,
                                                             final int conflictPolicy) {

    return setIsOpeningTask(filename).continueWithTask(new Continuation<Void, Task<SnapshotsClient.DataOrConflict<Snapshot>>>() {
      @Override
      public Task<SnapshotsClient.DataOrConflict<Snapshot>> then(@NonNull Task<Void> task) throws Exception {
        return snapshotsClient.open(filename, createIfNotFound, conflictPolicy)
            .addOnCompleteListener(createOpenListener(filename));
      }
    });
  }

  public Task<SnapshotsClient.DataOrConflict<Snapshot>> open(final SnapshotsClient snapshotsClient,
                                                             final SnapshotMetadata snapshotMetadata) {
    final String filename = snapshotMetadata.getUniqueName();

    return setIsOpeningTask(filename).continueWithTask(new Continuation<Void, Task<SnapshotsClient.DataOrConflict<Snapshot>>>() {
      @Override
      public Task<SnapshotsClient.DataOrConflict<Snapshot>> then(@NonNull Task<Void> task) throws Exception {
        return snapshotsClient.open(snapshotMetadata)
            .addOnCompleteListener(createOpenListener(filename));
      }
    });
  }

  public Task<SnapshotsClient.DataOrConflict<Snapshot>> open(final SnapshotsClient snapshotsClient,
                                                             final SnapshotMetadata snapshotMetadata,
                                                             final int conflictPolicy) {
    final String filename = snapshotMetadata.getUniqueName();

    return setIsOpeningTask(filename).continueWithTask(new Continuation<Void, Task<SnapshotsClient.DataOrConflict<Snapshot>>>() {
      @Override
      public Task<SnapshotsClient.DataOrConflict<Snapshot>> then(@NonNull Task<Void> task) throws Exception {
        return snapshotsClient.open(snapshotMetadata, conflictPolicy)
            .addOnCompleteListener(createOpenListener(filename));
      }
    });
  }

  public Task<SnapshotMetadata> commitAndClose(final SnapshotsClient snapshotsClient,
                                               final Snapshot snapshot,
                                               final SnapshotMetadataChange snapshotMetadataChange) {

    final String filename = snapshot.getMetadata().getUniqueName();

    return setIsClosingTask(filename).continueWithTask(new Continuation<Void, Task<SnapshotMetadata>>() {
      @Override
      public Task<SnapshotMetadata> then(@NonNull Task<Void> task) throws Exception {
        return snapshotsClient.commitAndClose(snapshot, snapshotMetadataChange)
            .addOnCompleteListener(new OnCompleteListener<SnapshotMetadata>() {
              @Override
              public void onComplete(@NonNull Task<SnapshotMetadata> task) {
                // even if commit and close fails, the file is closed.
                Log.d(TAG, "CommitAndClose complete, closing " +
                    filename);
                setClosed(filename);
              }
            });
      }
    });
  }

  public Task<String> delete(final SnapshotsClient snapshotsClient,
                             final SnapshotMetadata snapshotMetadata) {

    final String filename = snapshotMetadata.getUniqueName();
    TaskCompletionSource<Void> source = new TaskCompletionSource<>();

    if (isAlreadyOpen(filename)) {
      source.setException(new IllegalStateException(filename + " is still open!"));
    } else if (isAlreadyClosing(filename)) {
      source.setException(new IllegalStateException(filename + " is current closing!"));
    } else {
      setIsClosing(filename);
      source.setResult(null);
    }

    return source.getTask().continueWithTask(new Continuation<Void, Task<String>>() {
      @Override
      public Task<String> then(@NonNull Task<Void> task) throws Exception {
        return snapshotsClient.delete(snapshotMetadata)
            .addOnCompleteListener(new OnCompleteListener<String>() {
              @Override
              public void onComplete(@NonNull Task<String> task) {
                // deleted files are closed.
                setClosed(filename);
              }
            });
      }
    });
  }

  public Task<SnapshotsClient.DataOrConflict<Snapshot>> resolveConflict(final SnapshotsClient snapshotsClient,
                                                                        final String conflictId,
                                                                        final Snapshot snapshot) {
    final String filename = snapshot.getMetadata().getUniqueName();

    return setIsOpeningTask(filename).continueWithTask(new Continuation<Void, Task<SnapshotsClient.DataOrConflict<Snapshot>>>() {
      @Override
      public Task<SnapshotsClient.DataOrConflict<Snapshot>> then(@NonNull Task<Void> task) throws Exception {
        return snapshotsClient.resolveConflict(conflictId, snapshot)
            .addOnCompleteListener(new OnCompleteListener<SnapshotsClient.DataOrConflict<Snapshot>>() {
              @Override
              public void onComplete(@NonNull Task<SnapshotsClient.DataOrConflict<Snapshot>> task) {

                if (!task.isSuccessful()) {
                  setClosed(filename);
                }
              }
            });
      }
    });
  }

  private class CountDownTask {
    private final CountDownLatch latch;
    private boolean canceled;

    private final Status Success = new Status(CommonStatusCodes.SUCCESS);
    private final Status Canceled = new Status(CommonStatusCodes.CANCELED);

    public CountDownTask(CountDownLatch latch) {
      this.latch = latch;
      canceled = false;
    }

    @NonNull
    public Result await() {
      if (!canceled && latch != null) {
        try {
          latch.await();
        } catch (InterruptedException e) {
          return new Result() {
            @Override
            public Status getStatus() {
              return Canceled;
            }
          };
        }
      }
      return new Result() {
        @Override
        public Status getStatus() {
          return canceled ? Canceled : Success;
        }
      };
    }

  }
}