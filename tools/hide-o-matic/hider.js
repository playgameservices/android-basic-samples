/* Copyright (C) 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Please fill these out with the values from your
// actual game.  DON'T FORGET TO FILL OUT CLIENT_ID
// IN index.html!
var APP_ID = 'APP_ID';
var LEADERBOARD_ID = 'LEADERBOARD_ID';

// How many async loads have happened?
var UNITS_TO_LOAD = 3;
var unitsReady = 0;

/* Callback on signin check.
 *  Signin check happens both on page load and when you click
 *  the actual signin button.
 */
function signinCallback(authResult) {
  if (authResult['access_token']) {
    // Successfully authorized
    // Hide the sign-in button now that the user is authorized, for example:
    document.getElementById('signinButton')
      .setAttribute('style', 'display: none');
    // One of the async actions have happened
    unitsReady++;
    checkAllUnitsLoaded();
  } else if (authResult['error']) {
    // There was an error.
    // Possible error codes:
    //   "access_denied" - User denied access to your app
    //   "immediate_failed" - Could not automatically log in the user
    console.log('There was an error: ' + authResult['error']);

    document.getElementById('signinButton')
      .setAttribute('style', 'display: visible');
    document.getElementById('loggedInUI')
      .setAttribute('style', 'display: none');
  }
}

/** Load the current top 25 high scores and render them. */
function showHighScoreList() {
  var div = document.getElementById('tableDiv').innerHTML = '';

  // Create the request.
  var request = gapi.client.games.scores.list(
    {leaderboardId: LEADERBOARD_ID,
     collection: 'PUBLIC',
     timeSpan: 'ALL_TIME',
     maxResults: '25'});

  request.execute(function(response) {
    console.log('Leaderboard', response);
    var root = document.getElementById('tableDiv');
    var tab = document.createElement('table');
    tab.className = 'gridtable';
    var row, cell;

    // Make the header
    row = document.createElement('tr');
    cell = document.createElement('th');
    cell.appendChild(document.createTextNode(
                         'High score list, total scores on list are: ' +
                             response.result.numScores));
    row.appendChild(cell);
    tab.appendChild(row);

    row = document.createElement('tr');
    cell = document.createElement('th');
    cell.appendChild(document.createTextNode('DISPLAY NAME'));
    row.appendChild(cell);

    cell = document.createElement('th');
    cell.appendChild(document.createTextNode('Icon'));
    row.appendChild(cell);

    cell = document.createElement('th');
    cell.appendChild(document.createTextNode('PLAYER ID'));
    row.appendChild(cell);

    cell = document.createElement('th');
    row.appendChild(cell);

    cell = document.createElement('th');
    cell.appendChild(document.createTextNode('SCORE'));
    row.appendChild(cell);

    tab.appendChild(row);

    // Now actually parse the data.
    for (var index in response.result.items) {
      item = response.result.items[index];
      row = document.createElement('tr');

      console.log('Name: ' + item.player.displayName +
                  ', playerId:' + item.player.playerId +
                  ' ' + item.scoreValue);
      cell = document.createElement('td');
      cell.appendChild(document.createTextNode(item.player.displayName));
      row.appendChild(cell);

      cell = document.createElement('td');

      var img = document.createElement('img');
      img.setAttribute('src', item.player.avatarImageUrl + '?sz=50');
      img.setAttribute('height', '50px');
      img.setAttribute('width', '50px');
      cell.appendChild(img);
      row.appendChild(cell);

      cell = document.createElement('td');
      cell.appendChild(document.createTextNode(item.player.playerId));
      row.appendChild(cell);

      // Need an active button
      cell = document.createElement('td');
      var button = document.createElement('button');
      button.setAttribute('type', 'button');
      button.setAttribute('name', 'edit');
      button.setAttribute('value', item.player.playerId);
      button.appendChild(document.createTextNode('Pick me!'));
      button.addEventListener('click', sendPlayerDataToInputs, false);
      cell.appendChild(button);
      row.appendChild(cell);

      cell = document.createElement('td');
      cell.appendChild(document.createTextNode(item.scoreValue));
      row.appendChild(cell);

      tab.appendChild(row);
    }
    root.appendChild(tab);
  });
}

/** Responds to "Pick me!"
 * Fills in the textboxes at the bottom of the page with the user's ID
 * @param {Object} event the mouse event from clicking the button*  */
var sendPlayerDataToInputs = function(event) {
  console.log(event.target.value);

  document.getElementById('playerIdHideInput').value =
    event.target.value;

  document.getElementById('playerIdUnhideInput').value =
    event.target.value;

};

/** Use gamesManagement to hide a player */
var hidePlayer = function() {
  var id = document.getElementById('playerIdHideInput').value;

  if (id == '') {
    alert('You need to enter a valid player id.');
    return;
  }

  gapi.client.gamesManagement.players.hide(
    {applicationId: APP_ID,
     playerId: id}).execute(function(response) {
       console.log('Player hide:', response);
       if (response.error != null) {
         alert('There was an error hiding that player: ' +
               response.error.code + ': ' + response.error.message);
       }
       else
       {
         alert('Player is hidden! It may be a few seconds ' +
               'for this to propagate.');
       }
     });
};

var unhidePlayer = function() {
  var id = document.getElementById('playerIdUnhideInput').value;

  if (id == '') {
    alert('You need to enter a valid player id.');
    return;
  }

  gapi.client.gamesManagement.players.unhide(
    {applicationId: APP_ID,
     playerId: id}).execute(function(response) {
       console.log('Player hide:', response);
       if (response.error != null) {

         if (response.error.code == '404') {
           alert('You got a 404.  That might mean that ' +
                 ' player is already unhidden.');
         }
         else {
           alert('There was an error unhiding that player: ' +
                 response.error.code + ': ' + response.error.message);
         }
       }
       else
       {
         alert('Player is unhidden!  It may take up to 12 hours ' +
               'for this player to reappear.');
       }
     });
};

/** We have to wait for two libraries to load, and then
 * signin to occur before it's safe to show the logged in UI. */
function checkAllUnitsLoaded() {
  if (unitsReady >= UNITS_TO_LOAD) {
    document.getElementById('loggedInUI')
      .setAttribute('style', 'display: visible');
  }
}

/** Callback from loading client library.  You need a brief pause before
    you initiate new loads and really start the app. */
var onLoadCallback = function() {
  window.setTimeout(continueLoadingLibraries, 1);
};

var continueLoadingLibraries = function() {
  gapi.client.load('games', 'v1', function(response) {
    console.log('Games loaded.');
    unitsReady++;
    checkAllUnitsLoaded();
  });

  gapi.client.load('gamesManagement', 'v1management', function(response) {
    console.log('Management loaded');
    unitsReady++;
    checkAllUnitsLoaded();
  });
};

