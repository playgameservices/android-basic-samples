# Copyright (C) 2013 Google Inc.
# 
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# 
#     http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

LOCAL_PATH:=$(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := game

LOCAL_CFLAGS    := -Werror -Wno-trigraphs
# note: -Wno-trigraphs is needed because sometimes the trigraph sequences
# occur in the hard-coded texture data.

LOCAL_SRC_FILES := \
    android_main.cpp \
    anim.cpp \
    ascii_to_geom.cpp \
    dialog_scene.cpp \
    indexbuf.cpp \
    input_util.cpp \
    java_bgnactivity.cpp \
    java_ouractivity.cpp \
    jni_util.cpp \
    native_engine.cpp \
    obstacle.cpp  \
    obstacle_generator.cpp \
    our_shader.cpp \
    play_scene.cpp \
    scene.cpp \
    scene_manager.cpp \
    sfxman.cpp \
    shader.cpp \
    shape_renderer.cpp \
    tex_quad.cpp \
    text_renderer.cpp \
    texture.cpp \
    ui_scene.cpp \
    util.cpp \
    vertexbuf.cpp \
    welcome_scene.cpp 

LOCAL_STATIC_LIBRARIES := android_native_app_glue
LOCAL_LDLIBS    := -llog -lGLESv2 -landroid -lEGL -lOpenSLES

include $(BUILD_SHARED_LIBRARY)

$(call import-module,android/native_app_glue)

