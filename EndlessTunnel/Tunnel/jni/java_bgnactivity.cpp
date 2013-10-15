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
#include <jni.h>

#include <sys/atomics.h>
#include "engine.hpp"
#include "java_bgnactivity.hpp"
#include "jni_util.hpp"

#include "java_headers/com_google_example_games_tunnel_BaseGameNativeActivity.h"

#define SIGNIN_FAILED 0
#define SIGNIN_IN_PROGRESS 1
#define SIGNIN_SUCCEEDED 2
static int _signInStatus = SIGNIN_FAILED;

JNIEXPORT void JNICALL
Java_com_google_example_games_tunnel_BaseGameNativeActivity_native_1ReportSignInState
        (JNIEnv *env, jobject thiz, jboolean signedIn, jboolean inProgress) {
    int newState = inProgress ? SIGNIN_IN_PROGRESS : signedIn ? SIGNIN_SUCCEEDED : SIGNIN_FAILED;
    __atomic_swap(newState, &_signInStatus);
}

bool BGNActivity_IsSignedIn() {
    return _signInStatus == SIGNIN_SUCCEEDED;
}

bool BGNActivity_IsInProgress() {
    return _signInStatus == SIGNIN_IN_PROGRESS;
}

void BGNActivity_StartSignIn() {
    struct JniSetup *setup = GetJNISetup();
    jmethodID m_postStartSignIn = setup->env->GetMethodID(setup->clazz, "postStartSignIn", "()V");
    MY_ASSERT(m_postStartSignIn != NULL);
    _signInStatus = SIGNIN_IN_PROGRESS;
    setup->env->CallVoidMethod(setup->thiz, m_postStartSignIn);
}

void BGNActivity_StartSignOut() {
    struct JniSetup *setup = GetJNISetup();
    jmethodID m_postStartSignOut = setup->env->GetMethodID(setup->clazz, "postStartSignOut", "()V");
    MY_ASSERT(m_postStartSignOut != NULL);
    _signInStatus = SIGNIN_FAILED;
    setup->env->CallVoidMethod(setup->thiz, m_postStartSignOut);
}

void BGNActivity_ShowAchievements() {
    struct JniSetup *setup = GetJNISetup();
    jmethodID m_postShowAchievements = setup->env->GetMethodID(setup->clazz,
            "postShowAchievements", "()V");
    MY_ASSERT(m_postShowAchievements);
    setup->env->CallVoidMethod(setup->thiz, m_postShowAchievements);
}

void BGNActivity_ShowLeaderboards() {
    struct JniSetup *setup = GetJNISetup();
    jmethodID m_postShowLeaderboards = setup->env->GetMethodID(setup->clazz,
            "postShowLeaderboards", "()V");
    MY_ASSERT(m_postShowLeaderboards);
    setup->env->CallVoidMethod(setup->thiz, m_postShowLeaderboards);
}

void BGNActivity_ShowLeaderboard(const char *lb_id) {
    struct JniSetup *setup = GetJNISetup();
    jmethodID m_postShowLeaderboard = setup->env->GetMethodID(setup->clazz, "postShowLeaderboard",
            "(Ljava/lang/String;)V");
    MY_ASSERT(m_postShowLeaderboard);
    jstring j_lb_id = setup->env->NewStringUTF(lb_id);
    setup->env->CallVoidMethod(setup->thiz, m_postShowLeaderboard, j_lb_id);
    setup->env->DeleteLocalRef(j_lb_id);
}

void BGNActivity_UnlockAchievement(const char *ach_id) {
    struct JniSetup *setup = GetJNISetup();
    jmethodID m_postUnlockAchievement = setup->env->GetMethodID(setup->clazz,
            "postUnlockAchievement", "(Ljava/lang/String;)V");
    MY_ASSERT(m_postUnlockAchievement);
    jstring j_ach_id = setup->env->NewStringUTF(ach_id);
    setup->env->CallVoidMethod(setup->thiz, m_postUnlockAchievement, j_ach_id);
    setup->env->DeleteLocalRef(j_ach_id);
}

void BGNActivity_IncrementAchievement(const char *ach_id, int steps) {
    struct JniSetup *setup = GetJNISetup();
    jmethodID m_postIncrementAchievement = setup->env->GetMethodID(setup->clazz,
            "postIncrementAchievement", "(Ljava/lang/String;I)V");
    MY_ASSERT(m_postIncrementAchievement);
    jstring j_ach_id = setup->env->NewStringUTF(ach_id);
    setup->env->CallVoidMethod(setup->thiz, m_postIncrementAchievement, j_ach_id, (jint) steps);
    setup->env->DeleteLocalRef(j_ach_id);
}

void BGNActivity_SubmitScore(const char *lb_id, long score) {
    struct JniSetup *setup = GetJNISetup();
    jmethodID m_postSubmitScore = setup->env->GetMethodID(setup->clazz, "postSubmitScore",
            "(Ljava/lang/String;J)V");
    MY_ASSERT(m_postSubmitScore);
    jstring j_lb_id = setup->env->NewStringUTF(lb_id);
    setup->env->CallVoidMethod(setup->thiz, m_postSubmitScore, j_lb_id, (jlong) score);
    setup->env->DeleteLocalRef(j_lb_id);
}

void BGNActivity_GetDeviceMotionRange(int deviceId, int source,
            float *outMinX, float *outMaxX, float *outMinY, float *outMaxY) {
    struct JniSetup *setup = GetJNISetup();
    jmethodID m_getMinX = setup->env->GetMethodID(setup->clazz,
            "getDeviceMotionRangeMinX", "(II)F");
    jmethodID m_getMaxX = setup->env->GetMethodID(setup->clazz,
            "getDeviceMotionRangeMaxX", "(II)F");
    jmethodID m_getMinY = setup->env->GetMethodID(setup->clazz,
            "getDeviceMotionRangeMinY", "(II)F");
    jmethodID m_getMaxY = setup->env->GetMethodID(setup->clazz,
            "getDeviceMotionRangeMaxY", "(II)F");
    MY_ASSERT(m_getMinX);
    MY_ASSERT(m_getMaxX);
    MY_ASSERT(m_getMinY);
    MY_ASSERT(m_getMaxY);
    *outMinX = setup->env->CallFloatMethod(setup->thiz, m_getMinX, deviceId, source);
    *outMaxX = setup->env->CallFloatMethod(setup->thiz, m_getMaxX, deviceId, source);
    *outMinY = setup->env->CallFloatMethod(setup->thiz, m_getMinY, deviceId, source);
    *outMaxY = setup->env->CallFloatMethod(setup->thiz, m_getMaxY, deviceId, source);

    LOGD("Motion range for device %d, source %d is X:%f-%f, Y:%f-%f", deviceId, source,
            *outMinX, *outMaxX, *outMinY, *outMaxY);
}

int BGNActivity_GetApiLevel() {
    struct JniSetup *setup = GetJNISetup();
    jmethodID m_getApiLevel = setup->env->GetMethodID(setup->clazz,
            "getApiLevel", "()I");
    MY_ASSERT(m_getApiLevel);
    return setup->env->CallIntMethod(setup->thiz, m_getApiLevel);
}



