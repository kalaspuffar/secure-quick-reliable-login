package org.ea.sqrl.utils;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.SimplifiedActivity;
import org.ea.sqrl.activites.account.ClearQuickPassActivity;
import org.ea.sqrl.activites.account.EnableQuickPassActivity;
import org.ea.sqrl.processors.SQRLStorage;

import java.util.Arrays;


public class SqrlApplication extends Application {
    private static final String TAG = "SqrlApplication";
    public static final String APPS_PREFERENCES = "org.ea.sqrl.preferences";
    public static final String CURRENT_ID = "current_id";

    static ShortcutInfo scanShortcut;
    static ShortcutInfo logonShortcut;
    static ShortcutInfo clearQuickPassShortcut;

    @Override
    public void onCreate() {
        super.onCreate();
        configureShortcuts(getApplicationContext());
        setApplicationShortcuts(getApplicationContext());
    }

    public static void setApplicationShortcuts(Context context) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            SQRLStorage sqrlStorage = SQRLStorage.getInstance(context);
            if (getCurrentId(context) > 0) {
                ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
                if (sqrlStorage.hasQuickPass()) {
                    shortcutManager.setDynamicShortcuts(Arrays.asList(scanShortcut, clearQuickPassShortcut));
                } else {
                    shortcutManager.setDynamicShortcuts(Arrays.asList(scanShortcut, logonShortcut));
                }
            }
        }
    }

    public static void configureShortcuts(Context context) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent simplifiedActivity = new Intent(context, SimplifiedActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .setAction("android.intent.action.MAIN");

            Intent intentQuickScan = new Intent(context, SimplifiedActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    .setAction(SimplifiedActivity.ACTION_QUICK_SCAN);
            scanShortcut = new ShortcutInfo.Builder(context, "scanQrWeb")
                    .setShortLabel("Scan QR Code")
                    .setLongLabel("Scan Web QR Code for Login")
                    .setIcon(Icon.createWithResource(context, R.drawable.ic_scan_qr_black_24dp))
                    .setIntent(intentQuickScan)
                    .build();

            Intent intentLogon = new Intent(context, EnableQuickPassActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    .setAction(SimplifiedActivity.ACTION_LOGON);
            Intent[] logonIntentList = {simplifiedActivity, intentLogon};
            logonShortcut = new ShortcutInfo.Builder(context, "setQuickpass")
                    .setShortLabel("Set QuickPass")
                    .setLongLabel("Enter SQRL password to engage QuickPass")
                    .setIcon(Icon.createWithResource(context, R.drawable.ic_sqrl_icon_outof_safe_vector_outline))
                    .setIntents(logonIntentList)
                    .build();

            Intent intentClearQuickpass = new Intent(context, ClearQuickPassActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    .setAction(ClearQuickPassActivity.ACTION_CLEAR_QUICK_PASS);
            Intent[] clearQuickPassIntentList = {simplifiedActivity, intentClearQuickpass};
            clearQuickPassShortcut = new ShortcutInfo.Builder(context, "clearQuickpass")
                    .setShortLabel("Clear QuickPass")
                    .setLongLabel("Clear QuickPass State Immediately")
                    .setIcon(Icon.createWithResource(context, R.drawable.ic_sqrl_icon_into_safe_vector_outline))
                    .setIntents(clearQuickPassIntentList)
                    .build();
        }
    }

    public static long getCurrentId(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(APPS_PREFERENCES, Context.MODE_PRIVATE);
        return sharedPref.getLong(CURRENT_ID, 0);
    }
}
