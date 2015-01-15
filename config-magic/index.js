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
var CLIENT_ID =
    '845336616957-pa9ouhagvc7hvv4i0sb46ngd2ldqkerr.apps.googleusercontent.com';
var SCOPES = [
  'https://www.googleapis.com/auth/androidpublisher',
];

var auth = auth || {};
var ui = ui || {};

$(document).ready(function() {
  // Put the sample name in the menu bar title
  $('#sample').text('(' + ui.getSample() + ')');

  // Tell the config-form which sample
  $('config-form').attr('sample', ui.getSample());

  // Start of configuration event
  $('config-form').on('action-start', function() {
    ui.showProgressBar();
    ui.clearResults();
  });

  // End of configuration event
  $('config-form').on('action-end', function() {
    ui.hideProgressBar();
  });

  // Configuration event produced useful result
  $('config-form').on('action-result', function(e) {
    ui.setResults(e.target.result);
  });
});

function onClientLoad() {
  // Get the Games Configuration API
  gapi.client.load('gamesConfiguration', 'v1configuration', function() {
    console.log('Games Configuration API Loaded');

    // Try silent login
    console.log('Attempting silent authorization...');
    auth.signIn(true);
  });
}


/**
 * Initiate the sign-in process.
 * @param {boolean} immediate - true if the immediate, silent sign-in flow
 * should be used, false if the user should be prompted for consent.
 */
auth.signIn = function(immediate) {
  gapi.auth.authorize({
    client_id: CLIENT_ID,
    immediate: immediate,
    scope: SCOPES.join(' ')
  }, auth.authCallback);
};


/**
 * Initiate the sign-out process.
 */
auth.signOut = function() {
  console.log('Signing Out');
  gapi.auth.signOut(auth.authCallback);
  auth.authCallback({});
};


/**
 * Callback to parse the result of sign-in or sign-out. Displays the correct UI
 * for the sign-in state.
 * @param {object} authResult - the JSON object returned to a signIn or signOut
 * call.
 */
auth.authCallback = function(authResult) {
  console.log('Auth Result: ', authResult);
  if (authResult.access_token) {
    $('.signed-in').show();
    $('.signed-out').hide();
  } else {
    $('.signed-in').hide();
    $('.signed-out').show();
  }
};


/**
 * Parse the name of the sample to be configured from the 'sample' URL
 * query parameter.
 * @return {string} the name of the sample to be configured.
 */
ui.getSample = function() {
  var url = window.location.href;
  var matches = url.match(/sample=([a-zA-z0-9]*)/);
  return matches[1];
};


/**
 * Show an indeterminate progress bar.
 */
ui.showProgressBar = function() {
  $('#progress').show();
};


/**
 * Hide the indeterminate progress bar.
 */
ui.hideProgressBar = function() {
  $('#progress').hide();
};


/**
 * Set the text in the results area.
 * @param {string} result - the text to put in the result area.
 */
ui.setResults = function(result) {
  $('#ids').text(result);
};


/**
 * Clear the results from any previous action.
 */
ui.clearResults = function() {
  $('#ids').empty();
};
