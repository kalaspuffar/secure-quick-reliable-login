package org.ea.sqrl.utils;

import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;

/**
 *
 * @author Daniel Persson
 */
public class Utils {
    private static final String TAG = "EncryptionUtils";

    /**
     * We look into the string from the QRCode after either sqrldata, sqrl:// or qrl:// to start
     * our string.
     * Then we look for the ec11 that padds the QRCode in order to remove that. Lastly we remove
     * all trailing zeros.
     *
     * @param rawHexData    String from the QR code read.
     * @return  The string without any extra information.
     */
    public static byte[] readSQRLQRCode(byte[] rawHexData) {
        String string = EncryptionUtils.byte2hex(rawHexData);
        int start = string.indexOf("7371726c64617461");
        if(start == -1) {
            start = string.indexOf("7371726c3a2f2f");
        }
        if(start == -1) {
            start = string.indexOf("71726c3a2f2f");
        }
        if(start == -1) return new byte[0];

        int end = string.lastIndexOf("ec11");

        if(end == -1) end = string.length();

        string = string.substring(start, end);
        while(string.endsWith("ec11")) {
            string = string.substring(0, string.length()-4);
        }
        while(string.endsWith("0")) {
            string = string.substring(0, string.length()-1);
        }
        return EncryptionUtils.hex2Byte(string);
    }

    public static String readSQRLQRCodeAsString(byte[] rawHexData) {
        try {
            return new String(readSQRLQRCode(rawHexData), "ASCII");
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
