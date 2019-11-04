package org.ea.sqrl.utils;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;

import org.ea.sqrl.R;
import org.ea.sqrl.activites.MainActivity;
import org.ea.sqrl.activites.ClearQuickPassActivity;
import org.ea.sqrl.activites.EnableQuickPassActivity;
import org.ea.sqrl.activites.LoginActivity;
import org.ea.sqrl.database.IdentityDBHelper;
import org.ea.sqrl.processors.EntropyHarvester;
import org.ea.sqrl.processors.SQRLStorage;

import java.util.Arrays;
import java.util.Map;


public class SqrlApplication extends Application {
    private static final String TAG = "SqrlApplication";
    public static final String APPS_PREFERENCES = "org.ea.sqrl.preferences";
    public static final String CURRENT_ID = "current_id";
    public static String EXTRA_NEXT_ACTIVITY = "next_activity";

    static ShortcutInfo scanShortcut;
    static ShortcutInfo logonShortcut;
    static ShortcutInfo clearQuickPassShortcut;

    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        configureShortcuts(getApplicationContext());
        setApplicationShortcuts(getApplicationContext());
        try {
            long currentId = getCurrentId(getApplicationContext());
            if (currentId > 0) {
                SQRLStorage.getInstance(getApplicationContext()).read(IdentityDBHelper.getInstance(getApplicationContext()).getIdentityData(currentId));
            }
            EntropyHarvester.getInstance();
        } catch (Exception e) {
            Log.e(TAG, "Failed to get initiate EntropyHarvester or SQRLStorage.");
        }
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
            Intent intentQuickScan = new Intent(context, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    .setAction(MainActivity.ACTION_QUICK_SCAN);
            scanShortcut = new ShortcutInfo.Builder(context, "scanQrWeb")
                    .setShortLabel(context.getString(R.string.scan_qr_code))
                    .setLongLabel(context.getString(R.string.scan_qr_code_long))
                    .setIcon(Icon.createWithResource(context, R.drawable.ic_scan_qr_black_24dp))
                    .setIntent(intentQuickScan)
                    .build();

            Intent intentLogon = new Intent(context, EnableQuickPassActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    .setAction(LoginActivity.ACTION_QUICKPASS_OPERATION);
            Intent[] logonIntentList = {intentLogon};
            logonShortcut = new ShortcutInfo.Builder(context, "setQuickpass")
                    .setShortLabel(context.getString(R.string.set_quickpass))
                    .setLongLabel(context.getString(R.string.set_quickpass_long))
                    .setIcon(Icon.createWithResource(context, R.drawable.ic_sqrl_icon_outof_safe_vector_outline))
                    .setIntents(logonIntentList)
                    .build();

            Intent mainActivity = new Intent(context, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .setAction("android.intent.action.MAIN");
            Intent intentClearQuickpass = new Intent(context, ClearQuickPassActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    .setAction(ClearQuickPassActivity.ACTION_CLEAR_QUICK_PASS);
            Intent[] clearQuickPassIntentList = {mainActivity, intentClearQuickpass};
            clearQuickPassShortcut = new ShortcutInfo.Builder(context, "clearQuickpass")
                    .setShortLabel(context.getString(R.string.clear_quickpass))
                    .setLongLabel(context.getString(R.string.clear_quickpass_long))
                    .setIcon(Icon.createWithResource(context, R.drawable.ic_sqrl_icon_into_safe_vector_outline))
                    .setIntents(clearQuickPassIntentList)
                    .build();
        }
    }

    /**
     * Gets the currently active identity id from the app preferences.
     *
     * @param context    The context of the caller.
     * @return           Returns the currently active identity id, or 0 if non is set.
     */
    public static long getCurrentId(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(APPS_PREFERENCES, Context.MODE_PRIVATE);
        return sharedPref.getLong(CURRENT_ID, 0);
    }

    /**
     * Saves the provided identity id as the currently active id in the app preferences.
     *
     * @param application The caller's application object.
     */
    public static void saveCurrentId(Application application, long newIdentityId) {
        SharedPreferences sharedPref = application.getSharedPreferences(APPS_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putLong(CURRENT_ID, newIdentityId);
        editor.apply();
    }

    /**
     * Reloads the sqrl storage with the provided identity and updates the currently active
     * identity id in the app preferences accordingly.
     *
     * @param context   The context of the caller.
     * @param id        The id of the identity which should be set as currently active.
     *                  Set this to -1 to select the first available identity.
     */
    public static void setCurrentId(Context context, long id) {
        IdentityDBHelper dbHelper = IdentityDBHelper.getInstance(context);

        if (id == -1) {
            Map<Long,String> identities = dbHelper.getIdentities();
            if (identities.size() > 0) {
                id = identities.keySet().iterator().next();
            }
        }

        if (id == -1) return;

        SqrlApplication.saveCurrentId((Application) context.getApplicationContext(), id);

        SQRLStorage storage = SQRLStorage.getInstance(context.getApplicationContext());
        byte[] identityData = dbHelper.getIdentityData(id);

        if(storage.needsReload(identityData)) {
            storage.clearQuickPass();
            try {
                storage.read(identityData);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
    }
}
