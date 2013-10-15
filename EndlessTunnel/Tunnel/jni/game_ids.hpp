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
#ifndef endlesstunnel_game_ids_hpp

// Leaderboard ID (from Developer Console)
// TODO: To run this sample, REPLACE THIS by your leaderboard ID.
#define LEADERBOARD_ID "CgkItLL7uJAZEAIQAg"
    // when updating this, don't forget to also update the corresponding constant
    // in OurActivity.java

// Level numbers that correspond to each achievement
#define LEVEL_A 2
#define LEVEL_B 5
#define LEVEL_C 10 
#define LEVEL_D 15
#define LEVEL_E 20

// scores that correspond to the "reach X points without crashing" achievements
#define SCORE_A 5000
#define SCORE_B 7500
#define SCORE_C 10000 

// Achievement IDs (from Developer Console)
// TODO: To run this sample, REPLACE THESE by your IDs.
#define ACH_REACH_LEVEL_A "CgkItLL7uJAZEAIQAQ"
#define ACH_REACH_LEVEL_B "CgkItLL7uJAZEAIQAw"
#define ACH_REACH_LEVEL_C "CgkItLL7uJAZEAIQBA"
#define ACH_REACH_LEVEL_D "CgkItLL7uJAZEAIQBQ"
#define ACH_REACH_LEVEL_E "CgkItLL7uJAZEAIQBg"
#define ACH_TEN_IN_A_ROW "CgkItLL7uJAZEAIQBw" // collect 10 bonuses without missing
#define ACH_DOUBLE_CRASH "CgkItLL7uJAZEAIQCA" // crash twice against same obstacle
#define ACH_CLOSE_CALL "CgkItLL7uJAZEAIQCQ" // brush really close to an obstacle
#define ACH_THROUGH_HOLE "CgkItLL7uJAZEAIQEQ" // pass through a 1-cube narrow hole
#define ACH_PERFECT_SCORE_A "CgkItLL7uJAZEAIQCg" // reach 5000 points without crashing
#define ACH_PERFECT_SCORE_B "CgkItLL7uJAZEAIQCw" // reach 7500 points without crashing
#define ACH_PERFECT_SCORE_C "CgkItLL7uJAZEAIQDA" // reach 10000 points without crashing
#define ACH_READ_STORY "CgkItLL7uJAZEAIQDQ" // read the back story for the game
#define ACH_INC_PLAY "CgkItLL7uJAZEAIQDg" // incremental achievement for playing
#define ACH_INC_COLLECT_BONUS "CgkItLL7uJAZEAIQDw" // incremental achievement for collecting bonuses
#define ACH_INC_CRASH "CgkItLL7uJAZEAIQEA" // incremental achievement for crashing

// Each achievement above has a corresponding "index" below, which we use internally
// in arrays and such. This has nothing to do with Play Games -- it's a game-specific
// implementation.
#define IDX_ACH_REACH_LEVEL_A 0
#define IDX_ACH_REACH_LEVEL_B 1
#define IDX_ACH_REACH_LEVEL_C 2
#define IDX_ACH_REACH_LEVEL_D 3
#define IDX_ACH_REACH_LEVEL_E 4
#define IDX_ACH_TEN_IN_A_ROW 5
#define IDX_ACH_DOUBLE_CRASH 6
#define IDX_ACH_CLOSE_CALL 7
#define IDX_ACH_THROUGH_HOLE 8
#define IDX_ACH_PERFECT_SCORE_A 9
#define IDX_ACH_PERFECT_SCORE_B 10
#define IDX_ACH_PERFECT_SCORE_C 11
#define IDX_ACH_READ_STORY 12
#define IDX_ACH_COUNT 13 // total number of achievements

// correspondence array between achievement indices and achievement IDs
#define IDX_TO_ACHIEVEMENT { ACH_REACH_LEVEL_A, ACH_REACH_LEVEL_B, ACH_REACH_LEVEL_C, \
        ACH_REACH_LEVEL_D, ACH_REACH_LEVEL_E, ACH_TEN_IN_A_ROW, ACH_DOUBLE_CRASH, \
        ACH_CLOSE_CALL, ACH_THROUGH_HOLE, ACH_PERFECT_SCORE_A, ACH_PERFECT_SCORE_B, \
        ACH_PERFECT_SCORE_C, ACH_READ_STORY }

#endif

