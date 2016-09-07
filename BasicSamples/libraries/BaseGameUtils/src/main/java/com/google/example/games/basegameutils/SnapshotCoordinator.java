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
package com.google.example.games.basegameutils;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.snapshot.Snapshot;
import com.google.android.gms.games.snapshot.SnapshotContents;
import com.google.android.gms.games.snapshot.SnapshotMetadata;
import com.google.android.gms.games.snapshot.SnapshotMetadataChange;
import com.google.android.gms.games.snapshot.Snapshots;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
 * This class can be used as a drop-in replacement for Games.Snapshots.  If the usage of the API
 * is inconsistent with enforced rules (any file can be open only once before closing it, and
 * snapshot data can only be committed once per open), then an IllegalStateException is thrown.
 * <p/>
 * NOTE:  *** The one exception to the drop-in replacement is that each call that returns a
 * PendingResult, that PendingResult MUST be processed by setting the ResultCallback
 * or bycalling await().
 * This is important to make sure the open/closed book-keeping is accurate.
 * <p/>
 * To make it easier to use Snapshots correctly, you should call SnapshotCoordinator.waitForClosed()
 * to obtain a PendingResult which will be resolved when the file is ready to be opened again.
 */
public class SnapshotCoordinator implements Snapshots {

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
     * Blocking wait for the given file to be closed.  Returns immediately if the
     * file is not open.
     *
     * @param filename - the file name in question.
     */
    public PendingResult<Result> waitForClosed(String filename) {
        CountDownLatch l;
        synchronized (this) {
            l = opened.get(filename);
        }
        return new CountDownPendingResult(l);
    }

    /*
        Many operations on the Snapshots API do not affect specific files.  These methods are
        passed directly through to the client API.
     */

    @Override
    public int getMaxDataSize(GoogleApiClient googleApiClient) {
        return Games.Snapshots.getMaxDataSize(googleApiClient);
    }

    @Override
    public int getMaxCoverImageSize(GoogleApiClient googleApiClient) {
        return Games.Snapshots.getMaxCoverImageSize(googleApiClient);
    }

    @Override
    public Intent getSelectSnapshotIntent(GoogleApiClient googleApiClient, String title,
                                          boolean allowAddButton, boolean allowDelete,
                                          int maxSnapshots) {
        return Games.Snapshots.getSelectSnapshotIntent(googleApiClient, title,
                allowAddButton, allowDelete, maxSnapshots);
    }

    @Override
    public PendingResult<LoadSnapshotsResult> load(GoogleApiClient googleApiClient,
                                                   boolean forceReload) {
        return Games.Snapshots.load(googleApiClient, forceReload);
    }

    @Override
    public SnapshotMetadata getSnapshotFromBundle(Bundle bundle) {
        return Games.Snapshots.getSnapshotFromBundle(bundle);
    }

    @Override
    public PendingResult<OpenSnapshotResult> resolveConflict(GoogleApiClient googleApiClient,
                                                             String conflictId, String snapshotId,
                                                             SnapshotMetadataChange snapshotMetadataChange,
                                                             SnapshotContents snapshotContents) {

        // Since the unique name of the snapshot is unknown, this resolution method cannot be safely
        // used.  Please use another method of resolution.
        throw new IllegalStateException("resolving conflicts with ids is not supported.");
    }

    @Override
    public void discardAndClose(GoogleApiClient googleApiClient, Snapshot snapshot) {
        if (isAlreadyOpen(snapshot.getMetadata().getUniqueName()) &&
                !isAlreadyClosing(snapshot.getMetadata().getUniqueName())) {
            Games.Snapshots.discardAndClose(googleApiClient, snapshot);
            Log.d(TAG, "Closed " + snapshot.getMetadata().getUniqueName());
            setClosed(snapshot.getMetadata().getUniqueName());
        } else {
            throw new IllegalStateException(snapshot.getMetadata().getUniqueName() +
                    " is not open or is busy");
        }
    }

    @Override
    public PendingResult<OpenSnapshotResult> open(GoogleApiClient googleApiClient,
                                                  final String filename, boolean createIfNotFound) {
        // check if the file is already open
        if (!isAlreadyOpen(filename)) {
            setIsOpening(filename);
            try {
                return new CoordinatedPendingResult<>(
                        Games.Snapshots.open(googleApiClient, filename, createIfNotFound),
                        new ResultListener() {
                            @Override
                            public void onResult(Result result) {
                                // if open failed, set the file to closed, otherwise, keep it open.
                                if (!result.getStatus().isSuccess()) {
                                    Log.d(TAG, "Open was not a success: " +
                                            result.getStatus() + " for filename " + filename);
                                    setClosed(filename);
                                } else {
                                    Log.d(TAG, "Open successful: " + filename);
                                }
                            }
                        });
            } catch (RuntimeException e) {
                // catch runtime exceptions here - they should not happen, but they do.
                // mark the file as closed so it can be attempted to be opened again.
                setClosed(filename);
                throw e;
            }
        } else {
            // a more sophisticated solution could attach this operation to a future
            // that would be triggered by closing the file, but this will at least avoid
            // corrupting the data with non-resolvable conflicts.
            throw new IllegalStateException(filename + " is already open");
        }
    }

    @Override
    public PendingResult<OpenSnapshotResult> open(GoogleApiClient googleApiClient,
                                                  final String filename, boolean createIfNotFound,
                                                  int conflictPolicy) {
        // check if the file is already open
        if (!isAlreadyOpen(filename)) {
            setIsOpening(filename);
            try {
                return new CoordinatedPendingResult<>(
                        Games.Snapshots.open(googleApiClient, filename, createIfNotFound,
                                conflictPolicy),
                        new ResultListener() {
                            @Override
                            public void onResult(Result result) {
                                // if open failed, set the file to closed, otherwise, keep it open.
                                if (!result.getStatus().isSuccess()) {
                                    Log.d(TAG, "Open was not a success: " +
                                            result.getStatus() + " for filename " + filename);
                                    setClosed(filename);
                                } else {
                                    Log.d(TAG, "Open successful: " + filename);
                                }
                            }
                        });
            } catch (RuntimeException e) {
                setClosed(filename);
                throw e;
            }
        } else {
            throw new IllegalStateException(filename + " is already open");
        }
    }

    @Override
    public PendingResult<OpenSnapshotResult> open(GoogleApiClient googleApiClient,
                                                  final SnapshotMetadata snapshotMetadata) {
        // check if the file is already open
        if (!isAlreadyOpen(snapshotMetadata.getUniqueName())) {
            setIsOpening(snapshotMetadata.getUniqueName());
            try {
                return new CoordinatedPendingResult<>(
                        Games.Snapshots.open(googleApiClient, snapshotMetadata),
                        new ResultListener() {
                            @Override
                            public void onResult(Result result) {
                                // if open failed, set the file to closed, otherwise, keep it open.
                                if (!result.getStatus().isSuccess()) {
                                    Log.d(TAG, "Open was not a success: " +
                                            result.getStatus() + " for filename " +
                                            snapshotMetadata.getUniqueName());
                                    setClosed(snapshotMetadata.getUniqueName());
                                } else {
                                    Log.d(TAG, "Open was successful: " +
                                            snapshotMetadata.getUniqueName());
                                }
                            }
                        });
            } catch (RuntimeException e) {
                setClosed(snapshotMetadata.getUniqueName());
                throw e;
            }
        } else {
            throw new IllegalStateException(snapshotMetadata.getUniqueName() + " is already open");
        }
    }

    @Override
    public PendingResult<OpenSnapshotResult> open(GoogleApiClient googleApiClient,
                                                  final SnapshotMetadata snapshotMetadata,
                                                  int conflictPolicy) {
        // check if the file is already open
        if (!isAlreadyOpen(snapshotMetadata.getUniqueName())) {
            setIsOpening(snapshotMetadata.getUniqueName());
            try {
                return new CoordinatedPendingResult<>(Games.Snapshots.open(
                        googleApiClient, snapshotMetadata, conflictPolicy),
                        new ResultListener() {
                            @Override
                            public void onResult(Result result) {
                                // if open failed, set the file to closed, otherwise, keep it open.
                                if (!result.getStatus().isSuccess()) {
                                    Log.d(TAG, "Open was not a success: " +
                                            result.getStatus() + " for filename " +
                                            snapshotMetadata.getUniqueName());
                                    setClosed(snapshotMetadata.getUniqueName());
                                } else {
                                    Log.d(TAG, "Open was successful: " +
                                            snapshotMetadata.getUniqueName());
                                }
                            }
                        });
            } catch (RuntimeException e) {
                setClosed(snapshotMetadata.getUniqueName());
                throw e;
            }
        } else {
            throw new IllegalStateException(snapshotMetadata.getUniqueName() + " is already open");
        }
    }

    @Override
    public PendingResult<CommitSnapshotResult> commitAndClose(GoogleApiClient googleApiClient,
                                                              final Snapshot snapshot,
                                                              SnapshotMetadataChange
                                                                      snapshotMetadataChange) {
        if (isAlreadyOpen(snapshot.getMetadata().getUniqueName()) &&
                !isAlreadyClosing(snapshot.getMetadata().getUniqueName())) {
            setIsClosing(snapshot.getMetadata().getUniqueName());
            try {
                return new CoordinatedPendingResult<>(
                        Games.Snapshots.commitAndClose(googleApiClient, snapshot,
                                snapshotMetadataChange),
                        new ResultListener() {
                            @Override
                            public void onResult(Result result) {
                                // even if commit and close fails, the file is closed.
                                Log.d(TAG, "CommitAndClose complete, closing " +
                                        snapshot.getMetadata().getUniqueName());
                                setClosed(snapshot.getMetadata().getUniqueName());
                            }
                        });
            } catch (RuntimeException e) {
                setClosed(snapshot.getMetadata().getUniqueName());
                throw e;
            }
        } else {
            throw new IllegalStateException(snapshot.getMetadata().getUniqueName() +
                    " is either closed or is closing");
        }
    }

    @Override
    public PendingResult<DeleteSnapshotResult> delete(GoogleApiClient googleApiClient,
                                                      final SnapshotMetadata snapshotMetadata) {
        if (!isAlreadyOpen(snapshotMetadata.getUniqueName()) &&
                !isAlreadyClosing(snapshotMetadata.getUniqueName())) {
            setIsClosing(snapshotMetadata.getUniqueName());
            try {
                return new CoordinatedPendingResult<>(
                        Games.Snapshots.delete(googleApiClient, snapshotMetadata),
                        new ResultListener() {
                            @Override
                            public void onResult(Result result) {
                                // deleted files are closed.
                                setClosed(snapshotMetadata.getUniqueName());
                            }
                        });
            } catch (RuntimeException e) {
                setClosed(snapshotMetadata.getUniqueName());
                throw e;
            }
        } else {
            throw new IllegalStateException(snapshotMetadata.getUniqueName() +
                    " is either open or is busy");
        }
    }

    @Override
    public PendingResult<OpenSnapshotResult> resolveConflict(GoogleApiClient googleApiClient,
                                                             String conflictId,
                                                             final Snapshot snapshot) {
        if (!isAlreadyOpen(snapshot.getMetadata().getUniqueName()) &&
                !isAlreadyClosing(snapshot.getMetadata().getUniqueName())) {
            setIsOpening(snapshot.getMetadata().getUniqueName());
            try {
                return new CoordinatedPendingResult<>(
                        Games.Snapshots.resolveConflict(googleApiClient, conflictId, snapshot),
                        new ResultListener() {
                            @Override
                            public void onResult(Result result) {
                                if (!result.getStatus().isSuccess()) {
                                    setClosed(snapshot.getMetadata().getUniqueName());
                                }
                            }
                        });
            } catch (RuntimeException e) {
                setClosed(snapshot.getMetadata().getUniqueName());
                throw e;
            }
        } else {
            throw new IllegalStateException(snapshot.getMetadata().getUniqueName() +
                    " is already open or is busy");
        }
    }

    /**
     * Interface to be triggered when a PendingResult is completed.
     */
    private interface ResultListener {
        void onResult(Result result);
    }

    /**
     * Wrapper of PendingResult so the coordinator class is notified when an operation completes.
     *
     * @param <T>
     */
    private class CoordinatedPendingResult<T extends Result> extends PendingResult<T> {
        PendingResult<T> innerResult;
        ResultListener listener;

        public CoordinatedPendingResult(PendingResult<T> result, ResultListener listener) {
            innerResult = result;
            this.listener = listener;
        }

        @NonNull
        @Override
        public T await() {
            T retval = innerResult.await();
            if (listener != null) {
                listener.onResult(retval);
            }
            return retval;
        }

        @NonNull
        @Override
        public T await(long l, @NonNull TimeUnit timeUnit) {
            T retval = innerResult.await(l, timeUnit);
            if (listener != null) {
                listener.onResult(retval);
            }
            return retval;
        }

        @Override
        public void cancel() {
            if (listener != null) {
                listener.onResult(new Result() {

                    @Override
                    public Status getStatus() {
                        return new Status(CommonStatusCodes.CANCELED);
                    }
                });
            }
            innerResult.cancel();
        }

        @Override
        public boolean isCanceled() {
            return innerResult.isCanceled();
        }

        @Override
        public void setResultCallback(@NonNull ResultCallback<? super T> resultCallback) {
            final ResultCallback<? super T> theCallback = resultCallback;
            innerResult.setResultCallback(new ResultCallback<T>() {
                @Override
                public void onResult(@NonNull T t) {
                    if (listener != null) {
                        listener.onResult(t);
                    }
                    theCallback.onResult(t);
                }
            });
        }

        @Override
        public void setResultCallback(@NonNull ResultCallback<? super T> resultCallback,
                                      long l, @NonNull TimeUnit timeUnit) {
            final ResultCallback<? super T> theCallback = resultCallback;
            innerResult.setResultCallback(new ResultCallback<T>() {
                @Override
                public void onResult(@NonNull T t) {
                    if (listener != null) {
                        listener.onResult(t);
                    }
                    theCallback.onResult(t);
                }
            }, l, timeUnit);
        }
    }

    private class CountDownPendingResult extends PendingResult<Result> {
        private final CountDownLatch latch;
        private boolean canceled;

        private final Status Success = new Status(CommonStatusCodes.SUCCESS);
        private final Status Canceled = new Status(CommonStatusCodes.CANCELED);

        public CountDownPendingResult(CountDownLatch latch) {
            this.latch = latch;
            canceled = false;
        }

        @NonNull
        @Override
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

        @NonNull
        @Override
        public Result await(long l, @NonNull TimeUnit timeUnit) {
            if (!canceled && latch != null) {
                try {
                    latch.await(l, timeUnit);
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

        @Override
        public void cancel() {
            canceled = true;
        }

        @Override
        public boolean isCanceled() {
            return canceled;
        }

        @Override
        public void setResultCallback(
                @NonNull final ResultCallback<? super Result> resultCallback) {
            if (!canceled && latch != null) {
                AsyncTask<Object, Object, Void> task = new AsyncTask<Object, Object, Void>() {

                    /**
                     * Override this method to perform a computation on a background thread. The
                     * specified parameters are the parameters passed to {@link #execute}
                     * by the caller of this task.
                     * <p/>
                     * This method can call {@link #publishProgress} to publish updates
                     * on the UI thread.
                     *
                     * @param params The parameters of the task.
                     * @return A result, defined by the subclass of this task.
                     * @see #onPreExecute()
                     * @see #onPostExecute
                     * @see #publishProgress
                     */
                    @Override
                    protected Void doInBackground(Object... params) {
                        try {
                            latch.await();
                            resultCallback.onResult(new Result() {
                                @Override
                                public com.google.android.gms.common.api.Status getStatus() {
                                    return canceled ? Canceled : Success;
                                }
                            });
                        } catch (InterruptedException e) {
                            resultCallback.onResult(new Result() {
                                @Override
                                public com.google.android.gms.common.api.Status getStatus() {
                                    return Canceled;
                                }
                            });
                        }
                        return null;
                    }
                };
                task.execute(latch);
            } else {
                resultCallback.onResult(new Result() {
                    @Override
                    public com.google.android.gms.common.api.Status getStatus() {
                        return canceled ? Canceled : Success;
                    }
                });
            }
        }

        @Override
        public void setResultCallback(@NonNull final ResultCallback<? super Result> resultCallback,
                                      final long l, @NonNull final TimeUnit timeUnit) {
            if (!canceled && latch != null) {
                AsyncTask<Object, Object, Void> task = new AsyncTask<Object, Object, Void>() {

                    /**
                     * Override this method to perform a computation on a background thread. The
                     * specified parameters are the parameters passed to {@link #execute}
                     * by the caller of this task.
                     * <p/>
                     * This method can call {@link #publishProgress} to publish updates
                     * on the UI thread.
                     *
                     * @param params The parameters of the task.
                     * @return A result, defined by the subclass of this task.
                     * @see #onPreExecute()
                     * @see #onPostExecute
                     * @see #publishProgress
                     */
                    @Override
                    protected Void doInBackground(Object... params) {
                        try {
                            latch.await(l, timeUnit);
                            resultCallback.onResult(new Result() {
                                @Override
                                public com.google.android.gms.common.api.Status getStatus() {
                                    return canceled ? Canceled : Success;
                                }
                            });
                        } catch (InterruptedException e) {
                            resultCallback.onResult(new Result() {
                                @Override
                                public com.google.android.gms.common.api.Status getStatus() {
                                    return Canceled;
                                }
                            });
                        }
                        return null;
                    }
                };
                task.execute(latch);
            } else {
                resultCallback.onResult(new Result() {
                    @Override
                    public com.google.android.gms.common.api.Status getStatus() {
                        return canceled ? Canceled : Success;
                    }
                });
            }
        }
    }
}
