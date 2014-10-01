package com.google.example.games.basegameutils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.IntentSender;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.GamesActivityResultCodes;

public class BaseGameUtils {
  public static void showAlert(Activity activity, String message) {
    (new AlertDialog.Builder(activity)).setMessage(message)
        .setNeutralButton(android.R.string.ok, null).create().show();
  }

  public static boolean resolveConnectionFailure(Activity activity,
                                                 GoogleApiClient client, ConnectionResult result, int requestCode,
                                                 String fallbackErrorMessage) {

    if (result.hasResolution()) {
      try {
        result.startResolutionForResult(activity, requestCode);
        return true;
      } catch (IntentSender.SendIntentException e) {
        // The intent was canceled before it was sent.  Return to the default
        // state and attempt to connect to get an updated ConnectionResult.
        client.connect();
        return false;
      }
    } else {
      // not resolvable... so show an error message
      int errorCode = result.getErrorCode();
      Dialog dialog = GooglePlayServicesUtil.getErrorDialog(errorCode,
          activity, requestCode);
      if (dialog != null) {
        dialog.show();
      } else {
        // no built-in dialog: show the fallback error message
        showAlert(activity, fallbackErrorMessage);
      }
      return false;
    }
  }

  /**
   * For use in sample code only. Checks if the sample was set up correctly,
   * including changing the package name to a non-Google package name and
   * replacing the placeholder IDs. Shows alert dialogs to notify about problems.
   * DO NOT call this method from a production app, it's meant only for samples!
   * @param resIds the resource IDs to check for placeholders
   * @return true if sample is set up correctly; false otherwise.
   */
  public static boolean verifySampleSetup(Activity activity, int... resIds) {
    StringBuilder problems = new StringBuilder();
    boolean problemFound = false;
    problems.append("The following set up problems were found:\n\n");

    // Did the developer forget to change the package name?
    if (activity.getPackageName().startsWith("com.google.example.games")) {
      problemFound = true;
      problems.append("- Package name cannot be com.google.*. You need to change the "
          + "sample's package name to your own package.").append("\n");
    }

    for (int i : resIds) {
      if (activity.getString(i).toLowerCase().contains("replaceme")) {
        problemFound = true;
        problems.append("- You must replace all " +
            "placeholder IDs in the ids.xml file by your project's IDs.").append("\n");
        break;
      }
    }

    if (problemFound) {
      problems.append("\n\nThese problems may prevent the app from working properly.");
      showAlert(activity, problems.toString());
      return false;
    }

    return true;
  }

  public static void showActivityResultError(Activity activity, int requestCode, int actResp,
                                             int errorCode, int errorDescription) {
    if (activity == null) {
      Log.e("BaseGameUtils", "*** No Activity. Can't show failure dialog!");
      return;
    }
    Dialog errorDialog;

    switch (actResp) {
      case GamesActivityResultCodes.RESULT_APP_MISCONFIGURED:
        errorDialog = makeSimpleDialog(activity,
            activity.getString(R.string.gamehelper_app_misconfigured));
        break;
      case GamesActivityResultCodes.RESULT_SIGN_IN_FAILED:
        errorDialog = makeSimpleDialog(activity,
            activity.getString(R.string.gamehelper_sign_in_failed));
      break;
      case GamesActivityResultCodes.RESULT_LICENSE_FAILED:
        errorDialog = makeSimpleDialog(activity,
            activity.getString(R.string.gamehelper_license_failed));
        break;
      default:
        // No meaningful Activity response code, so generate default Google
        // Play services dialog
        errorDialog = GooglePlayServicesUtil.getErrorDialog(errorCode,
            activity, requestCode, null);
        if (errorDialog == null) {
          // get fallback dialog
          Log.e("BaseGamesUtils",
              "No standard error dialog available. Making fallback dialog.");
          errorDialog = makeSimpleDialog(
              activity,
              activity.getString(errorCode)
                  + " "
                  + activity.getString(errorDescription));
        }
    }

    errorDialog.show();
  }

  public static Dialog makeSimpleDialog(Activity activity, String text) {
    return (new AlertDialog.Builder(activity)).setMessage(text)
        .setNeutralButton(android.R.string.ok, null).create();
  }

  public static Dialog makeSimpleDialog(Activity activity, String title, String text) {
      return (new AlertDialog.Builder(activity))
              .setTitle(title)
              .setMessage(text)
              .setNeutralButton(android.R.string.ok, null)
              .create();
  }

}
