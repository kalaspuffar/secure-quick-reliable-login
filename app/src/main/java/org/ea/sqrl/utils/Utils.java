package org.ea.sqrl.utils;

import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;

import com.google.zxing.FormatException;
import com.google.zxing.common.BitSource;
import com.google.zxing.qrcode.decoder.Mode;
import com.google.zxing.qrcode.decoder.Version;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Daniel Persson
 */
public class Utils {
    private static final String TAG = "EncryptionUtils";

    private static final Map<String, int[]> errorBits;
    static {
        Map<String, int[]> aMap = new HashMap<>();
        aMap.put("L", new int[] {
                0, 152, 272, 440, 640, 864, 1088, 1248, 1552, 1856, 2192,
                2592, 2960, 3424, 3688, 4184, 4712, 5176, 5768, 6360, 6888,
                7456, 8048, 8752, 9392, 10208, 10960, 11744, 12248, 13048, 13880,
                14744, 15640, 16568, 17528, 18448, 19472, 20528, 21616, 22496, 23648
        });
        aMap.put("M", new int[] {
                0, 128, 224, 352, 512, 688,  864,  992, 1232, 1456, 1728,
                2032, 2320, 2672, 2920, 3320, 3624, 4056, 4504, 5016, 5352,
                5712, 6256, 6880, 7312, 8000, 8496, 9024, 9544, 10136, 10984,
                11640, 12328, 13048, 13800, 14496, 15312, 15936, 16816, 17728, 18672
        });
        aMap.put("Q", new int[] {
                0, 104, 176, 272, 384, 496,  608,  704,  880, 1056, 1232,
                1440, 1648, 1952, 2088, 2360, 2600, 2936, 3176, 3560, 3880,
                4096, 4544, 4912, 5312, 5744, 6032, 6464, 6968, 7288, 7880,
                8264, 8920, 9368, 9848, 10288, 10832, 11408, 12016, 12656, 13328
        });
        aMap.put("H", new int[] {
                0,  72, 128, 208, 288, 368,  480,  528,  688,  800,  976,
                1120, 1264, 1440, 1576, 1784, 2024, 2264, 2504, 2728, 3080,
                3248, 3536, 3712, 4112, 4304, 4768, 5024, 5288, 5608, 5960,
                6344, 6760, 7208, 7688, 7888, 8432, 8768, 9136, 9776, 10208
        });
        errorBits = Collections.unmodifiableMap(aMap);
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
    public static byte[] readSQRLQRCode(byte[] rawHexData, String errorLevel) throws FormatException {
        BitSource bits = new BitSource(rawHexData);
        byte[] resultBytes = new byte[0];

        int availableBits = bits.available();

        int version = 0;
        for(int i : errorBits.get(errorLevel)) {
            if(i > availableBits) break;
            version++;
        }

        Mode mode;
        do {
            // While still another segment to read...
            if (bits.available() < 4) {
                // OK, assume we're done. Really, a TERMINATOR mode should have been recorded here
                mode = Mode.TERMINATOR;
            } else {
                mode = Mode.forBits(bits.readBits(4)); // mode is encoded by 4 bits
            }
            if (mode != Mode.TERMINATOR) {
                if (mode == Mode.BYTE) {
                    int count = bits.readBits(mode.getCharacterCountBits(Version.getVersionForNumber(version)));
                    resultBytes = decodeByteSegment(bits, count);
                } else {
                    System.out.println("Incorrect mode "+mode);
                    throw FormatException.getFormatInstance();
                }
            }
        } while (mode != Mode.TERMINATOR);
        return resultBytes;
    }

    private static byte[] decodeByteSegment(BitSource bits, int count) throws FormatException {
        if (8 * count > bits.available()) {
            throw FormatException.getFormatInstance();
        }

        byte[] readBytes = new byte[count];
        for (int i = 0; i < count; i++) {
            readBytes[i] = (byte) bits.readBits(8);
        }

        return readBytes;
    }


    public static String readSQRLQRCodeAsString(byte[] rawHexData, String errorLevel) {
        try {
            return new String(readSQRLQRCode(rawHexData, errorLevel), "ASCII");
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return "";
    }

    public static int getInteger(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException nfe) {
            return -1;
        }
    }

    public static void showToast(View relativeLayout, int id) {
        Snackbar snackbar = Snackbar.make(relativeLayout, id, Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction("dismiss", v -> snackbar.dismiss());
    }

    public static void showToast(View relativeLayout, String message) {
        Snackbar snackbar = Snackbar.make(relativeLayout, message, Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction("dismiss", v -> snackbar.dismiss());
    }
}
