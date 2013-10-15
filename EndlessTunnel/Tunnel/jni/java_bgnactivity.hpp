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
#ifndef endlesstunnel_bgnactivity_hpp
#define endlesstunnel_bgnactivity_hpp

// Returns whether or not user is signed in with Google
bool BGNActivity_IsSignedIn();

// Start the sign in flow
void BGNActivity_StartSignIn();

// Start the sign out flow
void BGNActivity_StartSignOut();

// Returns whether or not there is a sign in flow in progress. This may be a result of
// explicitly requesting it via BGNActivity_StartSignIn, or as an automatic process
// that happens every time the app is brought back from the background.
bool BGNActivity_IsInProgress();

// Show the Achievements UI
void BGNActivity_ShowAchievements();

// Show the Leaderboards UI
void BGNActivity_ShowLeaderboards();

// Show the Leaderboard UI for a specific leaderboard
void BGNActivity_ShowLeaderboard(const char *lb_id);

// Unlock an achievement
void BGNActivity_UnlockAchievement(const char *ach_id);

// Increment an achievement
void BGNActivity_IncrementAchievement(const char *ach_id, int steps);

// Submit a score to a leaderboard
void BGNActivity_SubmitScore(const char *lb_id, long score);

// Get motion range for a particular input device
void BGNActivity_GetDeviceMotionRange(int deviceId, int source,
        float *outMinX, float *outMaxX, float *outMinY, float *outMaxY);

// Get API level
int BGNActivity_GetApiLevel();

#endif

