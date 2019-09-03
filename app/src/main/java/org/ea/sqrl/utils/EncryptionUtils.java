package org.ea.sqrl.utils;

import android.util.Base64;
import android.util.Log;

import org.ea.sqrl.processors.ProgressionUpdater;
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
    private static final String BASE56_DECODE = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz";
    private static final BigInteger BASE = BigInteger.valueOf(56);

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
        String resultStr = "";
        int i = 0;
        int len = 0;
        byte line = 0;
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        while(!largeNum.equals(BigInteger.ZERO)) {
            if(i == 19) {
                md.update(line);
                byte[] checksum = reverse(md.digest());
                BigInteger remainder = new BigInteger(1, checksum).mod(BASE);
                resultStr += (char)BASE56_ENCODE[remainder.intValue()];
                md.reset();
                line++;
                i = 0;
            }
            BigInteger[] res = largeNum.divideAndRemainder(BASE);
            largeNum = res[0];
            resultStr += (char)BASE56_ENCODE[res[1].intValue()];
            md.update(BASE56_ENCODE[res[1].intValue()]);
            i++;
            len++;
        }

        // Added extra padding.
        int paddLen = (int)Math.ceil( data.length * 8.0 / (Math.log(56) / Math.log(2)));
        for(int j = len; j < paddLen; j++) {
            resultStr += (char)BASE56_ENCODE[0];
            md.update(BASE56_ENCODE[0]);
        }

        md.update(line);
        byte[] checksum = reverse(md.digest());
        BigInteger remainder = new BigInteger(1, checksum).mod(BASE);
        resultStr += (char)BASE56_ENCODE[remainder.intValue()];

        return resultStr;
    }

    public static byte[] decodeBase56(String encodedString) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        int i = 0;
        int n = 0;
        byte line = 0;
        BigInteger largeNum = BigInteger.ZERO;
        int encodedStringLen = encodedString.length();
        for(String s : encodedString.split("")) {
            if(s.isEmpty()) continue;
            if(i == 19 || encodedStringLen - 1 == n + line) {
                md.update(line);
                byte[] checksum = reverse(md.digest());
                BigInteger remainder = new BigInteger(1, checksum).mod(BASE);
                if(s.getBytes()[0] != BASE56_ENCODE[remainder.intValue()]) {
                    throw new Exception("" + line + 1);
                }
                md.reset();
                line++;
                i = 0;
            } else {
                BigInteger newVal = BigInteger.valueOf(BASE56_DECODE.indexOf(s)).multiply(BASE.pow(n));
                largeNum = largeNum.add(newVal);
                md.update(s.getBytes());
                i++;
                n++;
            }
        }
        byte[] largeBytes = largeNum.toByteArray();

        return reverse(largeBytes);
    }

    public static int validateBase56(String cleanTextIdentity) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return -1;
        }
        int i = 0;
        int n = 0;
        byte line = 0;
        int encodedStringLen = cleanTextIdentity.length();
        for (String s : cleanTextIdentity.split("")) {
            if (s.isEmpty()) continue;
            if (i == 19 || encodedStringLen - 1 == n + line) {
                md.update(line);
                byte[] checksum = reverse(md.digest());
                BigInteger remainder = new BigInteger(1, checksum).mod(BASE);
                if (s.getBytes()[0] != BASE56_ENCODE[remainder.intValue()]) {
                    return line;
                }
                md.reset();
                line++;
                i = 0;
            } else {
                md.update(s.getBytes());
                i++;
                n++;
            }
        }
        return -1;
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

    public static String decodeUrlSafeString(String data) throws Exception {
        return new String(decodeUrlSafe(data), "UTF-8");
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


    public static void main(String[] args) {
        try {

            String s = "KB3Z 8My9 CVUX C34K B8NM" +
                       "bEXm jcUK RAq6 JT9p DaYW" +
                       "UstG K9hH xX98 KxHU weVa" +
                       "RvpV wJd5 JXbf eEDf 4cYy" +
                       "hzNL j6dW Ehq3 KXCV YSBf" +
                       "nRJd rAN";
            String encodedString = s.replaceAll("[^2-9a-zA-Z]", "");
            byte[] decodedBytes = decodeBase56(encodedString);
/*
            EntropyHarvester entropyHarvester = EntropyHarvester.getInstance();

            byte[] storageBytes = new byte[223];
            entropyHarvester.fetchRandom(storageBytes);

            System.out.println(byte2hex(storageBytes));

            String encodedString = encodeBase56(storageBytes);

            byte[] decodedBytes = decodeBase56(encodedString);

            System.out.println(byte2hex(decodedBytes));

            String decodedString = encodeBase56(decodedBytes);

            System.out.println(encodedString);
            System.out.println(decodedString);
*/
            /*
            byte[] rescueCodeBytes = new byte[12];
            entropyHarvester.fetchRandom(rescueCodeBytes);

            BigInteger rescueCodeNum = new BigInteger(1, rescueCodeBytes);
            String rescueCode = rescueCodeNum.toString(10).substring(rescueCodeNum.toString(10).length() - 24);
            System.out.println(Arrays.toString(rescueCode.split("(?<=\\G.{4})")));
            */
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
