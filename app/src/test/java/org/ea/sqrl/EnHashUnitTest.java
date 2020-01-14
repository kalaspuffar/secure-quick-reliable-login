package org.ea.sqrl;

import org.ea.sqrl.utils.EncryptionUtils;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertArrayEquals;

public class EnHashUnitTest {
    @Test
    public void testEnHashVectors() throws Exception {
        List<List<String>> vectors = TestHelper.parseVectorCsvFile(
                "enhash-vectors.txt", true, true );

        int vectorNumber = 1;
        for (List<String> vector: vectors) {
            byte[] input = TestHelper.base64UrlDecode(vector.get(0));
            byte[] expectedResult = TestHelper.base64UrlDecode(vector.get(1));

            byte[] result = EncryptionUtils.enHash(input);

            assertArrayEquals("testEnHashVectors / vector # " + vectorNumber +
                    ": Decoded result should match result in vector file", expectedResult, result);

            vectorNumber++;
        }
    }
}
