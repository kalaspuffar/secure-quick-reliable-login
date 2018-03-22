package org.ea.sqrl;

import android.support.test.espresso.accessibility.AccessibilityChecks;

/**
 * Created by danielp on 3/22/18.
 */

public class AccessabilitySingelton {
    private static AccessabilitySingelton instance;

    private AccessabilitySingelton() {
        AccessibilityChecks.enable().setRunChecksFromRootView(true);
    }

    public static void getInstance() {
        if(instance == null) {
            instance = new AccessabilitySingelton();
        }
    }
}
