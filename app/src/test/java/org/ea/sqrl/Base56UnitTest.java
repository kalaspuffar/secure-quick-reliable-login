package org.ea.sqrl;

import org.ea.sqrl.utils.EncryptionUtils;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * This testcase will check that an identity can be decoded and encoded without missing data.
 */
public class Base56UnitTest {
    @Test
    public void testEncodeAndDecodeNormal() throws Exception {
        final String testIdentity = "KKUtzSvTsNiNDdPQZdqCpZJCwzCdyQh6kk9vU7wRg6trU6cP6xVqLvNAff4iNv2PW8sw3tYcu7CaxQ5trcTCeB7WbaeDjxTbh6VEiNNPNBd";
        byte[] decodedBytes = EncryptionUtils.decodeBase56(testIdentity);
        String encoded = EncryptionUtils.encodeBase56(decodedBytes);

        assertEquals("Encoding should be the same before and after", testIdentity, encoded);
    }

    @Test
    public void testEncodeAndDecodePadded() throws Exception {
        final String testIdentity = "jwvG2Ee7cTT55NxVt5Ja8b32urmmmvbYyMDMKQR3Q2eXMsAKJCYg6SqaihcN69s8P3reQz2xxhy4eRyDgTc3F5zrSmMhSF9SwrDqRAU7E2u";
        byte[] decodedBytes = EncryptionUtils.decodeBase56(testIdentity);
        String encoded = EncryptionUtils.encodeBase56(decodedBytes);

        assertEquals("Encoding should be the same before and after", testIdentity, encoded);
    }

    @Test
    public void testEncodeAndDecodePaddedLong() throws Exception {
        final String testIdentity = "tP8zx8kY8EMPLqzWVq6aPL2my25Pc6KwrVyrUQwRiBKwRc6FPAmPumtrGQ8CtBhxqkRLExuE7tsFbrzUTfb2qSZ9eqDbcfjv8Zdqi6DXa5Ztn3CWsVAZFFF6jafiyixYhGqSeZdg4zjCi7cMgEsBYwWHevsB2H6y9gam2GbXk5A4SSek4Rmrmx64qxUnQJer62WnGcWArqCBzQYVw4GcvAsBjvjDvjN7RhKGdhLpLMTKB7DpfTkhCeaPDWCZxy3AVzxbwKSdjHcZmnaMegiE2t";
        byte[] decodedBytes = EncryptionUtils.decodeBase56(testIdentity);
        String encoded = EncryptionUtils.encodeBase56(decodedBytes);

        assertEquals("Encoding should be the same before and after", testIdentity, encoded);
    }

    @Test
    public void testEncodeAndDecodeFullFormatVectors() throws Exception {
        List<List<String>> vectors = TestHelper.parseVectorCsvFile(
                "base56-full-format-vectors.txt", true, true );

        int vectorNumber = 1;
        for (List<String> vector: vectors) {
            byte[] input = EncryptionUtils.hex2Byte(vector.get(1));
            String expectedResult = vector.get(2).replace(" ", "").replace("\\n", "");

            String encoded = EncryptionUtils.encodeBase56(input);
            byte[] decoded = EncryptionUtils.reverse(EncryptionUtils.decodeBase56(encoded));

            assertEquals("encodeBase56 / vector # " + vectorNumber +
                    ": Encoding should match result in vector file", expectedResult, encoded);

            assertArrayEquals("decodeBase56 / vector # " + vectorNumber +
                    ": Decoded result should match input in vector file", input, decoded);

            vectorNumber++;
        }
    }

    @Test
    public void testEncodeAndDecodeVectors() throws Exception {
        List<List<String>> vectors = TestHelper.parseVectorCsvFile(
                "base56-vectors.txt", true, true );

        int vectorNumber = 1;
        for (List<String> vector: vectors) {
            byte[] input = EncryptionUtils.hex2Byte(vector.get(0));
            String lineNum = vector.get(1);
            String expectedResult = vector.get(2);
            String expectedCheckChar = vector.get(3);

            String encoded = EncryptionUtils.encodeBase56(input);

            // Remove check character, this will be verified separately
            encoded = encoded.substring(0, encoded.length()-1);

            assertEquals("encodeBase56 / vector # " + vectorNumber +
                    ": Encoding should match result in vector file", expectedResult, encoded);

            //TODO: Add check character validation
            // This cannot be done sanely at the moment, because the check character
            // creation is inlined in the encode/decode/verify functions, and there is
            // no way to pass in the line number. This should definitely be pulled out
            // into a separate function!

            //TODO: Add decoding validation
            // This cannot be done sanely at the moment, because the check character
            // creation is inlined in the encode/decode/verify functions, and there is
            // no way to pass in the line number. This should definitely be pulled out
            // into a separate function!

            vectorNumber++;
        }
    }
}