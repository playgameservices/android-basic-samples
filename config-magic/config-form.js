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
(function() {
  var app = app || {};
  var util = util || {};
  var config = config || {};

  var elm = undefined;

  // Initialize Polymer
  Polymer('config-form', {
    publish: {
      appid: {
        reflect: true
      },
      infofile: {
        reflect: true
      },
      sample: {
        reflect: true
      }
    },
    ready: function() {
      this.reset = app.reset;
      this.list = app.list;
      this.configure = app.configure;

      this.hasAppId = function() {
        return (this.appid != undefined && this.appid.length > 0);
      };

      elm = this;
      console.log('Config Form Ready');
    }
  });

  /**
   * Skeleton objects for achievementConfiguration and leaderboardConfiguration.
   * Populated by info objects to create resources.
   */
  var SKELETONS = {
    achievement: {
      achievementType: 'STANDARD',
      initialState: 'REVEALED',
      draft: {
        name: {
          translations: [
            {
              locale: 'en-US',
              value: undefined
            }
          ]
        },
        description: {
          translations: [
            {
              locale: 'en-US',
              value: undefined
            }
          ]
        },
        pointValue: undefined
      }
    },
    leaderboard: {
      scoreOrder: undefined,
      draft: {
        name: {
          translations: [
            {
              locale: 'en-US',
              value: undefined
            }
          ]
        },
        scoreFormat: {
          numberFormatType: 'NUMERIC',
          numDecimalPlaces: 0
        }
      }
    }
  };

  /**
   * Run the full configuration process.  Adds all missing achievements and
   * leaderboards, then creates and displays a sample ids.xml file.
   */
  app.configure = function() {
    console.log('Setting Configuration');
    if (!elm.hasAppId()) {
      alert('Please enter your App ID');
      return;
    }

    // Insert all achievements and leaderboards
    elm.fire('action-start');
    var ach = config.insertAllConfigs(app.getAchievementInfo(),
        config.listAchievements, config.insertAchievement,
        util.createAchievementResource);
    var ldr = config.insertAllConfigs(app.getLeaderboardInfo(),
        config.listLeaderboards, config.insertLeaderboard,
        util.createLeaderboardResource);

    // Once all inserted, print ids.xml
    ach.then(ldr).then(function() {
      console.log('Configuration complete.');
      elm.fire('action-end');
      app.makeIdsXml();
    });
  };

  /**
   * List the current configuration of the application.  Prints the results
   * as a sample ids.xml file.
   */
  app.list = function() {
    console.log('Listing Configuration');
    if (!elm.hasAppId()) {
      alert('Please enter your App ID');
      return;
    }

    elm.fire('action-start');
    app.makeIdsXml();
  };

  /**
   * Delete all of the applications existing leaderboards and achievements.
   * Print an ids.xml file when completed that shows only the App ID.
   */
  app.reset = function() {
    console.log('Resetting Configuration');
    if (!elm.hasAppId()) {
      alert('Please enter your App ID');
      return;
    }

    elm.fire('action-start');
    var ldr = config.deleteAllLeaderboards();
    var ach = config.deleteAllAchievements();

    // TODO(samstern): Make sure both are executing
    ach.then(ldr).then(function() {
      console.log('All configuration deleted.');
      elm.fire('action-end');
      app.makeIdsXml();
    });
  };

  /**
   * List all of the applications configured leaderboards and achievements
   * and convert the response into a single Android XML file.  Display result
   * in the UI.
   */
  app.makeIdsXml = function() {
    elm.fire('action-start');

    var batch = gapi.client.newBatch();
    batch.add(config.listAchievements(), {id: 'ach'});
    batch.add(config.listLeaderboards(), {id: 'ldr'});

    batch.then(function(results) {
      var achItems = results.result['ach'].result.items;
      var ldrItems = results.result['ldr'].result.items;

      var achXml = util.resourcesToXml(achItems, 'achievement');
      var ldrXml = util.resourcesToXml(ldrItems, 'leaderboard');

      var appIdXml = document.createElement('string');
      appIdXml.setAttribute('name', 'app_id');
      appIdXml.innerHTML = elm.appid;

      // AppId + Achievements + Leaderboards
      var allXML = appIdXml.outerHTML + '\n\n' + achXml + '\n' + ldrXml;

      elm.result = allXML;
      elm.fire('action-end');
      elm.fire('action-result');
    });
  };

  /**
   * Get the information needed to configure achievements for this sample.
   * @return {object} the achievement information from the 'config' object.
   */
  app.getAchievementInfo = function() {
    return configs[elm.sample].achievements;
  };

  /**
   * Get the information needed to configure leaderboards for this sample.
   * @return {object} the leaderboard information from the 'config' object.
   */
  app.getLeaderboardInfo = function() {
    return configs[elm.sample].leaderboards;
  };

  /**
   * List all achievement configurations for the current application.
   * @return {promise} a promise from the achievementConfigurations.list
   * method.
   */
  config.listAchievements = function() {
    return gapi.client.gamesConfiguration.achievementConfigurations.list({
      applicationId: elm.appid
    });
  };

  /**
   * Insert a draft achievement into the current application.
   * @param {object} resource - an achievementConfiguration resource object,
   * @return {promise} a promise from the achievementConfigurations.insert
   * method.
   */
  config.insertAchievement = function(resource) {
    return gapi.client.gamesConfiguration.achievementConfigurations.insert({
      applicationId: elm.appid,
      resource: resource
    });
  };

  /**
   * Delete an achievement from the current application.
   * @param {object} achievementId - the id of the achievement to delete.
   * @return {promise} a promise from the achievementConfigurations.delete
   * method.
   */
  config.deleteAchievement = function(achievementId) {
    return gapi.client.gamesConfiguration.achievementConfigurations.delete({
      achievementId: achievementId
    });
  };

  /**
  * List all leaderboard configurations for the current application.
  * @return {promise} a promise from the leaderboardConfigurations.list
  * method.
  */
  config.listLeaderboards = function() {
    return gapi.client.gamesConfiguration.leaderboardConfigurations.list({
      applicationId: elm.appid
    });
  };

  /**
   * Insert a draft leaderboard into the current application.
   * @param {object} resource - an leaderboardConfiguration resource object,
   * @return {promise} a promise from the leaderboardConfigurations.insert
   * method.
   */
  config.insertLeaderboard = function(resource) {
    return gapi.client.gamesConfiguration.leaderboardConfigurations.insert({
      applicationId: elm.appid,
      resource: resource
    });
  };

  /**
   * Delete a leaderboard from the current application.
   * @param {object} leaderboardId - the id of the leaderboard to delete.
   * @return {promise} a promise from the leaderboardConfigurations.delete
   * method.
   */
  config.deleteLeaderboard = function(leaderboardId) {
    return gapi.client.gamesConfiguration.leaderboardConfigurations.delete({
      leaderboardId: leaderboardId
    });
  };

  /**
   * Delete all leaderboards from the current application.
   * @return {promise} the aggregate promise of the delete requests for each
   * leaderboard
   */
  config.deleteAllLeaderboards = function() {
    return config.listLeaderboards().then(function(resp) {
      var items = resp.result.items;
      var batch = gapi.client.newBatch();
      for (var i in items) {
        console.log('Deleting Leaderboard: ', items[i].id);
        batch.add(config.deleteLeaderboard(items[i].id));
      }

      return batch;
    });
  };

  /**
   * Delete all achievements from the current application.
   * @return {promise} the aggregate promise of the delete requests for each
   * achievement
   */
  config.deleteAllAchievements = function() {
    return config.listAchievements().then(function(resp) {
      var items = resp.result.items;
      var batch = gapi.client.newBatch();
      for (var i in items) {
        console.log('Deleting Achievement: ', items[i].id);
        batch.add(config.deleteAchievement(items[i].id));
      }

      return batch;
    });
  };

  /**
   * Insert a series of configuration objects for the current application.
   * @param {array} infoObjs - an array of info objects that will be converted
   * to configuration resources.
   * @param {function} listFn - the function to list all existing configurations
   * of the type to be inserted.  This should return a promise.
   * @param {function} insertFn - the function to insert a configuration
   * resource of the desired type.  This should take a resource object as its
   * only argument and return a promise.
   * @param {function} resourceFn - the function to convert a single info object
   * into a resource of the desired type.
   * @return {promise} the aggregate promise of all the insertFn requests.
  */
  config.insertAllConfigs = function(infoObjs, listFn, insertFn, resourceFn) {
    var inserted = 0;
    var toInsert = infoObjs.length;

    return listFn().then(function(resp) {
      var items = resp.result.items;

      var batch = gapi.client.newBatch();
      batch.add(listFn());

      for (var i in infoObjs) {
        // Create new leaderboard
        var infoObj = infoObjs[i];
        var newResource = resourceFn(infoObj);

        // Check if it exists
        var exists = false;
        for (var j in items) {
          if (items[j].draft.name.translations[0].value === infoObj.name) {
            exists = true;
          }
        }

        // Insert if necessary
        if (!exists) {
          console.log('Inserting: ', infoObj.name);
          batch.add(insertFn(newResource));
        } else {
          console.log('Skipping (duplicate name): ', infoObj.name);
        }
      }

      // Wait until all inserts have resolved
      return batch;
    });
  };

  /**
   * Make a clone of a simple JSON object. Uses serialization.
   * @param {object} obj - the JSON object to clone.
   * @return {object} a value-for-value clone of the argument object.
   */
  util.cloneObject = function(obj) {
    // Clone object by going through JSON
    return JSON.parse(JSON.stringify(obj));
  };

  /**
   * Create an achievement resource from a simple info object. Properly
   * populates a skeleton with any provided fields.
   * @param {object} infoObj - the info object representing the achievement.
   * @return {object} a properly-formed achievementConfiguration resource
   * object.
   */
  util.createAchievementResource = function(infoObj) {
    // Make a copy of the skeleton object
    var newResource = util.cloneObject(SKELETONS.achievement);

    // Set name, desc, and pointValue
    newResource.draft.name.translations[0].value = infoObj.name;
    newResource.draft.description.translations[0].value = infoObj.description;
    newResource.draft.pointValue = infoObj.pointValue;

    // Check if achievement should be incremental, otherwise standard
    if (infoObj.stepsToUnlock) {
      newResource.achievementType = 'INCREMENTAL';
      newResource.stepsToUnlock = infoObj.stepsToUnlock;
    }

    // Check if achievement should be hidden, otherwise revealed
    if (infoObj.hidden) {
      newResource.initialState = 'HIDDEN';
    }

    return newResource;
  };

  /**
   * Create an leaderboard resource from a simple info object. Properly
   * populates a skeleton with any provided fields.
   * @param {object} infoObj - the info object representing the leaderboard.
   * @return {object} a properly-formed leaderboardConfiguration resource object.
   */
  util.createLeaderboardResource = function(infoObj) {
    // Make a clone of the skeleton object
    var newResource = util.cloneObject(SKELETONS.leaderboard);

    // Set name and score order
    newResource.draft.name.translations[0].value = infoObj.name;
    newResource.scoreOrder = infoObj.scoreOrder;

    return newResource;
  };

  /**
   * Convert a series of resource objects to an Android XML file.
   * @param {array} items - an array of resource objects.
   * @param {string} prefix - a prefix to be used for the string key names in
   * the XML file.
   * @return {string} a string representing the contents of an Android resources
   * XML file.
   */
  util.resourcesToXml = function(items, prefix) {
    var root = document.createElement('xml');

    for (var i in items) {
      var item = items[i];
      var itemElm = document.createElement('string');
      var itemName = item.draft.name.translations[0].value;
      var name = prefix + '_' + itemName.toLowerCase().replace(/\s/g, '_');

      itemElm.setAttribute('name', name);
      itemElm.innerHTML = item.id;

      root.appendChild(itemElm);
      root.innerHTML += '\n';
    }

    return root.innerHTML;
  };

})();

