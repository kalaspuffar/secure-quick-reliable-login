package org.ea.sqrl.storage;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class SQRLStorage {
    private static final String STORAGE_HEADER = "sqrldata";


    private static final int PASSWORD_PBKDF = 1;
    private static final int RESCUECODE_PBKDF = 2;
    private static final int PREVIOUS_IDENTITY_KEYS = 3;

    public SQRLStorage(String input, boolean full) throws Exception {
        this(Base64.getDecoder().decode(input), full);
    }

    public SQRLStorage(byte[] input, boolean full) throws Exception {
        String header = new String(Arrays.copyOfRange(input, 0, 8));
/*
        System.out.println(input.length);
        for (int i = 0; i < input.length - 1; i++)
            System.out.println(i + " = " + getIntFromTwoBytes(input, i));
*/
        if (!STORAGE_HEADER.equals(header)) throw new Exception("Incorrect header");
        int readOffset = 8;
        int readLen = readOffset + 2;
        boolean addedKeys = false;
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
    private byte[] initializationVector;
    private byte[] randomSalt;
    private byte   logNFactor;
    private int iterationCount;
    private int optionFlags;
    private byte hintLength;
    private byte timeInSecondsToRunPWEnScryptOnPassword;
    private int idleTimoutInMinutes;
    private byte[] identityMasterKey;
    private byte[] identityLockKey;
    private byte[] verificationTag;


    public void handlePasswordBlock(byte[] input) {
        plaintextLength = getIntFromTwoBytes(input, 4);
        initializationVector = Arrays.copyOfRange(input, 6, 18);
        randomSalt = Arrays.copyOfRange(input, 18, 34);
        logNFactor = input[34];
        iterationCount = getIntFromFourBytes(input, 35);
        optionFlags = getIntFromTwoBytes(input, 39);
        hintLength = input[41];
        timeInSecondsToRunPWEnScryptOnPassword = input[42];
        idleTimoutInMinutes = getIntFromTwoBytes(input, 43);
        identityMasterKey = Arrays.copyOfRange(input, 45, 77);
        identityLockKey = Arrays.copyOfRange(input, 77, 109);
        verificationTag = Arrays.copyOfRange(input, 109, 125);
    }

    private byte[] rescue_randomSalt;
    private byte   rescue_logNFactor;
    private int rescue_iterationCount;
    private byte[] rescue_identityLockKey;
    private byte[] rescue_verificationTag;

    public void handleIdentityBlock(byte[] input) {
        rescue_randomSalt = Arrays.copyOfRange(input, 4, 20);
        rescue_logNFactor = input[20];
        rescue_iterationCount = getIntFromFourBytes(input, 21);
        rescue_identityLockKey = Arrays.copyOfRange(input, 25, 57);
        rescue_verificationTag = Arrays.copyOfRange(input, 57, 73);
    }

    private int previous_countOfKeys;
    private byte[] previous1;
    private byte[] previous2;
    private byte[] previous3;
    private byte[] previous4;
    private byte[] previous_verificationTag;

    public void handlePreviousIdentityBlock(byte[] input, int len) {
        previous_countOfKeys = getIntFromTwoBytes(input, 4);

        int lastKeyEnd = 6 + 32;
        previous1 = Arrays.copyOfRange(input, 6, lastKeyEnd);
        if(previous_countOfKeys > 1) {
            previous2 = Arrays.copyOfRange(input, lastKeyEnd, lastKeyEnd + 32);
            lastKeyEnd += 32;
        }
        if(previous_countOfKeys > 2) {
            previous3 = Arrays.copyOfRange(input, lastKeyEnd, lastKeyEnd + 32);
            lastKeyEnd += 32;
        }
        if(previous_countOfKeys > 3) {
            previous4 = Arrays.copyOfRange(input, lastKeyEnd, lastKeyEnd + 32);
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

    public int getIntFromTwoBytes(byte[] input, int offset) {
        return (input[offset] & 0xff) | ((input[offset + 1] & 0xff) << 8);
    }

    public int getIntFromFourBytes(byte[] input, int offset) {
        return (input[offset] & 0xff) | ((input[offset + 1] & 0xff) << 8) | (input[offset + 2] & 0xff) << 16 | ((input[offset + 3] & 0xff) << 24);
    }


    @Override
    public String toString() {
        return STORAGE_HEADER;
    }

    public static String input =
        "c3FybGRhdGF9AAEALQAPHT8/Pz4wP0Y/Pz8yPw8/Px9hP2Q/PwVAPz8aCT8AAAA/AQQFDwA/PwAj" +
        "P1A/Pz9ofDN6Pz87Pz8/Pz86Pz8/Pz8/Hz8BPxgXYz4/P08VOj8/Rz8/S2pgP20/CRU/LT8/UD8/" +
        "P1I/MF0/WH43UEs/PDQ/ATs/P0kAAgB5Pz8/LUsEYhI/PT8/Pz8/CT8AAAA/P1A/PVFsP3UpH30/" +
        "W1s/Y20mPz8/Pz8TdT8/Swk/PyQ/fz8UUT8TPxA/WR1WPz8=";

    public static void main(String[] args) {
        try {
            //File f = new File("c:/tmp/Testing.sqrl");
            //BufferedReader br = new BufferedReader(new FileReader(f));
            //String line = br.readLine();
            //System.out.println(Base64.getEncoder().encodeToString(line.getBytes("ASCII")));

            SQRLStorage storage = new SQRLStorage(input, true);
            System.out.println(storage);
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