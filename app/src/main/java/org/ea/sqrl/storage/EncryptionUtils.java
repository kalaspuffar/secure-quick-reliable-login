package org.ea.sqrl.storage;

import android.os.Build;
import android.os.Handler;
import android.util.Base64;
import android.widget.ProgressBar;

import com.lambdaworks.crypto.SCrypt;

import org.ea.sqrl.ProgressionUpdater;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 * Created by woden on 2018-01-25.
 */

public class EncryptionUtils {
    private static byte[] BASE56_ENCODE = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz".getBytes();

    public static String encodeBase56(byte[] data) throws Exception {
        BigInteger largeNum = new BigInteger(data);
        final BigInteger BASE = new BigInteger("56");
        String resultStr = "";
        int i = 0;
        MessageDigest md = MessageDigest.getInstance("SHA256");
        while(!largeNum.equals(BigInteger.ZERO)) {
            BigInteger[] res = largeNum.divideAndRemainder(BASE);
            largeNum = res[0];
            resultStr += (char)BASE56_ENCODE[res[1].intValue()];
            md.update(BASE56_ENCODE[res[1].intValue()]);
            if(i == 19) {
                md.update((byte)0);
                BigInteger reminder = new BigInteger(md.digest()).mod(BASE);
                resultStr += (char)BASE56_ENCODE[reminder.intValue()];
                i = 0;
            }
        }
        return resultStr;
    }


    public static byte[] hex2Byte(String str) {
        byte[] bytes = new byte[str.length() / 2];
        for (int i = 0; i < bytes.length; i++)
        {
            bytes[i] = (byte) Integer.parseInt(str.substring(2 * i, 2 * i + 2), 16);
        }
        return bytes;
    }

    public static String byte2hex(byte[] b) {
        String hs = "";
        String stmp = "";
        for (int n = 0; n < b.length; n++)
        {
            stmp = (java.lang.Integer.toHexString(b[n] & 0XFF));
            if (stmp.length() == 1) {
                hs = hs + "0" + stmp;
            } else {
                hs = hs + stmp;
            }
            if (n < b.length - 1) {
                hs = hs + "";
            }
        }
        return hs;
    }

    public static String encodeUrlSafe(byte[] data) throws Exception {
        if(Build.VERSION.BASE_OS != null) {
            return Base64.encodeToString(data, Base64.NO_PADDING + Base64.URL_SAFE + Base64.NO_WRAP);
        } else {
            return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(data);
        }
    }

    public static byte[] decodeUrlSafe(String data) throws Exception {
        if(Build.VERSION.BASE_OS != null) {
            return Base64.decode(data, Base64.NO_PADDING + Base64.URL_SAFE + Base64.NO_WRAP);
        } else {
            return java.util.Base64.getUrlDecoder().decode(data);
        }
    }

    public static byte[] xor(byte[] a, byte[] b) {
        byte[] result = new byte[a.length];
        for (int i = 0; i < a.length; i++) {
            result[i] = (byte) (((int) a[i]) ^ ((int) b[i]));
        }
        return result;
    }

    public static byte[] enSCrypt(String password, byte[] randomSalt, int logNFactor, int dkLen, int iterationCount, ProgressionUpdater progressionUpdater) throws Exception {
        progressionUpdater.startTimer();
        byte[] key = SCrypt.scrypt(password.getBytes(), randomSalt, 1 << logNFactor, 256, 1, dkLen);
        progressionUpdater.endTimer();

        progressionUpdater.incrementProgress();

        byte[] xorKey = Arrays.copyOf(key, key.length);
        for(int i = 1; i < iterationCount; i++) {
            key = SCrypt.scrypt(password.getBytes(), key, 1 << logNFactor, 256, 1, dkLen);
            xorKey = xor(key, xorKey);
            progressionUpdater.incrementProgress();
        }

        return xorKey;
    }

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
        int end = string.lastIndexOf("ec11");
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
            e.printStackTrace();
        }
        return "";
    }
}
