/*
 * Copyright (C) 2015 Google Inc.
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
var configs = {
  /** TRIVIAL QUEST **/
  trivialquest: {
    achievements: [
      {
        name: 'Trivial Victory',
        description: 'Complete the Trivial Quest',
        pointValue: 50
      }
    ],
    leaderboards: []
  },

  /** TYPE A NUMBER **/
  typeanumber: {
    achievements: [
      {
        name: 'Prime',
        description: 'Type a prime number',
        pointValue: 50
      },
      {
        name: 'Really Bored',
        description: 'You are so bored!',
        pointValue: 50,
        stepsToUnlock: 100
      },
      {
        name: 'Bored',
        description: 'You must be bored.',
        pointValue: 50,
        stepsToUnlock: 100
      },
      {
        name: 'Humble',
        description: 'You are a humble fellow',
        pointValue: 50
      },
      {
        name: 'Arrogant',
        description: 'Wow, you are arrogant.',
        pointValue: 50
      },
      {
        name: 'Leet',
        description: 'You are the most 1337 H4X0R',
        pointValue: 50,
        hidden: true
      }
    ],
    leaderboards: [
      {
        name: 'Easy',
        scoreOrder: 'LARGER_IS_BETTER'
      },
      {
        name: 'Hard',
        scoreOrder: 'LARGER_IS_BETTER'
      }
    ]
  }
};
