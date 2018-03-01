package org.ea.sqrl.processors;

import android.os.Build;
import android.util.Log;

import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec;
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec;

import org.ea.sqrl.jni.Grc_aesgcm;
import org.ea.sqrl.utils.EncryptionUtils;
import org.libsodium.jni.NaCl;
import org.libsodium.jni.Sodium;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Arrays;
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
    private static final int BLOCK_SIZE_LENGTH = 2;
    private ProgressionUpdater progressionUpdater;
    private int passwordBlockLength = 0;
    private static SQRLStorage instance = null;

    private boolean hasIdentityBlock = false;
    private boolean hasRescueBlock = false;
    private boolean hasPreviousBlock = false;

    private SQRLStorage() {
        Grc_aesgcm.gcm_initialize();
        NaCl.sodium();

        /*
            Here we look for the scrypt library and if we can't find it
            on the system UnsatificedLinkError will be thrown and we will
            fallback using a java version.
         */
        try {
            System.loadLibrary("scrypt");
            System.setProperty("com.lambdaworks.jni.loader", "sys");
        } catch (UnsatisfiedLinkError e) {
            System.setProperty("com.lambdaworks.jni.loader", "nil");
        }
    }

    public static SQRLStorage getInstance() {
        if(instance == null) {
            instance = new SQRLStorage();
        }
        return instance;
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

    public void read(byte[] input, boolean full) throws Exception {
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
    private byte[] rescueIdentityLockKeyEncrypted;
    private byte[] rescueIdentityLockKey;
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
        rescueIdentityLockKeyEncrypted = Arrays.copyOfRange(input, 25, 57);
        rescueVerificationTag = Arrays.copyOfRange(input, 57, 73);

        hasRescueBlock = true;
    }

    private byte[] previousPlaintext;
    private int previousCountOfKeys = 0;
    private byte[] previousKey1;
    private byte[] previousKey2;
    private byte[] previousKey3;
    private byte[] previousKey4;
    private byte[] previousVerificationTag;

    public void handlePreviousIdentityBlock(byte[] input) {
        previousPlaintext = Arrays.copyOfRange(input, 0, 6);
        previousCountOfKeys = getIntFromTwoBytes(input, 4);

        int lastKeyEnd = 6 + 32;
        previousKey1 = Arrays.copyOfRange(input, 6, lastKeyEnd);
        if(previousCountOfKeys > 1) {
            previousKey2 = Arrays.copyOfRange(input, lastKeyEnd, lastKeyEnd + 32);
            lastKeyEnd += 32;
        }
        if(previousCountOfKeys > 2) {
            previousKey3 = Arrays.copyOfRange(input, lastKeyEnd, lastKeyEnd + 32);
            lastKeyEnd += 32;
        }
        if(previousCountOfKeys > 3) {
            previousKey4 = Arrays.copyOfRange(input, lastKeyEnd, lastKeyEnd + 32);
            lastKeyEnd += 32;
        }
        previousVerificationTag = Arrays.copyOfRange(input, lastKeyEnd, lastKeyEnd + 16);
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


    /**
     * Decrypt the identity key, this has the master key used to login to sites and also the lock
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

            if (Build.VERSION.BASE_OS != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
        } catch (Exception e) {
            Log.e(SQRLStorage.TAG, e.getMessage(), e);
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

            if(Build.VERSION.BASE_OS != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Key keySpec = new SecretKeySpec(key, "AES");
                Cipher cipher = Cipher.getInstance("AES_256/GCM/NoPadding");
                GCMParameterSpec params = new GCMParameterSpec(128, nullBytes);
                cipher.init(Cipher.DECRYPT_MODE, keySpec, params);
                cipher.updateAAD(rescuePlaintext);
                cipher.update(rescueIdentityLockKeyEncrypted);
                try {
                    rescueIdentityLockKey = cipher.doFinal(rescueVerificationTag);
                } catch (AEADBadTagException badTag) {
                    return false;
                }
            } else {
                Grc_aesgcm.gcm_setkey(key, key.length);
                int res = Grc_aesgcm.gcm_auth_decrypt(
                        nullBytes, nullBytes.length,
                        identityPlaintext, identityPlaintextLength,
                        rescueIdentityLockKeyEncrypted, rescueIdentityLockKey,
                        rescueIdentityLockKeyEncrypted.length,
                        identityVerificationTag, identityVerificationTag.length
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

    public byte[] getPrivateKey(String domain) throws Exception {
        final Mac HMacSha256 = Mac.getInstance("HmacSHA256");
        final SecretKeySpec key = new SecretKeySpec(this.identityMasterKey, "HmacSHA256");
        HMacSha256.init(key);
        return HMacSha256.doFinal(domain.getBytes());
    }

    public boolean hasEncryptedKeys() {
        if(this.identityMasterKeyEncrypted != null) {
            return true;
        }
        return false;
    }


    public boolean hasKeys() {
        if(this.identityMasterKey != null) {
            return true;
        }
        return false;
    }

    public void clear() {
        if(!this.hasKeys()) return;
        try {
            clearBytes(this.identityLockKey);
            clearBytes(this.identityMasterKey);
        } finally {
            this.identityLockKey = null;
            this.identityMasterKey = null;
        }
    }

    private void clearBytes(byte[] data) {
        Random r = new SecureRandom();
        r.nextBytes(data);
        Arrays.fill(data, (byte)0);
        r.nextBytes(data);
        Arrays.fill(data, (byte)255);
    }

    /**
     * Decrypt the identity key, this has the master key used to login to sites and also the lock
     * key that we supply to the sites in order to unlock at a later date if the master key ever
     * gets compromised.
     *
     * @param password  Password used to unlock the master key.
     */
    public boolean encryptIdentityKey(String password, EntropyHarvester entropyHarvester) {
        if(!this.hasKeys()) return false;
        this.progressionUpdater.clear();

        try {
            entropyHarvester.fetchRandom(this.randomSalt);

            byte[] encResult = EncryptionUtils.enSCryptTime(password, randomSalt, logNFactor, 32, timeInSecondsToRunPWEnScryptOnPassword, this.progressionUpdater);
            this.iterationCount = getIntFromFourBytes(encResult, 0);
            byte[] key = Arrays.copyOfRange(encResult, 4, 36);

            byte[] identityKeys = EncryptionUtils.combine(identityMasterKey, identityLockKey);
            byte[] encryptionResult = new byte[identityKeys.length];

            entropyHarvester.fetchRandom(this.initializationVector);

            this.updateIdentityPlaintext();

            if(Build.VERSION.BASE_OS != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Key keySpec = new SecretKeySpec(key, "AES");
                Cipher cipher = Cipher.getInstance("AES_256/GCM/NoPadding");
                GCMParameterSpec params = new GCMParameterSpec(128, initializationVector);
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, params);
                cipher.updateAAD(identityPlaintext);
                cipher.update(identityKeys);
                encryptionResult = cipher.doFinal();

                this.identityMasterKeyEncrypted = Arrays.copyOfRange(encryptionResult, 0, 32);
                this.identityLockKeyEncrypted = Arrays.copyOfRange(encryptionResult, 32, 64);
                this.identityVerificationTag = Arrays.copyOfRange(encryptionResult, 64, 80);
            } else {
                byte[] resultVerificationTag = new byte[16];

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

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return false;
        }
        return true;
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
                BLOCK_SIZE_LENGTH +
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
                BLOCK_SIZE_LENGTH +
                newPlaintext.length +
                rescueIdentityLockKeyEncrypted.length +
                rescueVerificationTag.length
            ), newPlaintext);
        rescuePlaintext = newPlaintext;
    }

    private void updatePreviousPlaintext() {
        if(!hasPreviousBlock) return;

        if (previousCountOfKeys > 0) {
            byte[] newPlaintext = getIntToTwoBytes(PREVIOUS_IDENTITY_KEYS);
            newPlaintext = EncryptionUtils.combine(newPlaintext, getIntToTwoBytes(previousCountOfKeys));
            newPlaintext = EncryptionUtils.combine(
                this.getIntToTwoBytes(
                    BLOCK_SIZE_LENGTH +
                    newPlaintext.length +
                    previousKey1.length * previousCountOfKeys
                ), newPlaintext);
            rescuePlaintext = newPlaintext;
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
            result = EncryptionUtils.combine(result, rescueIdentityLockKeyEncrypted);
            result = EncryptionUtils.combine(result, rescueVerificationTag);
        }

        if (hasPreviousBlock && previousCountOfKeys > 0) {
            result = EncryptionUtils.combine(result, previousPlaintext);
            result = EncryptionUtils.combine(result, previousKey1);
            if (previousCountOfKeys > 1) {
                result = EncryptionUtils.combine(result, previousKey2);
            }
            if (previousCountOfKeys > 2) {
                result = EncryptionUtils.combine(result, previousKey3);
            }
            if (previousCountOfKeys > 3) {
                result = EncryptionUtils.combine(result, previousKey4);
            }
            result = EncryptionUtils.combine(result, previousVerificationTag);
        }
        return result;
    }


    public String getServerUnlockKey(EntropyHarvester entropyHarvester) {
        /*
        VerifyUnlock := 	SignPublic( DHKA( IdentityLock, RandomLock ))
        ServerUnlock := 	MakePublic( RandomLock )
        */
        try {
            byte[] randomLock = new byte[32];
            entropyHarvester.fetchRandom(randomLock);

            byte[] bytesToSign = new byte[32];
            byte[] serverUnlock = new byte[32];
            byte[] notimportant = new byte[32];
            byte[] verifyUnlock = new byte[32];

            Sodium.crypto_scalarmult(bytesToSign, this.identityLockKey, randomLock);
            Sodium.crypto_sign_seed_keypair(notimportant, verifyUnlock, bytesToSign);
            Sodium.crypto_sign_ed25519_sk_to_pk(serverUnlock, randomLock);

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
            //String rawQRCodeData = "71a48c7371726c3a2f2f7371";
            String rawQRCodeData = "4ce7371726c646174617d0001002d0031d32536a67c4661faef7631d4a7854da64297af4bda01438eca39a40921000000f10104010f0085de0eea7b76134eee4e0b2d638955ad5fd253d857c86177151d30a956182abc55b80316da5c22fcc9b92c4f5a4850f5a4ecb6b948f05b297de13b1a7698cbee461c5e7ef0f8759e659a7ad853555be64900020079d1d5da2d4b046212f43da2f7b0a39709ca000000d5dd50893d516c8175291f7d905b5bf5636d26fee5d3f8801375f7824b09a2a824de7fc41451ca13e610d5591d568db60ec11ec11ec11ec11ec11ec11ec11ec11ec11ec11ec11ec11";
            byte[] bytesArray = EncryptionUtils.readSQRLQRCode(EncryptionUtils.hex2Byte(rawQRCodeData));

            SQRLStorage storage = SQRLStorage.getInstance();
            storage.read(bytesArray, true);

            byte[] saveData = storage.createSaveData();

            System.out.println(EncryptionUtils.byte2hex(bytesArray));
            System.out.println(EncryptionUtils.byte2hex(saveData));

            System.out.println(Arrays.equals(bytesArray, saveData));

            /*
            File file = new File("Testing.sqrl");
            byte[] bytesArray = new byte[(int) file.length()];

            FileInputStream fis = new FileInputStream(file);
            fis.read(bytesArray);
            fis.close();

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update("tfJ8CRxisuQQGY3KRcv".getBytes("US-ASCII"));
            md.update((byte)0);
            BigInteger reminder = new BigInteger(1, md.digest()).mod(BigInteger.valueOf(56));
            System.out.println(reminder.intValue());

            System.out.println(EncryptionUtils.byte2hex(bytesArray));
            SQRLStorage storage = SQRLStorage.getInstance();
            storage.read(bytesArray, true);
            //storage.decryptIdentityKey("Testing1234");
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