package org.ea.sqrl.storage;

import java.util.Arrays;
import java.util.Base64;

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
        if (!STORAGE_HEADER.equals(header)) throw new Exception("Incorrect header");
        int readOffset = 8;
        int readLen = readOffset + 2;
        boolean addedKeys = false;
        while (input.length > readLen) {
            int len = getIntFromTwoBytes(input, readOffset);
            System.out.println(len);
            if (readOffset + len > input.length)
                throw new Exception(
                        "Incorrect length of block offset " + readOffset + " len " + len + " input len "+input.length
                );
            handleBlock(Arrays.copyOfRange(input, readOffset, readOffset + len));
            readOffset += len;
            if(!addedKeys) {
                readOffset -= 1;
                readOffset += 32;
                readOffset += 32;
                readOffset += 16;
                addedKeys = false;
            }
            readLen = readOffset + 2;
        }
    }

    public void handleBlock(byte[] input) throws Exception {
        int len = getIntFromTwoBytes(input, 0);
        int type = getIntFromTwoBytes(input, 2);
        switch (type) {
            case PASSWORD_PBKDF:
                int ptLen = getIntFromTwoBytes(input, 4);
                System.out.println("Key block "+ptLen);
                break;
            case RESCUECODE_PBKDF:
                System.out.println("Identity block");
                break;
            case PREVIOUS_IDENTITY_KEYS:
                System.out.println("Previous identity block");
                break;
            default:
                throw new Exception("Unknown type "+type);
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
    public int getIntFromTwoBytes(byte[] input, int offset) {
        return (input[offset] & 0xff) | ((input[offset + 1] & 0xff) << 8);
    }

    @Override
    public String toString() {
        return STORAGE_HEADER;
    }

    public static String input =
            "c3FybGRhdGF9AAEALQAPHe+/ve+/ve+/vT4w77+9Ru+/ve+/vTLvv70P77+977+9H2Hvv71k77+9" +
                    "77+9BUDvv73vv70aCe+/vQAAAO+/vQEEBQ8AxKkAI++/vVDvv73WhGh8M3rvv73vv70777+977+9" +
                    "Ou+/ve+/vdmZH++/vQHvv70YF2M+37FPFTrvv73vv71H77+977+9S2pg77+9be+/vQkV77+9Le+/" +
                    "ve+/vVDvv73vv71S77+9MF3vv71YfjdQS++/vTw077+9ATvvv73vv71JAAIAee+/ve+/ve+/vS1L" +
                    "BGIS77+9Pe+/ve+/vQnvv70AAADvv73vv71Q77+9PVFs77+9dSkffe+/vVtb77+9Y20m77+977+9" +
                    "77+977+9E3Xvv71LCe+/ve+/vSTvv71/77+9FFHvv70T77+9EO+/vVkdVu+/ve+/vQ==";

    public static void main(String[] args) {
        try {
            SQRLStorage storage = new SQRLStorage(input, true);
            System.out.println(storage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
