package org.ea.sqrl.utils;

import android.os.Build;
import android.util.Base64;
import android.util.Log;

import org.ea.sqrl.processors.EntropyHarvester;
import org.ea.sqrl.processors.ProgressionUpdater;
import org.libsodium.jni.NaCl;
import org.libsodium.jni.Sodium;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.BitSet;

/**
 *
 * @author Daniel Persson
 */
public class EncryptionUtils {
    private static final String TAG = "EncryptionUtils";
    private static final byte[] BASE56_ENCODE = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz".getBytes();

    public static byte[] combine(byte[] a, byte b) {
        return combine(a, new byte[] {b});
    }

    public static byte[] combine(byte[] a, byte[] b) {
        byte[] keys = new byte[a.length + b.length];
        System.arraycopy(a, 0, keys, 0, a.length);
        System.arraycopy(b, 0, keys, a.length, b.length);
        return keys;
    }

    /**
     * This function will reverse a byte stream so we get the least significant byte first.
     *
     * @param data  input data stream.
     * @return      reversed byte stream
     */
    private static byte[] reverse(byte[] data) {
        for(int i = 0; i < data.length / 2; i++) {
            byte temp = data[i];
            data[i] = data[data.length - i - 1];
            data[data.length - i - 1] = temp;
        }
        return data;
    }


    /**
     * This function will create an base56 string with the least significant byte first.
     * We use the reverse function every time we need to do math operations on the byte stream
     * in order to get the correct ordering on the bytes in our big integers.
     *
     * The string contains rows that each are 20 chars long. The first 19 characters come from the
     * base56 encoded byte stream and the last byte is a checksum. So we will take the 19 characters
     * of the line plus a zero based line number as our 20th character and then use a SHA-256 message
     * digest in order to get the bytes that we reverse and modulus with 56 in order to get the last
     * byte of the line.
     *
     * @param data          Input data stream
     * @return              String of base56 encoded with checksum for each line.
     * @throws Exception    Throws an exception if the platform doesn't support SHA-256.
     */
    public static String encodeBase56(byte[] data) throws Exception {
        data = reverse(data);
        BigInteger largeNum = new BigInteger(1, data);
        final BigInteger BASE = BigInteger.valueOf(56);
        String resultStr = "";
        int i = 0;
        byte line = 0;
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        while(!largeNum.equals(BigInteger.ZERO)) {
            if(i == 19) {
                md.update(line);
                byte[] checksum = reverse(md.digest());
                BigInteger reminder = new BigInteger(1, checksum).mod(BASE);
                resultStr += (char)BASE56_ENCODE[reminder.intValue()];
                md.reset();
                line++;
                i = 0;
            }
            BigInteger[] res = largeNum.divideAndRemainder(BASE);
            largeNum = res[0];
            resultStr += (char)BASE56_ENCODE[res[1].intValue()];
            md.update(BASE56_ENCODE[res[1].intValue()]);
            i++;
        }
        md.update(line);
        byte[] checksum = reverse(md.digest());
        BigInteger reminder = new BigInteger(1, checksum).mod(BASE);
        resultStr += (char)BASE56_ENCODE[reminder.intValue()];

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
        String stmp;
        for (int n = 0; n < b.length; n++)
        {
            stmp = Integer.toHexString(b[n] & 0XFF);
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
        return Base64.encodeToString(data, Base64.NO_PADDING + Base64.URL_SAFE + Base64.NO_WRAP);
    }

    public static byte[] decodeUrlSafe(String data) throws Exception {
        return Base64.decode(data, Base64.NO_PADDING + Base64.URL_SAFE + Base64.NO_WRAP);
    }

    public static byte[] xor(byte[] a, byte[] b) {
        byte[] result = new byte[a.length];
        for (int i = 0; i < a.length; i++) {
            result[i] = (byte) (((int) a[i]) ^ ((int) b[i]));
        }
        return result;
    }

    public static byte[] enHash(byte[] bytesToHash) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            bytesToHash = digest.digest(bytesToHash);
            byte[] xorKey = Arrays.copyOf(bytesToHash, bytesToHash.length);

            for(int i=0; i<15; i++) {
                bytesToHash = digest.digest(bytesToHash);
                xorKey = xor(xorKey, bytesToHash);
            }

            return xorKey;

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return new byte[0];
        }
    }

    public static byte[] enSCryptIterations(String password, byte[] randomSalt, int logNFactor, int dkLen, int iterationCount, ProgressionUpdater progressionUpdater) throws Exception {
        progressionUpdater.startTimer();

        byte[] key = new byte[dkLen];
        byte[] pwdBytes = password.getBytes();
        Sodium.crypto_pwhash_scryptsalsa208sha256_ll(pwdBytes, pwdBytes.length, randomSalt, randomSalt.length, 1 << logNFactor, 256, 1, key, key.length);

        progressionUpdater.endTimer();
        progressionUpdater.incrementProgress();

        byte[] xorKey = Arrays.copyOf(key, key.length);
        for(int i = 1; i < iterationCount; i++) {
            Sodium.crypto_pwhash_scryptsalsa208sha256_ll(pwdBytes, pwdBytes.length, key, key.length, 1 << logNFactor, 256, 1, key, key.length);
            xorKey = xor(key, xorKey);
            progressionUpdater.incrementProgress();
        }

        return xorKey;
    }

    private static byte[] getIntToFourBytes(int input) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(input).array();
    }


    public static byte[] enSCryptTime(String password, byte[] randomSalt, int logNFactor, int dkLen, byte secondsToRun, ProgressionUpdater progressionUpdater) throws Exception {
        long startTime = System.currentTimeMillis();
        progressionUpdater.setMax(secondsToRun & 0xFF);

        byte[] key = new byte[dkLen];
        byte[] pwdBytes = password.getBytes();

        Sodium.crypto_pwhash_scryptsalsa208sha256_ll(pwdBytes, pwdBytes.length, randomSalt, randomSalt.length, 1 << logNFactor, 256, 1, key, key.length);

        int iterationCount = 1;
        long time = System.currentTimeMillis() - startTime;
        byte[] xorKey = Arrays.copyOf(key, key.length);
        while(time < ((secondsToRun & 0xFF) * 1000)) {
            Sodium.crypto_pwhash_scryptsalsa208sha256_ll(pwdBytes, pwdBytes.length, key, key.length, 1 << logNFactor, 256, 1, key, key.length);
            xorKey = xor(key, xorKey);
            time = System.currentTimeMillis() - startTime;
            progressionUpdater.setTimeDone(time);
            iterationCount++;
        }
        progressionUpdater.incrementProgress();

        return EncryptionUtils.combine(getIntToFourBytes(iterationCount), xorKey);
    }


    public static String bitsToString(BitSet b) {
        StringBuilder s = new StringBuilder();
        for( int i = 0; i < b.length();  i++ ) {
            s.append( b.get(i) ? 1 : 0 );
        }
        return s.toString();
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

    public static void main(String[] args) {
        try {
            EntropyHarvester entropyHarvester = EntropyHarvester.getInstance();
            byte[] rescueCodeBytes = new byte[12];
            entropyHarvester.fetchRandom(rescueCodeBytes);

            BigInteger rescueCodeNum = new BigInteger(1, rescueCodeBytes);
            String rescueCode = rescueCodeNum.toString(10).substring(rescueCodeNum.toString(10).length() - 24);
            System.out.println(Arrays.toString(rescueCode.split("(?<=\\G.{4})")));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
