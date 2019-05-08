package org.ea.sqrl.utils;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.content.IntentCompat;
import android.util.AndroidException;
import android.util.Log;

import com.google.zxing.FormatException;

import org.ea.sqrl.database.IdentityDBHelper;
import org.ea.sqrl.processors.SQRLStorage;

import java.util.Locale;

import static android.content.pm.PackageManager.GET_META_DATA;

/**
 *
 * @author Daniel Persson
 */
public class Utils {
    private static final String TAG = "EncryptionUtils";

    /**
     * Wrapper function if we ever need to read more than one byte segment in the future.
     *
     * @param data    String from the QR code read.
     * @return  The string without any extra information.
     */
    public static byte[] readSQRLQRCode(Intent data) throws FormatException {
        return data.getByteArrayExtra("SCAN_RESULT_BYTE_SEGMENTS_0");
    }

    public static String readSQRLQRCodeAsString(Intent data) {
        try {
            return new String(readSQRLQRCode(data), "ASCII");
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return "";
    }

    public static int getInteger(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException nfe) {
            return -1;
        }
    }

    public static void setLanguage(Context context) {
        String lang = getLanguage(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Resources res = context.getResources();
            android.content.res.Configuration conf = res.getConfiguration();
            Locale locale = new Locale(lang);

            conf.setLocale(locale);
            res.updateConfiguration(conf, res.getDisplayMetrics());
        }
    }

    public static String getLanguage(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        String lang = sharedPreferences.getString("language", "");
        return lang.isEmpty() ? Locale.getDefault().getLanguage() : lang;
    }

    public static void reloadActivityTitle(Activity activity) {
        try {
            int labelId = activity.getPackageManager().getActivityInfo(
                    activity.getComponentName(), GET_META_DATA).labelRes;
            if (labelId != 0) {
                activity.setTitle(labelId);
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void refreshStorageFromDb(Activity activity) throws Exception {
        SharedPreferences sharedPref = activity.getApplication().getSharedPreferences(
                "org.ea.sqrl.preferences",
                Context.MODE_PRIVATE
        );
        long currentId = sharedPref.getLong("current_id", 0);
        IdentityDBHelper aDbHelper = new IdentityDBHelper(activity);
        byte[] identityData = aDbHelper.getIdentityData(currentId);
        SQRLStorage sqrlStorage = SQRLStorage.getInstance(activity);
        sqrlStorage.read(identityData);
    }

    public static void restartApp(Context context) {
        PackageManager pm = context.getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage(context.getPackageName());
        if (intent == null) return;

        ComponentName componentName = intent.getComponent();
        Intent mainIntent = Intent.makeRestartActivityTask(componentName);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(mainIntent);

        if (context instanceof Activity) {
            ((Activity) context).finish();
        }
        Runtime.getRuntime().exit(0);
    }
}
