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
#include "anim.hpp"
#include "dialog_scene.hpp"
#include "game_ids.hpp"
#include "java_bgnactivity.hpp"
#include "java_ouractivity.hpp"
#include "our_shader.hpp"
#include "play_scene.hpp"
#include "tex_quad.hpp"
#include "welcome_scene.hpp"

#include "data/blurb.inl"
#include "data/gplus_texture.inl"
#include "data/strings.inl"

#define TITLE_POS center, 0.85f
#define TITLE_FONT_SCALE 1.0f
#define TITLE_COLOR 0.0f, 1.0f, 0.0f

// button defaults:
#define BUTTON_COLOR 0.0f, 1.0f, 0.0f
#define BUTTON_SIZE 0.2f, 0.2f
#define BUTTON_FONT_SCALE 0.5f

// button geometry
#define BUTTON_PLAY_POS center, 0.5f
#define BUTTON_PLAY_SIZE 0.4f, 0.4f
#define BUTTON_PLAY_FONT_SCALE 1.0f

// size of all side buttons (achievements, leaderboards, story, about)
#define BUTTON_SIDEBUTTON_WIDTH (center - 0.4f)
#define BUTTON_SIDEBUTTON_HEIGHT 0.2f
#define BUTTON_SIDEBUTTON_SIZE BUTTON_SIDEBUTTON_WIDTH, BUTTON_SIDEBUTTON_HEIGHT

// position of each side button (the buttons on the sides of the PLAY button)
#define BUTTON_ACHIEVEMENTS_POS 0.1f + 0.5f * BUTTON_SIDEBUTTON_WIDTH, 0.6f
#define BUTTON_LEADERBOARD_POS 0.1f + 0.5f * BUTTON_SIDEBUTTON_WIDTH, 0.4f
#define BUTTON_STORY_POS center + 0.3f + 0.5f * BUTTON_SIDEBUTTON_WIDTH, 0.6f
#define BUTTON_ABOUT_POS center + 0.3f + 0.5f * BUTTON_SIDEBUTTON_WIDTH, 0.4f

#define BUTTON_SIGN_IN_POS 0.35f, 0.1f
#define BUTTON_SIGN_IN_WIDTH 0.4f
#define BUTTON_SIGN_IN_HEIGHT 0.15f
#define BUTTON_SIGN_IN_SIZE BUTTON_SIGN_IN_WIDTH, BUTTON_SIGN_IN_HEIGHT
#define BUTTON_SIGN_IN_COLOR 0xdd/256.0f, 0x4b/256.0f, 0x39/256.0f
#define BUTTON_SIGN_IN_FONT_SCALE 0.55f

#define BUTTON_SIGN_OUT_POS 0.2f, 0.1f
#define BUTTON_SIGN_OUT_SIZE 0.3f, 0.13f
#define BUTTON_SIGN_OUT_COLOR 0.0f, 0.4f, 0.0f

#define BUTTON_WHY_POS 0.8f, 0.1f
#define BUTTON_WHY_SIZE 0.3f, 0.2f

// position of the Google+ icon
#define GPLUS_ICON_POS 0.23f, 0.1f
#define GPLUS_ICON_SIZE 0.065f


WelcomeScene::WelcomeScene() {
    UiWidget *w;

    mOurShader = NULL;
    mGooglePlusTexture = NULL;
    mGooglePlusTexQuad = NULL;

}

WelcomeScene::~WelcomeScene() {
}

void WelcomeScene::RenderBackground() {
    RenderBackgroundAnimation(mShapeRenderer);
}

void WelcomeScene::OnButtonClicked(int id) {
    SceneManager *mgr = SceneManager::GetInstance();

    if (id == mPlayButtonId) {
        mgr->RequestNewScene(new PlayScene());
    } else if (id == mSignInButtonId) {
        BGNActivity_StartSignIn();
    } else if (id == mSignOutButtonId) {
        mgr->RequestNewScene((new DialogScene())->SetText(BLURB_SIGNOUT)->SetTwoButtons(
                S_YES, DialogScene::ACTION_SIGN_OUT, S_NO, DialogScene::ACTION_RETURN));
    } else if (id == mAchievementsButtonId && BGNActivity_IsSignedIn()) {
        BGNActivity_ShowAchievements();
    } else if (id == mLeaderboardButtonId && BGNActivity_IsSignedIn()) {
        BGNActivity_ShowLeaderboard(LEADERBOARD_ID);
    } else if (id == mStoryButtonId) {
        mgr->RequestNewScene((new DialogScene())->SetText(BLURB_STORY)->SetSingleButton(S_OK,
                DialogScene::ACTION_RETURN));
    } else if (id == mAboutButtonId) {
        mgr->RequestNewScene((new DialogScene())->SetText(BLURB_ABOUT)->SetSingleButton(S_OK,
                DialogScene::ACTION_RETURN));
    } else if (id == mWhyButtonId) {
        mgr->RequestNewScene((new DialogScene())->SetText(BLURB_WHY_SIGN_IN)->SetSingleButton(S_OK,
                DialogScene::ACTION_RETURN));
    }
}

void WelcomeScene::DoFrame() {
    // update widget states based on signed-in status
    UpdateWidgetStates();

    // if the sign in or cloud save process is in progress, show a wait screen. Otherwise, not:
    SetWaitScreen(BGNActivity_IsInProgress() ||
            (BGNActivity_IsSignedIn() && OurActivity_GetCloudData() == OURACTIVITY_CLOUD_WAITING));

    // draw the UI
    UiScene::DoFrame();

    // draw the Google+ icon, if needed
    if (!BGNActivity_IsSignedIn() && !mWaitScreen) {
        mGooglePlusTexQuad->Render();
    }
}

void WelcomeScene::UpdateWidgetStates() {
    bool signedIn = BGNActivity_IsSignedIn();
    GetWidgetById(mSignInButtonId)->SetVisible(!signedIn);
    GetWidgetById(mSignOutButtonId)->SetVisible(signedIn);
    GetWidgetById(mWhyButtonId)->SetVisible(!signedIn);
    GetWidgetById(mAchievementsButtonId)->SetEnabled(signedIn);
    GetWidgetById(mLeaderboardButtonId)->SetEnabled(signedIn);

    // Build navigation
    AddNav(mPlayButtonId, UI_DIR_RIGHT, mStoryButtonId);
    AddNav(mPlayButtonId, UI_DIR_LEFT, signedIn ? mAchievementsButtonId : -1);
    AddNav(mPlayButtonId, UI_DIR_DOWN, signedIn ? mSignOutButtonId : mSignInButtonId);

    AddNav(mAchievementsButtonId, UI_DIR_DOWN, mLeaderboardButtonId);
    AddNav(mAchievementsButtonId, UI_DIR_RIGHT, mPlayButtonId);

    AddNav(mLeaderboardButtonId, UI_DIR_DOWN, mSignOutButtonId);
    AddNav(mLeaderboardButtonId, UI_DIR_UP, mAchievementsButtonId);
    AddNav(mLeaderboardButtonId, UI_DIR_RIGHT, mPlayButtonId);

    AddNav(mStoryButtonId, UI_DIR_LEFT, mPlayButtonId);
    AddNav(mStoryButtonId, UI_DIR_DOWN, mAboutButtonId);

    AddNav(mAboutButtonId, UI_DIR_LEFT, mPlayButtonId);
    AddNav(mAboutButtonId, UI_DIR_UP, mStoryButtonId);
    AddNav(mAboutButtonId, UI_DIR_DOWN, signedIn ? mSignOutButtonId : mSignInButtonId);

    AddNav(mSignInButtonId, UI_DIR_UP, mPlayButtonId);
    AddNav(mSignInButtonId, UI_DIR_RIGHT, mWhyButtonId);

    AddNav(mWhyButtonId, UI_DIR_LEFT, mSignInButtonId);
    AddNav(mWhyButtonId, UI_DIR_UP, mPlayButtonId);

    AddNav(mSignOutButtonId, UI_DIR_UP, mPlayButtonId);
    AddNav(mSignOutButtonId, UI_DIR_RIGHT, mPlayButtonId);
}

void WelcomeScene::OnStartGraphics() {
    UiScene::OnStartGraphics();

    mOurShader = new OurShader();
    mOurShader->Compile();

    mGooglePlusTexture = new Texture();
    mGooglePlusTexture->InitFromRawRGB(GPLUS_TEXTURE.width, GPLUS_TEXTURE.height, false,
            GPLUS_TEXTURE.pixel_data);

    mGooglePlusTexQuad = new TexQuad(mGooglePlusTexture, mOurShader, 0.0f, 0.0f, 1.0f, 1.0f);
    mGooglePlusTexQuad->SetCenter(GPLUS_ICON_POS);
    mGooglePlusTexQuad->SetWidth(GPLUS_ICON_SIZE);
    mGooglePlusTexQuad->SetHeight(GPLUS_ICON_SIZE);
}

void WelcomeScene::OnCreateWidgets() {
    // create widgets
    float maxX = SceneManager::GetInstance()->GetScreenAspect();
    float center = 0.5f * maxX;

    // create the static title
    NewWidget()->SetText(S_TITLE)->SetCenter(TITLE_POS)->SetTextColor(TITLE_COLOR)
            ->SetFontScale(TITLE_FONT_SCALE)->SetTransition(UiWidget::TRANS_FROM_TOP);

    // create the "play" button
    mPlayButtonId = NewWidget()->SetText(S_PLAY)->SetTextColor(BUTTON_COLOR)
            ->SetCenter(BUTTON_PLAY_POS)->SetSize(BUTTON_PLAY_SIZE)
            ->SetFontScale(BUTTON_PLAY_FONT_SCALE)->SetIsButton(true)
            ->SetTransition(UiWidget::TRANS_SCALE)->GetId();

    // create the "sign out" button
    mSignOutButtonId = NewWidget()->SetTextColor(BUTTON_SIGN_OUT_COLOR)->SetText(S_SIGN_OUT)
            ->SetCenter(BUTTON_SIGN_OUT_POS)->SetSize(BUTTON_SIGN_OUT_SIZE)
            ->SetFontScale(BUTTON_SIGN_IN_FONT_SCALE)->SetIsButton(true)
            ->SetTransition(UiWidget::TRANS_FROM_BOTTOM)->GetId();

    // create the "sign in" button
    mSignInButtonId = NewWidget()->SetTextColor(1.0f, 1.0f, 1.0f)->SetText(S_SIGN_IN)
            ->SetBackColor(BUTTON_SIGN_IN_COLOR)
            ->SetCenter(BUTTON_SIGN_IN_POS)->SetSize(BUTTON_SIGN_IN_SIZE)
            ->SetFontScale(BUTTON_SIGN_IN_FONT_SCALE)->SetIsButton(true)
            ->SetHasBorder(false)->SetTransition(UiWidget::TRANS_FROM_BOTTOM)->GetId();

    // create the "why sign in" button
    mWhyButtonId = NewWidget()->SetTextColor(BUTTON_SIGN_IN_COLOR)->SetText(S_WHY_SIGN_IN)
            ->SetCenter(BUTTON_WHY_POS)->SetSize(BUTTON_WHY_SIZE)
            ->SetFontScale(BUTTON_FONT_SCALE)->SetIsButton(true)->SetHasBorder(false)
            ->SetTransparent(true)->SetTransition(UiWidget::TRANS_FROM_BOTTOM)->GetId();

    // achievements button
    mAchievementsButtonId = NewWidget()->SetTextColor(BUTTON_COLOR)->SetText(S_ACHIEVEMENTS)
            ->SetCenter(BUTTON_ACHIEVEMENTS_POS)->SetSize(BUTTON_SIDEBUTTON_SIZE)
            ->SetFontScale(BUTTON_FONT_SCALE)->SetIsButton(true)
            ->SetTransition(UiWidget::TRANS_FROM_LEFT)->GetId();

    // leaderboard button
    mLeaderboardButtonId = NewWidget()->SetTextColor(BUTTON_COLOR)->SetText(S_LEADERBOARD)
            ->SetCenter(BUTTON_LEADERBOARD_POS)->SetSize(BUTTON_SIDEBUTTON_SIZE)
            ->SetFontScale(BUTTON_FONT_SCALE)->SetIsButton(true)
            ->SetTransition(UiWidget::TRANS_FROM_LEFT)->GetId();

    // story button
    mStoryButtonId = NewWidget()->SetTextColor(BUTTON_COLOR)->SetText(S_STORY)
            ->SetCenter(BUTTON_STORY_POS)->SetSize(BUTTON_SIDEBUTTON_SIZE)
            ->SetFontScale(BUTTON_FONT_SCALE)->SetIsButton(true)
            ->SetTransition(UiWidget::TRANS_FROM_RIGHT)->GetId();

    // about button
    mAboutButtonId = NewWidget()->SetTextColor(BUTTON_COLOR)->SetText(S_ABOUT)
            ->SetCenter(BUTTON_ABOUT_POS)->SetSize(BUTTON_SIDEBUTTON_SIZE)
            ->SetFontScale(BUTTON_FONT_SCALE)->SetIsButton(true)
            ->SetTransition(UiWidget::TRANS_FROM_RIGHT)->GetId();

    // "Play" button is the default button
    SetDefaultButton(mPlayButtonId);

    // enable/disable widgets as appropriate to signed in state
    UpdateWidgetStates();

}

void WelcomeScene::OnKillGraphics() {
    UiScene::OnKillGraphics();
    CleanUp(&mGooglePlusTexQuad);
    CleanUp(&mGooglePlusTexture);
    CleanUp(&mOurShader);
}

