package org.ea.sqrl;

import org.ea.sqrl.utils.EncryptionUtils;
import org.junit.Test;

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
}