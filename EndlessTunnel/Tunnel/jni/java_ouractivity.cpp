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
#include <sys/atomics.h>

#include "common.hpp"
#include "java_ouractivity.hpp"
#include "jni_util.hpp"

#include "java_headers/com_google_example_games_tunnel_OurActivity.h"

static const int DIR_MAX = 512;
static char *_files_dir = NULL;

static int _cloud_result_available = 0;
static int _joystick_present = 0;

static volatile bool _cloud_load_success = false;
static volatile int _cloud_load_data = 0;

const char* OurActivity_GetFilesDir() {
    if (_files_dir) {
        return _files_dir;
    }

    LOGD("Attempting to query for save path.");
    struct JniSetup *setup = GetJNISetup();
    jmethodID m_getSavePath = setup->env->GetMethodID(setup->clazz,
            "getSavePath", "()Ljava/lang/String;");
    MY_ASSERT(m_getSavePath);
    jstring j_savePath = (jstring) setup->env->CallObjectMethod(setup->thiz, m_getSavePath);
    const char *str = setup->env->GetStringUTFChars(j_savePath, NULL);
    int capacity = strlen(str) + 1;
    _files_dir = new char[capacity];
    strcpy(_files_dir, str);
    _files_dir[capacity - 1] = '\0';
    LOGD("Save path: %s", _files_dir);

    setup->env->ReleaseStringUTFChars(j_savePath, str);
    setup->env->DeleteLocalRef(j_savePath);

    return _files_dir;
}

void OurActivity_ShowEncouragementToasts(int score) {
    struct JniSetup *setup = GetJNISetup();
    jmethodID m_postShowEncouragementToasts = setup->env->GetMethodID(setup->clazz,
            "postShowEncouragementToasts", "(I)V");
    MY_ASSERT(m_postShowEncouragementToasts);
    setup->env->CallVoidMethod(setup->thiz, m_postShowEncouragementToasts, score);
}

void OurActivity_ResetEncouragementToasts(int score) {
    struct JniSetup *setup = GetJNISetup();
    jmethodID m_postResetEncouragementToasts = setup->env->GetMethodID(setup->clazz,
            "postResetEncouragementToasts", "(I)V");
    MY_ASSERT(m_postResetEncouragementToasts);
    setup->env->CallVoidMethod(setup->thiz, m_postResetEncouragementToasts, score);
}

void OurActivity_SaveState(int level) {
    struct JniSetup *setup = GetJNISetup();
    jmethodID m_postSaveState = setup->env->GetMethodID(setup->clazz, "postSaveState", "(I)V");
    MY_ASSERT(m_postSaveState);
    setup->env->CallVoidMethod(setup->thiz, m_postSaveState, level);
}

int OurActivity_GetCloudData() {
    if (!_cloud_result_available) {
        return OURACTIVITY_CLOUD_WAITING;
    } else if (!_cloud_load_success) {
        return OURACTIVITY_CLOUD_FAILED;
    } else {
        return _cloud_load_data < 0 ? 0 : _cloud_load_data;
    }
}

bool OurActivity_IsJoystickPresent() {
    return (bool) _joystick_present;
}

JNIEXPORT void JNICALL
        Java_com_google_example_games_tunnel_OurActivity_native_1ReportCloudLoadResult
        (JNIEnv *env, jobject thiz, jboolean success, jint level) {

    _cloud_load_success = (bool) success;
    _cloud_load_data = (int) level;
    __atomic_swap(1, &_cloud_result_available);

    LOGD("Cloud load result: %s, level %d", _cloud_load_success ? "SUCCESS" : "FAILURE",
        _cloud_load_data);
}

JNIEXPORT void JNICALL
        Java_com_google_example_games_tunnel_OurActivity_native_1ReportJoystickPresent
        (JNIEnv *env, jobject thiz, jboolean present) {

    LOGD("Joystick report: %s", present ? "PRESENT" : "ABSENT");
    __atomic_swap(present ? 1 : 0, &_joystick_present);
}


