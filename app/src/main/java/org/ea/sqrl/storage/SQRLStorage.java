package org.ea.sqrl.storage;

import android.os.Build;
import android.os.Handler;
import android.util.Base64;
import android.widget.ProgressBar;

import org.ea.sqrl.ProgressionUpdater;

import java.io.File;
import java.io.FileInputStream;
import java.security.Key;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class SQRLStorage {
    private static final String STORAGE_HEADER = "sqrldata";
    private static final int PASSWORD_PBKDF = 1;
    private static final int RESCUECODE_PBKDF = 2;
    private static final int PREVIOUS_IDENTITY_KEYS = 3;
    private ProgressionUpdater progressionUpdater;

    private static SQRLStorage instance = null;

    private SQRLStorage() {
        System.setProperty("com.lambdaworks.jni.loader", "nil");
    }

    public static SQRLStorage getInstance() {
        if(instance == null) {
            instance = new SQRLStorage();
        }
        return instance;
    }

    public void read(byte[] input, boolean full) throws Exception {
        String header = new String(Arrays.copyOfRange(input, 0, 8));


        if (!STORAGE_HEADER.equals(header)) throw new Exception("Incorrect header");
        int readOffset = 8;
        int readLen = readOffset + 2;
        while (input.length > readLen) {
            int headerFix = readOffset > 0 ? 0 : 8;
            int len = getIntFromTwoBytes(input, readOffset + headerFix);
            if (readOffset + len > input.length)
                throw new Exception(
                        "Incorrect length of block offset " + readOffset + " len " + len + " input len "+input.length
                );

            handleBlock(Arrays.copyOfRange(input, readOffset + headerFix, readOffset + len - headerFix));
            readOffset += len;
            readLen = readOffset + 2;
        }
    }

    /**
     * Password block.
     */
    private int plaintextLength;
    private byte[] plaintext;
    private byte[] initializationVector;
    private byte[] randomSalt;
    private int   logNFactor;
    private int iterationCount;
    private int optionFlags;
    private byte hintLength;
    private byte timeInSecondsToRunPWEnScryptOnPassword;
    private int idleTimoutInMinutes;
    private byte[] identityMasterKeyEncrypted;
    private byte[] identityLockKeyEncrypted;
    private byte[] verificationTag;

    private byte[] identityMasterKey;
    private byte[] identityLockKey;


    public void handlePasswordBlock(byte[] input) {
        plaintextLength = getIntFromTwoBytes(input, 4);
        plaintext = Arrays.copyOfRange(input, 0, plaintextLength);
        initializationVector = Arrays.copyOfRange(input, 6, 18);
        randomSalt = Arrays.copyOfRange(input, 18, 34);
        logNFactor = input[34];
        iterationCount = getIntFromFourBytes(input, 35);
        optionFlags = getIntFromTwoBytes(input, 39);
        hintLength = input[41];
        timeInSecondsToRunPWEnScryptOnPassword = input[42];
        idleTimoutInMinutes = getIntFromTwoBytes(input, 43);
        identityMasterKeyEncrypted = Arrays.copyOfRange(input, 45, 77);
        identityLockKeyEncrypted = Arrays.copyOfRange(input, 77, 109);
        verificationTag = Arrays.copyOfRange(input, 109, 125);
    }


    private byte[] rescue_plaintext;
    private byte[] rescue_randomSalt;
    private byte   rescue_logNFactor;
    private int rescue_iterationCount;
    private byte[] rescue_identityLockKeyEncrypted;
    private byte[] rescue_identityLockKey;
    private byte[] rescue_verificationTag;

    public void handleIdentityBlock(byte[] input) {
        rescue_plaintext = Arrays.copyOfRange(input, 0, 25);
        rescue_randomSalt = Arrays.copyOfRange(input, 4, 20);
        rescue_logNFactor = input[20];
        rescue_iterationCount = getIntFromFourBytes(input, 21);
        rescue_identityLockKeyEncrypted = Arrays.copyOfRange(input, 25, 57);
        rescue_verificationTag = Arrays.copyOfRange(input, 57, 73);
    }

    private byte[] previous_plaintext;
    private int previous_countOfKeys;
    private byte[] previous_key1;
    private byte[] previous_key2;
    private byte[] previous_key3;
    private byte[] previous_key4;
    private byte[] previous_verificationTag;

    public void handlePreviousIdentityBlock(byte[] input, int len) {
        previous_plaintext = Arrays.copyOfRange(input, 0, 6);
        previous_countOfKeys = getIntFromTwoBytes(input, 4);

        int lastKeyEnd = 6 + 32;
        previous_key1 = Arrays.copyOfRange(input, 6, lastKeyEnd);
        if(previous_countOfKeys > 1) {
            previous_key2 = Arrays.copyOfRange(input, lastKeyEnd, lastKeyEnd + 32);
            lastKeyEnd += 32;
        }
        if(previous_countOfKeys > 2) {
            previous_key3 = Arrays.copyOfRange(input, lastKeyEnd, lastKeyEnd + 32);
            lastKeyEnd += 32;
        }
        if(previous_countOfKeys > 3) {
            previous_key4 = Arrays.copyOfRange(input, lastKeyEnd, lastKeyEnd + 32);
            lastKeyEnd += 32;
        }
        previous_verificationTag = Arrays.copyOfRange(input, lastKeyEnd, lastKeyEnd + 16);
    }


    public void handleBlock(byte[] input) throws Exception {
        int len = getIntFromTwoBytes(input, 0);
        int type = getIntFromTwoBytes(input, 2);

        switch (type) {
            case PASSWORD_PBKDF:
                handlePasswordBlock(input);
                break;
            case RESCUECODE_PBKDF:
                handleIdentityBlock(input);
                break;
            case PREVIOUS_IDENTITY_KEYS:
                handlePreviousIdentityBlock(input, len);
                break;
            default:
                throw new Exception("Unknown type "+type);
        }
    }

    private int getIntFromTwoBytes(byte[] input, int offset) {
        return (input[offset] & 0xff) | ((input[offset + 1] & 0xff) << 8);
    }

    private int getIntFromFourBytes(byte[] input, int offset) {
        return (input[offset] & 0xff) | ((input[offset + 1] & 0xff) << 8) | (input[offset + 2] & 0xff) << 16 | ((input[offset + 3] & 0xff) << 24);
    }

    /**
     * Decrypt the identity key, this has the master key used to login to sites and also the lock
     * key that we supply to the sites in order to unlock at a later date if the master key ever
     * gets compromised.
     *
     * @param password  Password used to unlock the master key.
     */
    public void decryptIdentityKey(String password) {
        if(Build.VERSION.BASE_OS != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        this.progressionUpdater.setMax(iterationCount);

        try {
            byte[] key = EncryptionUtils.enSCrypt(password, randomSalt, logNFactor, 32, iterationCount, this.progressionUpdater);
            System.out.println(EncryptionUtils.byte2hex(key));
            //byte[] key = EncryptionUtils.hex2Byte("a8694c73b0d6c7d6e93eda31552118ce0d9a5d5168170bd2b7123852c18cb14a");

            Key keySpec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES_256/GCM/NoPadding");
            GCMParameterSpec params = new GCMParameterSpec(128, initializationVector);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, params);
            cipher.updateAAD(plaintext);
            cipher.update(identityMasterKeyEncrypted);
            cipher.update(identityLockKeyEncrypted);
            byte[] decryptionResult = cipher.doFinal(verificationTag);
            identityMasterKey = Arrays.copyOfRange(decryptionResult, 0, 32);
            identityLockKeyEncrypted = Arrays.copyOfRange(decryptionResult, 32, 64);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This unlocks the unlock key used to recover your identity if your master key gets comprimised
     * this key should NEVER be saved in the device. It's just used to create a new identity.
     *
     * @param rescueCode    Special rescueCode printed on paper in the format of 0000-0000-0000-0000-0000-0000
     */
    public void decryptUnlockKey(String rescueCode) {
        if(Build.VERSION.BASE_OS != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        this.progressionUpdater.setMax(rescue_iterationCount);
        rescueCode = rescueCode.replaceAll("-", "");

        try {
            byte[] key = EncryptionUtils.enSCrypt(rescueCode, rescue_randomSalt, rescue_logNFactor, 32, rescue_iterationCount, this.progressionUpdater);
            System.out.println(EncryptionUtils.byte2hex(key));
            //byte[] key = EncryptionUtils.hex2Byte("8ea530fa2e42a3bd8379e115c2b94fcf9e784e3720519611ab9db068277e2b7b");

            byte[] nullBytes = new byte[12];
            Arrays.fill(nullBytes, (byte)0);

            Key keySpec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES_256/GCM/NoPadding");
            GCMParameterSpec params = new GCMParameterSpec(128, nullBytes);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, params);
            cipher.updateAAD(rescue_plaintext);
            cipher.update(rescue_identityLockKeyEncrypted);
            rescue_identityLockKey = cipher.doFinal(rescue_verificationTag);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return STORAGE_HEADER;
    }


    public static void main(String[] args) {
        try {
            File file = new File("Testing.sqrl");
            byte[] bytesArray = new byte[(int) file.length()];

            FileInputStream fis = new FileInputStream(file);
            fis.read(bytesArray);
            fis.close();

            System.out.println(EncryptionUtils.byte2hex(bytesArray));
            SQRLStorage storage = SQRLStorage.getInstance();
            storage.read(bytesArray, true);
            storage.decryptIdentityKey("Testing1234");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setProgressionUpdater(ProgressionUpdater progressionUpdater) {
        this.progressionUpdater = progressionUpdater;
    }

    public byte[] getPrivateKey(String domain) throws Exception {
        final Mac HMacSha256 = Mac.getInstance("HmacSHA256");
        final SecretKeySpec key = new SecretKeySpec(this.identityMasterKey, "HmacSHA256");
        HMacSha256.init(key);
        return HMacSha256.doFinal(domain.getBytes());
    }

}

/*
        sqrldata – lowercase signature means binary follows             8 bytes
            {length = 125} – inclusive length of entire outer block     2 bytes
            {type = 1} – user access password protected data            2 bytes
            {pt length = 45} – inclusive length of entire inner block   2 bytes
            {aes-gcm iv} – initialization vector for auth/encrypt       12 bytes
            {scrypt random salt} – update for password change           16 bytes
            {scrypt log(n-factor)} – memory consumption factor          1 byte
            {scrypt iteration count} – time consumption factor          4 bytes
            {option flags} – 16 binary flags                            2 bytes
            {hint length} – number of chars in hint                     1 byte
            {pw verify sec} – seconds to run PW EnScrypt                1 byte
            {idle timeout min} – idle minutes before wiping PW          2 bytes
        {encrypted identity master key (IMK)}                           32 bytes
        {encrypted identity lock key (ILK)}                             32 bytes
        {verification tag}                                              16 bytes
        {length = 73}                                                   2 bytes
        {type = 2} – rescue code data                                   2 bytes
        {scrypt random salt}                                            16 bytes
        {scrypt log(n-factor)}                                          1 byte
        {scrypt iteration count}                                        4 bytes
        {encrypted identity unlock key (IUK)}                           32 bytes
        {verification tag}                                              16 bytes
        {length = 54, 86, 118 or 150}                                   2 bytes
        {type = 3} – previous identity unlock keys                      2 bytes
        {edition >= 1} – count of all previous keys                     2 bytes
        {encrypted previous IUK}                                        32 bytes
        {encrypted next older IUK (if present)}                         32 bytes
        {encrypted next older IUK (if present)}                         32 bytes
        {encrypted oldest previous IUK (if present)}                    32 bytes
        {verification tag}                                              16 bytes
*/