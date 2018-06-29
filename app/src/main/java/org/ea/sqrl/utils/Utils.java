package org.ea.sqrl.utils;

import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;

import com.google.zxing.FormatException;
import com.google.zxing.common.BitSource;
import com.google.zxing.qrcode.decoder.Mode;
import com.google.zxing.qrcode.decoder.Version;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Daniel Persson
 */
public class Utils {
    private static final String TAG = "EncryptionUtils";

    /**
     * Wrapper function if we ever need to read more than one byte segment in the future.
     *
     * @param rawHexData    String from the QR code read.
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

    public static void showToast(View relativeLayout, int id) {
        Snackbar snackbar = Snackbar.make(relativeLayout, id, Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction("dismiss", v -> snackbar.dismiss());
    }

    public static void showToast(View relativeLayout, String message) {
        Snackbar snackbar = Snackbar.make(relativeLayout, message, Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction("dismiss", v -> snackbar.dismiss());
    }
}
