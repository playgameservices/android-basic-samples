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
#ifndef endlesstunnel_java_ouractivity_hpp
#define endlesstunnel_java_ouractivity_hpp

// Returns the path where game files should be saved.
const char* OurActivity_GetFilesDir();

// Shows encouragement toasts ("You've just beat Xyz!").
void OurActivity_ShowEncouragementToasts(int score);

// Resets the state of encouragement toasts (so they appear again). This should be
// called, for example, when restarting a level.
void OurActivity_ResetEncouragementToasts(int score);

// Save the given level to cloud save.
void OurActivity_SaveState(int level);

// Determines whether or not a joystick is present. Note: due to platform limitations,
// this may not correctly report a joystick to be present on devices before
// Jellybean. So on API >= 16, "true" means "there is definitely a joystick" and "false" means
// "there is definitely not a joystick", but on earlier APIs "false" means "we don't
// know".
bool OurActivity_IsJoystickPresent();

// >= 0 means data is available. Negative results as below.
int OurActivity_GetCloudData();
#define OURACTIVITY_CLOUD_WAITING -1 // waiting for cloud (try again soon)
#define OURACTIVITY_CLOUD_FAILED -2 // failed to load

#endif

