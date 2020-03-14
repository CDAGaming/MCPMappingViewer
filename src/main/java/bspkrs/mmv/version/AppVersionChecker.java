/*
 * Copyright (C) 2014 bspkrs
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package bspkrs.mmv.version;

import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.prefs.Preferences;

public class AppVersionChecker {
    private final String appName;
    private final String currentVersion;
    private final Preferences versionCheckTracker;
    private final String LAST_VERSION_FOUND = "lastversionfound";
    private final String RUNS_SINCE_LAST_MSG = "runs_since_last_message";
    private final String CHECK_ERROR = "check_error";
    private final boolean errorDetected;
    private URL versionURL;
    private String newVersion;
    private String updateURL;
    private String[] logMsg;
    private String[] dialogMsg;
    private String lastNewVersionFound;
    private int runsSinceLastMessage;

    public AppVersionChecker(String appName, String currentVersion, String versionURL, String updateURL, String[] logMsg, String[] dialogMsg, int timeoutMS) {
        this.appName = appName;
        this.currentVersion = currentVersion;
        this.updateURL = updateURL;
        this.logMsg = logMsg;
        this.dialogMsg = dialogMsg;

        try {
            if (versionURL.startsWith("http://dl.dropboxusercontent.com"))
                versionURL = versionURL.replaceFirst("http", "https");

            this.versionURL = new URL(versionURL);
        } catch (Throwable ignore) {
        }

        String[] versionLines = loadTextFromURL(this.versionURL, new String[]{CHECK_ERROR}, timeoutMS);

        if ((versionLines.length == 0) || versionLines[0].trim().equals("<html>"))
            newVersion = CHECK_ERROR;
        else
            newVersion = versionLines[0].trim();

        errorDetected = newVersion.equals(CHECK_ERROR);

        versionCheckTracker = Preferences.userNodeForPackage(AppVersionChecker.class);

        lastNewVersionFound = versionCheckTracker.get(LAST_VERSION_FOUND, currentVersion);

        if (lastNewVersionFound.equals("<html>"))
            lastNewVersionFound = currentVersion;

        runsSinceLastMessage = versionCheckTracker.getInt(RUNS_SINCE_LAST_MSG, 0);

        if (errorDetected)
            newVersion = lastNewVersionFound;

        if (!errorDetected && !isCurrentVersion(lastNewVersionFound, newVersion)) {
            runsSinceLastMessage = 0;
            lastNewVersionFound = newVersion;
        } else
            runsSinceLastMessage = runsSinceLastMessage % 10;

        versionCheckTracker.putInt(RUNS_SINCE_LAST_MSG, runsSinceLastMessage + 1);
        versionCheckTracker.put(LAST_VERSION_FOUND, lastNewVersionFound);

        // Override instantiated updateURL with second line of version file if it exists and is non-blank
        if ((versionLines.length > 1) && (versionLines[1].trim().length() != 0))
            this.updateURL = versionLines[1];

        setLogMessage(logMsg);
        setDialogMessage(dialogMsg);
    }

    public AppVersionChecker(String appName, String oldVer, String versionURL, String updateURL) {
        this(appName, oldVer, versionURL, updateURL, new String[]{"{appName} {oldVer} is out of date! Visit {updateURL} to download the latest release ({newVer})."}, new String[]{"{appName} {newVer} is out! Download the latest from {updateURL}"}, 5000);
    }

    public static boolean isCurrentVersion(String currentVersion, String newVersion) {
        return new NaturalOrderComparator().compare(currentVersion, newVersion) >= 0;
    }

    public static String[] loadTextFromURL(URL url, String[] defaultValue, int timeoutMS) {
        List<String> arraylist = new ArrayList<>();
        Scanner scanner;
        try {
            URLConnection uc = url.openConnection();
            uc.setReadTimeout(timeoutMS);
            uc.setConnectTimeout(timeoutMS);
            scanner = new Scanner(uc.getInputStream(), "UTF-8");
        } catch (Throwable e) {
            return defaultValue;
        }

        while (scanner.hasNextLine()) {
            arraylist.add(scanner.nextLine());
        }
        scanner.close();
        return arraylist.toArray(new String[0]);
    }

    public void checkVersionWithLogging() {
        if (!isCurrentVersion(currentVersion, newVersion))
            for (String msg : logMsg)
                System.out.println(msg);
    }

    public String[] getLogMessage() {
        return logMsg;
    }

    public void setLogMessage(String[] logMsg) {
        this.logMsg = logMsg;

        for (int i = 0; i < this.logMsg.length; i++)
            this.logMsg[i] = replaceAllTags(this.logMsg[i]);
    }

    public String[] getDialogMessage() {
        return dialogMsg;
    }

    public void setDialogMessage(String[] dialogMsg) {
        this.dialogMsg = dialogMsg;

        for (int i = 0; i < this.dialogMsg.length; i++)
            this.dialogMsg[i] = replaceAllTags(this.dialogMsg[i]);

    }

    public boolean isCurrentVersion() {
        return isCurrentVersion(runsSinceLastMessage == 0 ? currentVersion : lastNewVersionFound, newVersion);
    }

    private String replaceAllTags(String s) {
        return s.replace("{oldVer}", currentVersion).replace("{newVer}", newVersion).replace("{appName}", appName).replace("{updateURL}", updateURL);
    }
}
