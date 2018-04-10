package org.ea.sqrl.processors;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import org.ea.sqrl.activites.BaseActivity;
import org.ea.sqrl.jni.Grc_aesgcm;
import org.ea.sqrl.utils.EncryptionUtils;
import org.libsodium.jni.NaCl;
import org.libsodium.jni.Sodium;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.Key;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * This class handles the S4 Storage data. We can load identities from disk, QRCode or pure
 * text in order to decrypt and handle your identities. We also provide secure keys to use
 * for logging in and creating accounts.
 *
 * @author Daniel Persson
 */
public class SQRLStorage {
    private static final String TAG = "SQRLStorage";
    private static final String STORAGE_HEADER = "sqrldata";
    private static final int PASSWORD_PBKDF = 1;
    private static final int RESCUECODE_PBKDF = 2;
    private static final int PREVIOUS_IDENTITY_KEYS = 3;
    private static final int HEADER_LENGTH = 8;
    private static final int BLOCK_LENGTH_SIZE = 2;
    private ProgressionUpdater progressionUpdater;
    private int passwordBlockLength = 0;
    private static SQRLStorage instance = null;

    private byte[] quickPassKey;
    private byte[] quickPassKeyEncrypted;
    private byte[] quickPassRandomSalt;
    private byte[] quickPassVerificationTag;
    private int quickPassIterationCount;
    private byte[] quickPassInitializationVector;

    private byte[] tempRescueCode;

    private boolean hasIdentityBlock = false;
    private boolean hasRescueBlock = false;
    private boolean hasPreviousBlock = false;
    private int previousKeyIndex = 0;

    private SQRLStorage() {
        Grc_aesgcm.gcm_initialize();
        NaCl.sodium();
    }

    public static SQRLStorage getInstance() {
        if(instance == null) {
            instance = new SQRLStorage();
        }
        return instance;
    }

    public boolean hasMorePreviousKeys() {
        return previousKeyIndex < previousCountOfKeys;
    }

    public void increasePreviousKeyIndex() {
        previousKeyIndex++;
    }

    public void newRescueCode(EntropyHarvester entropyHarvester) {
        tempRescueCode = new byte[12];
        entropyHarvester.fetchRandom(tempRescueCode);
    }

    public List<String> getTempShowableRescueCode() {
        String rescueCodeStr = getTempRescueCode();
        return splitEqually(rescueCodeStr, 4);
    }

    public static List<String> splitEqually(String text, int size) {
        List<String> ret = new ArrayList<>((text.length() + size - 1) / size);
        for (int start = 0; start < text.length(); start += size) {
            ret.add(text.substring(start, Math.min(text.length(), start + size)));
        }
        return ret;
    }

    public String getTempRescueCode() {
        BigInteger rescueCodeNum = new BigInteger(1, tempRescueCode);
        String rescueCodeStr = rescueCodeNum.toString(10);
        return rescueCodeStr.substring(rescueCodeStr.length() - 24);
    }

    public String fixString(String input) {
        int i = 1;
        String result = "";
        for(char s : input.toCharArray()) {
            result += s;
            if(i != 0 && i % 4 == 0) {
                result += " ";
                if(i % 20 == 0) {
                    result += "\n";
                }
            }
            i++;
        }
        return result;
    }

    public void read(byte[] input) throws Exception {
        this.cleanIdentity();
        String header = new String(Arrays.copyOfRange(input, 0, 8));

        hasIdentityBlock = false;
        hasRescueBlock = false;
        hasPreviousBlock = false;
        passwordBlockLength = 0;

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

        String inputString = EncryptionUtils.encodeBase56(Arrays.copyOfRange(input, HEADER_LENGTH + passwordBlockLength, input.length));
        verifyingRecoveryBlock = fixString(inputString);
    }

    /**
     * Password block.
     */
    private int identityPlaintextLength;
    private byte[] identityPlaintext;
    private byte[] initializationVector;
    private byte[] randomSalt;
    private byte logNFactor;
    private int  iterationCount;
    private int  optionFlags;
    private byte hintLength;
    private byte timeInSecondsToRunPWEnScryptOnPassword;
    private int idleTimoutInMinutes;
    private byte[] identityMasterKeyEncrypted;
    private byte[] identityLockKeyEncrypted;
    private byte[] identityVerificationTag;

    private byte[] identityMasterKey;
    private byte[] identityLockKey;

    public void handleIdentityBlock(byte[] input) {
        passwordBlockLength = input.length;
        identityPlaintextLength = getIntFromTwoBytes(input, 4);
        identityPlaintext = Arrays.copyOfRange(input, 0, identityPlaintextLength);
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
        identityVerificationTag = Arrays.copyOfRange(input, 109, 125);
        hasIdentityBlock = true;
    }

    private byte[] rescuePlaintext;
    private byte[] rescueRandomSalt;
    private byte rescueLogNFactor;
    private int rescueIterationCount;
    private byte[] rescueIdentityUnlockKeyEncrypted;
    private byte[] rescueIdentityUnlockKey;
    private byte[] rescueVerificationTag;

    private String verifyingRecoveryBlock;

    public String getVerifyingRecoveryBlock() {
        return verifyingRecoveryBlock;
    }

    public void handleRecoveryBlock(byte[] input) throws Exception {
        rescuePlaintext = Arrays.copyOfRange(input, 0, 25);
        rescueRandomSalt = Arrays.copyOfRange(input, 4, 20);
        rescueLogNFactor = input[20];
        rescueIterationCount = getIntFromFourBytes(input, 21);
        rescueIdentityUnlockKeyEncrypted = Arrays.copyOfRange(input, 25, 57);
        rescueVerificationTag = Arrays.copyOfRange(input, 57, 73);

        hasRescueBlock = true;
    }

    private byte[] previousPlaintext;
    private int previousCountOfKeys = 0;
    private byte[] previousKey1Encrypted;
    private byte[] previousKey2Encrypted;
    private byte[] previousKey3Encrypted;
    private byte[] previousKey4Encrypted;
    private byte[] previousKey1;
    private byte[] previousKey2;
    private byte[] previousKey3;
    private byte[] previousKey4;
    private byte[] previousVerificationTag;

    public void handlePreviousIdentityBlock(byte[] input) {
        previousPlaintext = Arrays.copyOfRange(input, 0, 6);
        previousCountOfKeys = getIntFromTwoBytes(input, 4);

        int startValue = 6;
        previousKey1Encrypted = Arrays.copyOfRange(input, startValue, startValue + 32);
        startValue += 32;
        if(previousCountOfKeys > 1) {
            previousKey2Encrypted = Arrays.copyOfRange(input, startValue, startValue + 32);
            startValue += 32;
        }
        if(previousCountOfKeys > 2) {
            previousKey3Encrypted = Arrays.copyOfRange(input, startValue, startValue + 32);
            startValue += 32;
        }
        if(previousCountOfKeys > 3) {
            previousKey4Encrypted = Arrays.copyOfRange(input, startValue, startValue + 32);
            startValue += 32;
        }
        previousVerificationTag = Arrays.copyOfRange(input, startValue, startValue + 16);

        hasPreviousBlock = true;
    }

    public void handleBlock(byte[] input) throws Exception {
        int type = getIntFromTwoBytes(input, 2);

        switch (type) {
            case PASSWORD_PBKDF:
                handleIdentityBlock(input);
                break;
            case RESCUECODE_PBKDF:
                handleRecoveryBlock(input);
                break;
            case PREVIOUS_IDENTITY_KEYS:
                handlePreviousIdentityBlock(input);
                break;
            default:
                throw new Exception("Unknown type "+type);
        }
    }

    public void cleanIdentity() {
        this.previousKeyIndex = 0;
        this.identityPlaintextLength = -1;
        this.identityPlaintext = null;
        this.initializationVector = null;
        this.randomSalt = null;
        this.logNFactor = -1;
        this.iterationCount = -1;
        this.optionFlags = -1;
        this.hintLength = -1;
        this.timeInSecondsToRunPWEnScryptOnPassword = -1;
        this.idleTimoutInMinutes = -1;
        this.identityMasterKeyEncrypted = null;
        this.identityLockKeyEncrypted = null;
        this.identityVerificationTag = null;
        if(this.identityMasterKey != null)
            clearBytes(this.identityMasterKey);
        if(this.identityLockKey != null)
            clearBytes(this.identityLockKey);
        this.identityMasterKey = null;
        this.identityLockKey = null;

        this.rescuePlaintext = null;
        this.rescueRandomSalt = null;
        this.rescueLogNFactor = -1;
        this.rescueIterationCount = -1;
        this.rescueIdentityUnlockKeyEncrypted = null;
        if(this.rescueIdentityUnlockKey != null)
            clearBytes(this.rescueIdentityUnlockKey);
        this.rescueIdentityUnlockKey = null;
        this.rescueVerificationTag = null;
        this.verifyingRecoveryBlock = null;

        this.previousPlaintext = null;
        this.previousCountOfKeys = 0;
        this.previousKey1Encrypted = null;
        this.previousKey2Encrypted = null;
        this.previousKey3Encrypted = null;
        this.previousKey4Encrypted = null;

        if(this.previousKey1 != null)
            clearBytes(this.previousKey1);
        if(this.previousKey2 != null)
            clearBytes(this.previousKey2);
        if(this.previousKey3 != null)
            clearBytes(this.previousKey3);
        if(this.previousKey4 != null)
            clearBytes(this.previousKey4);

        this.previousKey1 = null;
        this.previousKey2 = null;
        this.previousKey3 = null;
        this.previousKey4 = null;
        this.previousVerificationTag = null;
    }


    private int getIntFromTwoBytes(byte[] input, int offset) {
        return (input[offset] & 0xff) | ((input[offset + 1] & 0xff) << 8);
    }

    private int getIntFromFourBytes(byte[] input, int offset) {
        return (input[offset] & 0xff) | ((input[offset + 1] & 0xff) << 8) | (input[offset + 2] & 0xff) << 16 | ((input[offset + 3] & 0xff) << 24);
    }

    private byte[] getIntToTwoBytes(int input) {
        byte[] res = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(input).array();
        return Arrays.copyOfRange(res, 0 , 2);
    }

    private byte[] getIntToFourBytes(int input) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(input).array();
    }


    public boolean decryptIdentityKeyQuickPass(String password) {
        this.progressionUpdater.setMax(quickPassIterationCount);

        password = password.substring(0, this.getHintLength());

        try {
            byte[] key = EncryptionUtils.enSCryptIterations(password, quickPassRandomSalt, logNFactor, 32, quickPassIterationCount, this.progressionUpdater);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Key keySpec = new SecretKeySpec(key, "AES");
                Cipher cipher = Cipher.getInstance("AES_256/GCM/NoPadding");
                GCMParameterSpec params = new GCMParameterSpec(128, quickPassInitializationVector);
                cipher.init(Cipher.DECRYPT_MODE, keySpec, params);
                cipher.update(quickPassKeyEncrypted);
                try {
                    quickPassKey = cipher.doFinal(quickPassVerificationTag);
                } catch (AEADBadTagException badTag) {
                    return false;
                }
            } else {
                byte[] emptyPlainText = new byte[0];
                this.quickPassKey = new byte[32];

                Grc_aesgcm.gcm_setkey(key, key.length);
                int res = Grc_aesgcm.gcm_auth_decrypt(
                        quickPassInitializationVector, quickPassInitializationVector.length,
                        emptyPlainText, emptyPlainText.length,
                        quickPassKeyEncrypted, quickPassKey, quickPassKeyEncrypted.length,
                        quickPassVerificationTag, quickPassVerificationTag.length
                );
                Grc_aesgcm.gcm_zero_ctx();

                if (res == 0x55555555) return false;
            }

            if(hasPreviousBlock) {
                return decryptPreviousBlock();
            }
        } catch (Exception e) {
            Log.e(SQRLStorage.TAG, e.getMessage(), e);
            return false;
        }
        return true;
    }

    /**
     * Decrypt the identity key using quickpass, this has the master key used to login to sites and also the lock
     * key that we supply to the sites in order to unlock at a later date if the master key ever
     * gets compromised.
     *
     * @param password  Password used to unlock the master key.
     */
    public boolean decryptIdentityKey(String password) {
        this.progressionUpdater.setMax(iterationCount);
        try {
            byte[] key = EncryptionUtils.enSCryptIterations(password, randomSalt, logNFactor, 32, iterationCount, this.progressionUpdater);
            byte[] identityKeys = EncryptionUtils.combine(identityMasterKeyEncrypted, identityLockKeyEncrypted);
            byte[] decryptionResult = new byte[identityKeys.length];

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Key keySpec = new SecretKeySpec(key, "AES");
                Cipher cipher = Cipher.getInstance("AES_256/GCM/NoPadding");
                GCMParameterSpec params = new GCMParameterSpec(128, initializationVector);
                cipher.init(Cipher.DECRYPT_MODE, keySpec, params);
                cipher.updateAAD(identityPlaintext);
                cipher.update(identityKeys);
                try {
                    decryptionResult = cipher.doFinal(identityVerificationTag);
                } catch (AEADBadTagException badTag) {
                    return false;
                }
            } else {
                Grc_aesgcm.gcm_setkey(key, key.length);
                int res = Grc_aesgcm.gcm_auth_decrypt(
                        initializationVector, initializationVector.length,
                        identityPlaintext, identityPlaintextLength,
                        identityKeys, decryptionResult, identityKeys.length,
                        identityVerificationTag, identityVerificationTag.length
                );
                Grc_aesgcm.gcm_zero_ctx();

                if (res == 0x55555555) return false;
            }

            identityMasterKey = Arrays.copyOfRange(decryptionResult, 0, 32);
            identityLockKey = Arrays.copyOfRange(decryptionResult, 32, 64);

            if(hasPreviousBlock) {
                return decryptPreviousBlock();
            }
        } catch (Exception e) {
            Log.e(SQRLStorage.TAG, e.getMessage(), e);
            return false;
        }
        return true;
    }

    private boolean decryptPreviousBlock() {
        byte[] masterKey = this.identityMasterKey;
        if (this.quickPassKey != null) {
            masterKey = this.quickPassKey;
        }

        try {
            byte[] identityKeys = previousKey1Encrypted;
            if(previousCountOfKeys > 1) {
                identityKeys = EncryptionUtils.combine(identityKeys, previousKey2Encrypted);
            }
            if(previousCountOfKeys > 2) {
                identityKeys = EncryptionUtils.combine(identityKeys, previousKey3Encrypted);
            }
            if(previousCountOfKeys > 3) {
                identityKeys = EncryptionUtils.combine(identityKeys, previousKey4Encrypted);
            }

            byte[] decryptionResult = new byte[identityKeys.length];

            byte[] nullBytes = new byte[12];
            Arrays.fill(nullBytes, (byte)0);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Key keySpec = new SecretKeySpec(masterKey, "AES");
                Cipher cipher = Cipher.getInstance("AES_256/GCM/NoPadding");
                GCMParameterSpec params = new GCMParameterSpec(128, nullBytes);
                cipher.init(Cipher.DECRYPT_MODE, keySpec, params);
                cipher.updateAAD(previousPlaintext);
                cipher.update(identityKeys);
                try {
                    decryptionResult = cipher.doFinal(previousVerificationTag);
                } catch (AEADBadTagException badTag) {
                    return false;
                }
            } else {
                Grc_aesgcm.gcm_setkey(masterKey, masterKey.length);
                int res = Grc_aesgcm.gcm_auth_decrypt(
                        nullBytes, nullBytes.length,
                        previousPlaintext, previousPlaintext.length,
                        identityKeys, decryptionResult, identityKeys.length,
                        previousVerificationTag, previousVerificationTag.length
                );
                Grc_aesgcm.gcm_zero_ctx();

                if (res == 0x55555555) return false;
            }

            previousKey1 = Arrays.copyOfRange(decryptionResult, 0, 32);
            if(previousCountOfKeys > 1) {
                previousKey2 = Arrays.copyOfRange(decryptionResult, 32, 64);
            }
            if(previousCountOfKeys > 2) {
                previousKey3 = Arrays.copyOfRange(decryptionResult, 64, 96);
            }
            if(previousCountOfKeys > 3) {
                previousKey4 = Arrays.copyOfRange(decryptionResult, 96, 128);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return false;
        }
        return true;
    }

    /**
     * This unlocks the unlock key used to recover your identity if your master key gets comprimised
     * this key should NEVER be saved in the device. It's just used to create a new identity.
     *
     * @param rescueCode    Special rescueCode printed on paper in the format of 0000-0000-0000-0000-0000-0000
     */
    public boolean decryptUnlockKey(String rescueCode) {
        this.progressionUpdater.setMax(rescueIterationCount);
        rescueCode = rescueCode.replaceAll("-", "");

        try {
            byte[] key = EncryptionUtils.enSCryptIterations(rescueCode, rescueRandomSalt, rescueLogNFactor, 32, rescueIterationCount, this.progressionUpdater);

            byte[] nullBytes = new byte[12];
            Arrays.fill(nullBytes, (byte)0);

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Key keySpec = new SecretKeySpec(key, "AES");
                Cipher cipher = Cipher.getInstance("AES_256/GCM/NoPadding");
                GCMParameterSpec params = new GCMParameterSpec(128, nullBytes);
                cipher.init(Cipher.DECRYPT_MODE, keySpec, params);
                cipher.updateAAD(rescuePlaintext);
                cipher.update(rescueIdentityUnlockKeyEncrypted);
                try {
                    rescueIdentityUnlockKey = cipher.doFinal(rescueVerificationTag);
                } catch (AEADBadTagException badTag) {
                    return false;
                }
            } else {
                rescueIdentityUnlockKey = new byte[rescueIdentityUnlockKeyEncrypted.length];

                Grc_aesgcm.gcm_setkey(key, key.length);
                int res = Grc_aesgcm.gcm_auth_decrypt(
                        nullBytes, nullBytes.length,
                        rescuePlaintext, rescuePlaintext.length,
                        rescueIdentityUnlockKeyEncrypted, rescueIdentityUnlockKey,
                        rescueIdentityUnlockKeyEncrypted.length,
                        rescueVerificationTag, rescueVerificationTag.length
                );
                Grc_aesgcm.gcm_zero_ctx();

                if (res == 0x55555555) return false;
            }
        } catch (Exception e) {
            Log.e(SQRLStorage.TAG, e.getMessage(), e);
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return STORAGE_HEADER;
    }

    public void setProgressionUpdater(ProgressionUpdater progressionUpdater) {
        this.progressionUpdater = progressionUpdater;
    }

    public byte[] getKeySeed(String domain) throws Exception {
        byte[] masterKey = this.identityMasterKey;
        if (this.quickPassKey != null) {
            masterKey = this.quickPassKey;
        }
        final Mac HMacSha256 = Mac.getInstance("HmacSHA256");
        final SecretKeySpec key = new SecretKeySpec(masterKey, "HmacSHA256");
        HMacSha256.init(key);
        return HMacSha256.doFinal(domain.getBytes());
    }

    public byte[] getPrivateKey(String domain) throws Exception {
        byte[] publicKey = new byte[32];
        byte[] privateKey = new byte[64];

        Sodium.crypto_sign_seed_keypair(publicKey, privateKey, getKeySeed(domain));
        return privateKey;
    }

    public byte[] getPublicKey(String domain) throws Exception {
        byte[] publicKey = new byte[32];
        byte[] privateKey = new byte[64];

        Sodium.crypto_sign_seed_keypair(publicKey, privateKey, getKeySeed(domain));
        return publicKey;
    }

    public byte[] getPreviousKeySeed(String domain) throws Exception {
        byte[] currentPreviousUnlockKey;

        switch (this.previousKeyIndex) {
            case 1:
                currentPreviousUnlockKey = this.previousKey1;
                break;
            case 2:
                currentPreviousUnlockKey = this.previousKey2;
                break;
            case 3:
                currentPreviousUnlockKey = this.previousKey3;
                break;
            case 4:
                currentPreviousUnlockKey = this.previousKey4;
                break;
            default:
                currentPreviousUnlockKey = this.previousKey1;
        }

        byte[] currentPreviousKey = EncryptionUtils.enHash(currentPreviousUnlockKey);
        final Mac HMacSha256 = Mac.getInstance("HmacSHA256");
        final SecretKeySpec key = new SecretKeySpec(currentPreviousKey, "HmacSHA256");
        HMacSha256.init(key);
        return HMacSha256.doFinal(domain.getBytes());
    }

    public byte[] getPreviousPublicKey(String domain) throws Exception {
        byte[] publicKey = new byte[32];
        byte[] privateKey = new byte[64];

        Sodium.crypto_sign_seed_keypair(publicKey, privateKey, getPreviousKeySeed(domain));
        return publicKey;
    }

    public boolean hasPreviousKeys() {
        return hasPreviousBlock;
    }

    public byte[] getPreviousPrivateKey(String domain) throws Exception {
        byte[] publicKey = new byte[32];
        byte[] privateKey = new byte[64];

        Sodium.crypto_sign_seed_keypair(publicKey, privateKey, getPreviousKeySeed(domain));
        return privateKey;
    }


    public boolean hasEncryptedKeys() {
        return this.identityMasterKeyEncrypted != null;
    }


    public boolean hasKeys() {
        return this.identityMasterKey != null;
    }

    public boolean hasQuickPass() {
        return this.quickPassKeyEncrypted != null || this.quickPassKey != null;
    }

    public void clearQuickPass(Context context) {
        this.previousKeyIndex = 0;
        try {
            if(this.quickPassKeyEncrypted != null) {
                clearBytes(this.quickPassKeyEncrypted);
            }
            if(this.quickPassKey != null) {
                clearBytes(this.quickPassKey);
            }
        } finally {
            this.quickPassKeyEncrypted = null;
            this.quickPassKey = null;
        }

        NotificationManager notificationManager =
                (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        if(notificationManager != null) {
            notificationManager.cancel(BaseActivity.NOTIFICATION_IDENTITY_UNLOCKED);
        }
    }

    public void clear() {
        this.previousKeyIndex = 0;
        try {
            if(this.identityLockKey != null) {
                clearBytes(this.identityLockKey);
            }
            if(this.identityMasterKey != null) {
                clearBytes(this.identityMasterKey);
            }
            if(this.rescueIdentityUnlockKey != null) {
                clearBytes(this.rescueIdentityUnlockKey);
            }
            if(this.quickPassKey != null) {
                clearBytes(this.quickPassKey);
            }
            if(this.tempRescueCode != null) {
                clearBytes(this.tempRescueCode);
            }
            if(this.previousKey1 != null) {
                clearBytes(this.previousKey1);
            }
            if(this.previousKey2 != null) {
                clearBytes(this.previousKey2);
            }
            if(this.previousKey3 != null) {
                clearBytes(this.previousKey3);
            }
            if(this.previousKey4 != null) {
                clearBytes(this.previousKey4);
            }
        } finally {
            this.identityLockKey = null;
            this.identityMasterKey = null;
            this.rescueIdentityUnlockKey = null;
            this.quickPassKey = null;
            this.tempRescueCode = null;
            this.previousKey1 = null;
            this.previousKey2 = null;
            this.previousKey3 = null;
            this.previousKey4 = null;
        }
    }

    private void clearBytes(byte[] data) {
        if(data == null) return;
        Random r = new SecureRandom();
        r.nextBytes(data);
        Arrays.fill(data, (byte)0);
        r.nextBytes(data);
        Arrays.fill(data, (byte)255);
    }

    /**
     * Encrypt the identity key, this has the master key used to login to sites and also the lock
     * key that we supply to the sites in order to lock at a later date if the master key ever
     * gets compromised.
     *
     * @param password          Password used to encrypt the master key.
     * @param entropyHarvester  Class to give us new random bits for encryption
     */
    public boolean encryptIdentityKeyQuickPass(String password, EntropyHarvester entropyHarvester) {
        if(!this.hasKeys() || !this.hasEncryptedKeys()) return false;
        this.progressionUpdater.clear();
        password = password.substring(0, this.getHintLength());

        this.quickPassRandomSalt = new byte[16];
        this.quickPassInitializationVector = new byte[12];
        this.quickPassKeyEncrypted = new byte[32];
        this.quickPassVerificationTag = new byte[16];

        try {
            entropyHarvester.fetchRandom(this.quickPassRandomSalt);

            byte[] encResult = EncryptionUtils.enSCryptTime(password, this.quickPassRandomSalt, logNFactor, 32, timeInSecondsToRunPWEnScryptOnPassword, this.progressionUpdater);
            this.quickPassIterationCount = getIntFromFourBytes(encResult, 0);
            byte[] key = Arrays.copyOfRange(encResult, 4, 36);

            entropyHarvester.fetchRandom(this.quickPassInitializationVector);

            this.updateIdentityPlaintext();

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Key keySpec = new SecretKeySpec(key, "AES");
                Cipher cipher = Cipher.getInstance("AES_256/GCM/NoPadding");
                GCMParameterSpec params = new GCMParameterSpec(128, this.quickPassInitializationVector);
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, params);
                cipher.update(identityMasterKey);
                byte[] encryptionResult = cipher.doFinal();

                this.quickPassKeyEncrypted = Arrays.copyOfRange(encryptionResult, 0, 32);
                this.quickPassVerificationTag = Arrays.copyOfRange(encryptionResult, 32, 48);
            } else {
                byte[] emptyPlainText = new byte[0];
                Grc_aesgcm.gcm_setkey(key, key.length);
                int res = Grc_aesgcm.gcm_encrypt_and_tag(
                        this.quickPassInitializationVector, this.quickPassInitializationVector.length,
                        emptyPlainText, emptyPlainText.length,
                        identityMasterKey, this.quickPassKeyEncrypted, identityMasterKey.length,
                        this.quickPassVerificationTag, this.quickPassVerificationTag.length
                );
                Grc_aesgcm.gcm_zero_ctx();
                if (res == 0x55555555) return false;
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return false;
        }
        return true;
    }

    /**
     * Encrypt the identity key, this has the master key used to login to sites and also the lock
     * key that we supply to the sites in order to lock at a later date if the master key ever
     * gets compromised.
     *
     * @param password          Password used to encrypt the master key.
     * @param entropyHarvester  Class to give us new random bits for encryption
     */
    public boolean encryptIdentityKey(String password, EntropyHarvester entropyHarvester) {
        if(!this.hasKeys()) return false;
        this.progressionUpdater.clear();

        if(!this.hasEncryptedKeys()) {
            this.setHintLength(4);
            this.setIdleTimeout(5);
            this.setPasswordVerify(5);
            this.optionFlags = 0x1f3;
            this.logNFactor = 9;
            this.identityPlaintextLength = 45;
            this.randomSalt = new byte[16];
            this.initializationVector = new byte[12];
            this.hasIdentityBlock = true;
            this.identityMasterKeyEncrypted = new byte[32];
            this.identityLockKeyEncrypted = new byte[32];
            this.identityVerificationTag = new byte[16];
        }

        try {
            entropyHarvester.fetchRandom(this.randomSalt);

            byte[] encResult = EncryptionUtils.enSCryptTime(password, randomSalt, logNFactor, 32, timeInSecondsToRunPWEnScryptOnPassword, this.progressionUpdater);
            this.iterationCount = getIntFromFourBytes(encResult, 0);
            byte[] key = Arrays.copyOfRange(encResult, 4, 36);

            byte[] identityKeys = EncryptionUtils.combine(identityMasterKey, identityLockKey);

            entropyHarvester.fetchRandom(this.initializationVector);

            this.updateIdentityPlaintext();

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Key keySpec = new SecretKeySpec(key, "AES");
                Cipher cipher = Cipher.getInstance("AES_256/GCM/NoPadding");
                GCMParameterSpec params = new GCMParameterSpec(128, initializationVector);
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, params);
                cipher.updateAAD(identityPlaintext);
                cipher.update(identityKeys);
                byte[] encryptionResult = cipher.doFinal();

                this.identityMasterKeyEncrypted = Arrays.copyOfRange(encryptionResult, 0, 32);
                this.identityLockKeyEncrypted = Arrays.copyOfRange(encryptionResult, 32, 64);
                this.identityVerificationTag = Arrays.copyOfRange(encryptionResult, 64, 80);
            } else {
                byte[] resultVerificationTag = new byte[16];
                byte[] encryptionResult = new byte[identityKeys.length];

                Grc_aesgcm.gcm_setkey(key, key.length);
                int res = Grc_aesgcm.gcm_encrypt_and_tag(
                        initializationVector, initializationVector.length,
                        identityPlaintext, identityPlaintextLength,
                        identityKeys, encryptionResult, identityKeys.length,
                        resultVerificationTag, resultVerificationTag.length
                );
                Grc_aesgcm.gcm_zero_ctx();

                if (res == 0x55555555) return false;

                this.identityMasterKeyEncrypted = Arrays.copyOfRange(encryptionResult, 0, 32);
                this.identityLockKeyEncrypted = Arrays.copyOfRange(encryptionResult, 32, 64);
                this.identityVerificationTag = resultVerificationTag;
            }

            if(hasPreviousBlock) {
                return encryptPreviousBlock();
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return false;
        }
        return true;
    }


    private boolean encryptPreviousBlock() {
        try {
            byte[] identityKeys = previousKey1;
            if(previousCountOfKeys > 1) {
                identityKeys = EncryptionUtils.combine(identityKeys, previousKey2);
            }
            if(previousCountOfKeys > 2) {
                identityKeys = EncryptionUtils.combine(identityKeys, previousKey3);
            }
            if(previousCountOfKeys > 3) {
                identityKeys = EncryptionUtils.combine(identityKeys, previousKey4);
            }

            byte[] nullBytes = new byte[12];
            Arrays.fill(nullBytes, (byte)0);

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Key keySpec = new SecretKeySpec(this.identityMasterKey, "AES");
                Cipher cipher = Cipher.getInstance("AES_256/GCM/NoPadding");
                GCMParameterSpec params = new GCMParameterSpec(128, nullBytes);
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, params);
                cipher.updateAAD(previousPlaintext);
                cipher.update(identityKeys);
                byte[] encryptionResult = cipher.doFinal();

                int nextKeyStart = 0;
                previousKey1Encrypted = Arrays.copyOfRange(encryptionResult, nextKeyStart, nextKeyStart + 32);
                nextKeyStart += 32;
                if(previousCountOfKeys > 1) {
                    previousKey2Encrypted = Arrays.copyOfRange(encryptionResult, nextKeyStart, nextKeyStart + 32);
                    nextKeyStart += 32;
                }
                if(previousCountOfKeys > 2) {
                    previousKey3Encrypted = Arrays.copyOfRange(encryptionResult, nextKeyStart, nextKeyStart + 32);
                    nextKeyStart += 32;
                }
                if(previousCountOfKeys > 3) {
                    previousKey4Encrypted = Arrays.copyOfRange(encryptionResult, nextKeyStart, nextKeyStart + 32);
                    nextKeyStart += 32;
                }
                this.previousVerificationTag = Arrays.copyOfRange(encryptionResult, nextKeyStart, nextKeyStart + 16);
            } else {
                byte[] resultVerificationTag = new byte[16];
                byte[] encryptionResult = new byte[identityKeys.length];

                Grc_aesgcm.gcm_setkey(this.identityMasterKey, this.identityMasterKey.length);
                int res = Grc_aesgcm.gcm_encrypt_and_tag(
                        nullBytes, nullBytes.length,
                        previousPlaintext, previousPlaintext.length,
                        identityKeys, encryptionResult, identityKeys.length,
                        resultVerificationTag, resultVerificationTag.length
                );
                Grc_aesgcm.gcm_zero_ctx();

                if (res == 0x55555555) return false;

                int nextKeyStart = 0;
                previousKey1Encrypted = Arrays.copyOfRange(encryptionResult, nextKeyStart, nextKeyStart + 32);
                nextKeyStart += 32;
                if(previousCountOfKeys > 1) {
                    previousKey2Encrypted = Arrays.copyOfRange(encryptionResult, nextKeyStart, nextKeyStart + 32);
                    nextKeyStart += 32;
                }
                if(previousCountOfKeys > 2) {
                    previousKey3Encrypted = Arrays.copyOfRange(encryptionResult, nextKeyStart, nextKeyStart + 32);
                    nextKeyStart += 32;
                }
                if(previousCountOfKeys > 3) {
                    previousKey4Encrypted = Arrays.copyOfRange(encryptionResult, nextKeyStart, nextKeyStart + 32);
                }
                previousVerificationTag = resultVerificationTag;
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return false;
        }
        return true;
    }

    private void addPreviousKey(byte[] identityUnlockKey) {
        if(hasPreviousBlock) {
            this.reInitializeMasterKeyIdentity();
        }
        if(!hasPreviousBlock || previousCountOfKeys == 0 || this.decryptPreviousBlock()) {
            Log.e(TAG, "Before previous key");
            Log.e(TAG, EncryptionUtils.byte2hex(identityUnlockKey));
            if(this.previousKey1 != null)
                Log.e(TAG, EncryptionUtils.byte2hex(this.previousKey1));
            if(this.previousKey2 != null)
                Log.e(TAG, EncryptionUtils.byte2hex(this.previousKey2));
            if(this.previousKey3 != null)
                Log.e(TAG, EncryptionUtils.byte2hex(this.previousKey3));
            if(this.previousKey4 != null)
                Log.e(TAG, EncryptionUtils.byte2hex(this.previousKey4));

            if (this.previousCountOfKeys < 4) {
                this.previousCountOfKeys++;
            }
            if (previousCountOfKeys > 3) {
                this.previousKey4 = this.previousKey3;
            }
            if (previousCountOfKeys > 2) {
                this.previousKey3 = this.previousKey2;
            }
            if (previousCountOfKeys > 1) {
                this.previousKey2 = this.previousKey1;
            }
            this.previousKey1 = identityUnlockKey;
            this.hasPreviousBlock = true;
            this.updatePreviousPlaintext();
        }
        Log.e(TAG, "After previous key");
        if(this.previousKey1 != null)
            Log.e(TAG, EncryptionUtils.byte2hex(this.previousKey1));
        if(this.previousKey2 != null)
            Log.e(TAG, EncryptionUtils.byte2hex(this.previousKey2));
        if(this.previousKey3 != null)
            Log.e(TAG, EncryptionUtils.byte2hex(this.previousKey3));
        if(this.previousKey4 != null)
            Log.e(TAG, EncryptionUtils.byte2hex(this.previousKey4));
    }

    /*
E/SQRLStorage: Before previous key
E/SQRLStorage: a88b6952bcf73fd1192c42003d0e1efd4d1ef87ccc6f72a1a64ce722fa9f8c72
E/SQRLStorage: After previous key
E/SQRLStorage: a88b6952bcf73fd1192c42003d0e1efd4d1ef87ccc6f72a1a64ce722fa9f8c72

E/SQRLStorage: Before previous key
E/SQRLStorage: e2bd235e4bee2c382c8c7ea4047c10bb2de9ecdda9279c0a48f9a026f1e1377c
E/SQRLStorage: After previous key
E/SQRLStorage: e2bd235e4bee2c382c8c7ea4047c10bb2de9ecdda9279c0a48f9a026f1e1377c
     */


    /**
     * Encrypt the identity key, this has the master key used to login to sites and also the lock
     * key that we supply to the sites in order to lock at a later date if the master key ever
     * gets compromised.
     *
     * @param entropyHarvester  Class to give us new random bits for encryption
     */
    public boolean encryptRescueKey(EntropyHarvester entropyHarvester) {
        this.progressionUpdater.clear();

        if(this.hasRescueBlock && this.rescueIdentityUnlockKey != null) {
            addPreviousKey(this.rescueIdentityUnlockKey);
        }

        this.rescueRandomSalt = new byte[16];
        this.rescueLogNFactor = 9;
        this.rescueIdentityUnlockKey = new byte[32];
        this.rescueIdentityUnlockKeyEncrypted = new byte[32];
        this.rescueVerificationTag = new byte[16];
        this.hasRescueBlock = true;

        byte rescueCodeEncryptionTime = (byte)150; // 2 min 30 seconds.
        try {
            entropyHarvester.fetchRandom(this.rescueRandomSalt);
            entropyHarvester.fetchRandom(this.rescueIdentityUnlockKey);

            byte[] encResult = EncryptionUtils.enSCryptTime(getTempRescueCode(), rescueRandomSalt, rescueLogNFactor, 32, rescueCodeEncryptionTime, this.progressionUpdater);
            this.rescueIterationCount = getIntFromFourBytes(encResult, 0);
            byte[] key = Arrays.copyOfRange(encResult, 4, 36);

            byte[] nullBytes = new byte[12];
            Arrays.fill(nullBytes, (byte)0);

            this.updateRescuePlaintext();

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Key keySpec = new SecretKeySpec(key, "AES");
                Cipher cipher = Cipher.getInstance("AES_256/GCM/NoPadding");
                GCMParameterSpec params = new GCMParameterSpec(128, nullBytes);
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, params);
                cipher.updateAAD(rescuePlaintext);
                cipher.update(rescueIdentityUnlockKey);
                byte[] encryptionResult = cipher.doFinal();

                this.rescueIdentityUnlockKeyEncrypted = Arrays.copyOfRange(encryptionResult, 0, 32);
                this.rescueVerificationTag = Arrays.copyOfRange(encryptionResult, 32, 48);
            } else {
                byte[] resultVerificationTag = new byte[16];
                byte[] encryptionResult = new byte[rescueIdentityUnlockKey.length];

                Grc_aesgcm.gcm_setkey(key, key.length);
                int res = Grc_aesgcm.gcm_encrypt_and_tag(
                        nullBytes, nullBytes.length,
                        rescuePlaintext, rescuePlaintext.length,
                        rescueIdentityUnlockKey, encryptionResult, rescueIdentityUnlockKey.length,
                        resultVerificationTag, resultVerificationTag.length
                );
                Grc_aesgcm.gcm_zero_ctx();

                if (res == 0x55555555) return false;

                this.rescueIdentityUnlockKeyEncrypted = encryptionResult;
                this.rescueVerificationTag = resultVerificationTag;
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return false;
        }
        return true;
    }


    public String getOptions(boolean noiptest, boolean suk) {
        List<String> options = new ArrayList<>();
        if(isNoByPass()) {
            options.add("hardlock");
        }
        if(isSQRLOnly()) {
            options.add("sqrlonly");
        }
        if(noiptest) {
            options.add("noiptest");
        }
        if(suk) {
            options.add("suk");
        }

        StringBuilder sb = new StringBuilder();
        if(options.size() > 0) {
            sb.append("opt=");
            boolean first = true;
            for (String s : options) {
                if (!first) {
                    sb.append("~");
                }
                sb.append(s);
                first = false;
            }
            sb.append("\r\n");
        }
        return sb.toString();
    }

    private void updateIdentityPlaintext() {
        if(!hasIdentityBlock) return;
        byte[] newPlaintext = getIntToTwoBytes(PASSWORD_PBKDF);
        newPlaintext = EncryptionUtils.combine(newPlaintext, getIntToTwoBytes(identityPlaintextLength));
        newPlaintext = EncryptionUtils.combine(newPlaintext, initializationVector);
        newPlaintext = EncryptionUtils.combine(newPlaintext, randomSalt);
        newPlaintext = EncryptionUtils.combine(newPlaintext, logNFactor);
        newPlaintext = EncryptionUtils.combine(newPlaintext, getIntToFourBytes(iterationCount));
        newPlaintext = EncryptionUtils.combine(newPlaintext, getIntToTwoBytes(optionFlags));
        newPlaintext = EncryptionUtils.combine(newPlaintext, hintLength);
        newPlaintext = EncryptionUtils.combine(newPlaintext, timeInSecondsToRunPWEnScryptOnPassword);
        newPlaintext = EncryptionUtils.combine(newPlaintext, getIntToTwoBytes(idleTimoutInMinutes));

        newPlaintext = EncryptionUtils.combine(
            this.getIntToTwoBytes(
                BLOCK_LENGTH_SIZE +
                newPlaintext.length +
                identityMasterKeyEncrypted.length +
                identityLockKeyEncrypted.length +
                identityVerificationTag.length
            ), newPlaintext);
        identityPlaintext = newPlaintext;

    }

    private void updateRescuePlaintext() {
        if(!hasRescueBlock) return;

        byte[] newPlaintext = getIntToTwoBytes(RESCUECODE_PBKDF);
        newPlaintext = EncryptionUtils.combine(newPlaintext, rescueRandomSalt);
        newPlaintext = EncryptionUtils.combine(newPlaintext, rescueLogNFactor);
        newPlaintext = EncryptionUtils.combine(newPlaintext, getIntToFourBytes(rescueIterationCount));

        newPlaintext = EncryptionUtils.combine(
            this.getIntToTwoBytes(
                BLOCK_LENGTH_SIZE +
                newPlaintext.length +
                rescueIdentityUnlockKeyEncrypted.length +
                rescueVerificationTag.length
            ), newPlaintext);

        rescuePlaintext = newPlaintext;
    }

    private void updatePreviousPlaintext() {
        if(!hasPreviousBlock) return;

        final int PREVIOUS_KEY_LEN = 32;
        final int PREVIOUS_VERIFY_LEN = 16;

        if (previousCountOfKeys > 0) {
            byte[] newPlaintext = getIntToTwoBytes(PREVIOUS_IDENTITY_KEYS);
            newPlaintext = EncryptionUtils.combine(newPlaintext, getIntToTwoBytes(previousCountOfKeys));
            newPlaintext = EncryptionUtils.combine(
                this.getIntToTwoBytes(
                    BLOCK_LENGTH_SIZE +
                    newPlaintext.length +
                    PREVIOUS_KEY_LEN * previousCountOfKeys +
                    PREVIOUS_VERIFY_LEN
                ), newPlaintext);
            previousPlaintext = newPlaintext;
        }
    }

    public byte[] createSaveData() {
        updateIdentityPlaintext();
        updateRescuePlaintext();
        updatePreviousPlaintext();

        byte[] result = "sqrldata".getBytes();
        if(hasIdentityBlock) {
            result = EncryptionUtils.combine(result, identityPlaintext);
            result = EncryptionUtils.combine(result, identityMasterKeyEncrypted);
            result = EncryptionUtils.combine(result, identityLockKeyEncrypted);
            result = EncryptionUtils.combine(result, identityVerificationTag);
        }

        if(hasRescueBlock) {
            result = EncryptionUtils.combine(result, rescuePlaintext);
            result = EncryptionUtils.combine(result, rescueIdentityUnlockKeyEncrypted);
            result = EncryptionUtils.combine(result, rescueVerificationTag);
        }

        if (hasPreviousBlock && previousCountOfKeys > 0) {
            result = EncryptionUtils.combine(result, previousPlaintext);
            result = EncryptionUtils.combine(result, previousKey1Encrypted);
            if (previousCountOfKeys > 1) {
                result = EncryptionUtils.combine(result, previousKey2Encrypted);
            }
            if (previousCountOfKeys > 2) {
                result = EncryptionUtils.combine(result, previousKey3Encrypted);
            }
            if (previousCountOfKeys > 3) {
                result = EncryptionUtils.combine(result, previousKey4Encrypted);
            }
            result = EncryptionUtils.combine(result, previousVerificationTag);
        }
        return result;
    }

    public void reInitializeMasterKeyIdentity() {
        if(this.rescueIdentityUnlockKey != null) {
            this.identityMasterKey = EncryptionUtils.enHash(this.rescueIdentityUnlockKey);
            this.identityLockKey = new byte[this.identityMasterKey.length];
            Sodium.crypto_scalarmult_base(this.identityLockKey, this.rescueIdentityUnlockKey);
        }
    }

    public byte[] getUnlockRequestSigningKey(byte[] serverUnlock) {
        /*
        UnlockRequestSigning := SignPrivate( DHKA( ServerUnlock, IdentityUnlock ))
        */

        byte[] bytesToSign = new byte[32];
        byte[] notImportant = new byte[32];
        byte[] unlockRequestSign = new byte[64];

        Sodium.crypto_scalarmult(bytesToSign, this.rescueIdentityUnlockKey, serverUnlock);
        Sodium.crypto_sign_seed_keypair(notImportant, unlockRequestSign, bytesToSign);
        return unlockRequestSign;
    }

    public String getServerUnlockKey(EntropyHarvester entropyHarvester) {
        /*
        VerifyUnlock := 	SignPublic( DHKA( IdentityLock, RandomLock ))
        ServerUnlock := 	MakePublic( RandomLock )

        libsodium crypto_scalarmult_base()
        in: secret key = RLK
        out: public key = SUK

        libsodium crypto_scalarmult()
        in: private key = RLK
        in: public key = ILK
        out: shared key = DHK

        libsodium crypto_sign_seed_keypair()
        in: seed = DHK
        out: public key = VUK
        */
        try {
            byte[] randomLock = new byte[32];
            entropyHarvester.fetchRandom(randomLock);

            byte[] bytesToSign = new byte[32];
            byte[] serverUnlock = new byte[32];
            byte[] notImportant = new byte[64];
            byte[] verifyUnlock = new byte[32];

            Sodium.crypto_scalarmult_base(serverUnlock, randomLock);
            Sodium.crypto_scalarmult(bytesToSign, randomLock, this.identityLockKey);
            Sodium.crypto_sign_seed_keypair(verifyUnlock, notImportant, bytesToSign);

            StringBuilder sb = new StringBuilder();
            sb.append("suk=");
            sb.append(EncryptionUtils.encodeUrlSafe(serverUnlock));
            sb.append("\r\n");
            sb.append("vuk=");
            sb.append(EncryptionUtils.encodeUrlSafe(verifyUnlock));
            sb.append("\r\n");
            return sb.toString();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return "";
    }

    public boolean hasIdentityBlock() {
        return hasIdentityBlock;
    }

    public void setHintLength(int hintLength) {
        this.hintLength = (byte)hintLength;
    }

    public void setPasswordVerify(int passwordVerify) {
        this.timeInSecondsToRunPWEnScryptOnPassword = (byte)passwordVerify;
    }

    public void setIdleTimeout(int idleTimeout) {
        this.idleTimoutInMinutes = idleTimeout;
    }

    public void setSQRLOnly(boolean SQRLOnly) {
        if(SQRLOnly) {
            optionFlags |= 1 << 2;
        } else {
            optionFlags &= ~(1 << 2);
        }
    }

    public void setNoByPass(boolean noByPass) {
        if(noByPass) {
            optionFlags |= 1 << 3;
        } else {
            optionFlags &= ~(1 << 3);
        }
    }

    public int getHintLength() {
        return this.hintLength;
    }

    public int getPasswordVerify() {
        return this.timeInSecondsToRunPWEnScryptOnPassword;
    }

    public int getIdleTimeout()  {
        return this.idleTimoutInMinutes;
    }

    public boolean isSQRLOnly() {
        return ((optionFlags >> 2) & 1) == 1;
    }

    public boolean isNoByPass() {
        return ((optionFlags >> 3) & 1) == 1;
    }


    public static void main(String[] args) {
        try {
/*
            //String rawQRCodeData = "71a48c7371726c3a2f2f7371";
            String rawQRCodeData = "4ce7371726c646174617d0001002d0031d32536a67c4661faef7631d4a7854da64297af4bda01438eca39a40921000000f10104010f0085de0eea7b76134eee4e0b2d638955ad5fd253d857c86177151d30a956182abc55b80316da5c22fcc9b92c4f5a4850f5a4ecb6b948f05b297de13b1a7698cbee461c5e7ef0f8759e659a7ad853555be64900020079d1d5da2d4b046212f43da2f7b0a39709ca000000d5dd50893d516c8175291f7d905b5bf5636d26fee5d3f8801375f7824b09a2a824de7fc41451ca13e610d5591d568db60ec11ec11ec11ec11ec11ec11ec11ec11ec11ec11ec11ec11";
            byte[] bytesArray = EncryptionUtils.readSQRLQRCode(EncryptionUtils.hex2Byte(rawQRCodeData));

            SQRLStorage storage = SQRLStorage.getInstance();
            storage.setProgressionUpdater(new ProgressionUpdater());
            storage.read(bytesArray);
*/
/*
            boolean ok = storage.decryptIdentityKey("Testing1234");
            System.out.println(ok);
            boolean ok2 = storage.decryptUnlockKey("3304-9688-5754-6944-1932-9846");
            System.out.println(ok2);
*/
            /*
            byte[] saveData = storage.createSaveData();

            System.out.println(EncryptionUtils.byte2hex(bytesArray));
            System.out.println(EncryptionUtils.byte2hex(saveData));

            System.out.println(Arrays.equals(bytesArray, saveData));
            */


            File file = new File("Testing3.sqrl");
            byte[] bytesArray = new byte[(int) file.length()];

            FileInputStream fis = new FileInputStream(file);
            fis.read(bytesArray);
            fis.close();

            SQRLStorage storage = SQRLStorage.getInstance();
            storage.setProgressionUpdater(new ProgressionUpdater());
            storage.read(bytesArray);

            byte[] saveData = storage.createSaveData();

            storage.read(saveData);

            /*
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update("tfJ8CRxisuQQGY3KRcv".getBytes("US-ASCII"));
            md.update((byte)0);
            BigInteger reminder = new BigInteger(1, md.digest()).mod(BigInteger.valueOf(56));
            System.out.println(reminder.intValue());
            */

        } catch (Exception e) {
            e.printStackTrace();
        }
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