package org.ea.sqrl.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;

import com.google.zxing.FormatException;

import java.util.Locale;

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
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        String lang = sharedPreferences.getString("language", "");

        lang = lang.isEmpty() ? Locale.getDefault().getLanguage() : lang;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Resources res = context.getResources();

            DisplayMetrics dm = res.getDisplayMetrics();
            android.content.res.Configuration conf = res.getConfiguration();
            conf.setLocale(new Locale(lang));
            res.updateConfiguration(conf, dm);
        }
    }
}
