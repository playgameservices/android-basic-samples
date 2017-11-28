package com.google.example.games.basegameutils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;

public class BaseGameUtils {

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
            if (activity.getString(i).startsWith("YOUR_")) {
                problemFound = true;
                problems.append("- You must replace `YOUR_*`" +
                        "placeholder IDs in the ids.xml file by your project's IDs.").append("\n");
                break;
            }
        }

        if (problemFound) {
            problems.append("\n\nThese problems may prevent the app from working properly.");
            (new AlertDialog.Builder(activity)).setMessage(problems.toString())
                    .setNeutralButton(android.R.string.ok, null).create().show();
            return false;
        }

        return true;
    }

    /**
     * Create a simple {@link Dialog} with an 'OK' button and a message.
     *
     * @param activity the Activity in which the Dialog should be displayed.
     * @param text the message to display on the Dialog.
     * @return an instance of {@link android.app.AlertDialog}
     */
    public static Dialog makeSimpleDialog(Activity activity, String text) {
        return (new AlertDialog.Builder(activity)).setMessage(text)
                .setNeutralButton(android.R.string.ok, null).create();
    }

}
