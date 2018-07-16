package org.ea.sqrl.utils;

import android.content.Intent;
import android.util.Log;

import com.google.zxing.FormatException;

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
}
