/* 
 * QR Code generator library (Java)
 * 
 * Copyright (c) Project Nayuki. (MIT License)
 * https://www.nayuki.io/page/qr-code-generator-library
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * - The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 * - The Software is provided "as is", without warranty of any kind, express or
 *   implied, including but not limited to the warranties of merchantability,
 *   fitness for a particular purpose and noninfringement. In no event shall the
 *   authors or copyright holders be liable for any claim, damages or other
 *   liability, whether in an action of contract, tort or otherwise, arising from,
 *   out of or in connection with the Software or the use or other dealings in the
 *   Software.
 */

package io.nayuki.qrcodegen;

import java.util.List;
import java.util.regex.Pattern;


/**
 * Represents a character string to be encoded in a QR Code symbol. Each segment has
 * a mode, and a sequence of characters that is already encoded as a sequence of bits.
 * Instances of this class are immutable.
 * <p>This segment class imposes no length restrictions, but QR Codes have restrictions.
 * Even in the most favorable conditions, a QR Code can only hold 7089 characters of data.
 * Any segment longer than this is meaningless for the purpose of generating QR Codes.</p>
 */
public final class QrSegment {
	
	/*---- Static factory functions ----*/

    /**
     * Returns a segment representing the specified binary data encoded in byte mode.
     * @param data the binary data
     * @return a segment containing the data
     * @throws NullPointerException if the array is {@code null}
     */
    public static QrSegment makeBytes(byte[] data) {
        BitBuffer bb = new BitBuffer();
        for (byte b : data)
            bb.appendBits(b & 0xFF, 8);
        return new QrSegment(Mode.BYTE, data.length, bb);
    }


    /**
     * Returns a segment representing the specified string of decimal digits encoded in numeric mode.
     * @param digits a string consisting of digits from 0 to 9
     * @return a segment containing the data
     * @throws NullPointerException if the string is {@code null}
     * @throws IllegalArgumentException if the string contains non-digit characters
     */
    public static QrSegment makeNumeric(String digits) {
        if (!NUMERIC_REGEX.matcher(digits).matches())
            throw new IllegalArgumentException("String contains non-numeric characters");

        BitBuffer bb = new BitBuffer();
        int i;
        for (i = 0; i + 3 <= digits.length(); i += 3)  // Process groups of 3
            bb.appendBits(Integer.parseInt(digits.substring(i, i + 3)), 10);
        int rem = digits.length() - i;
        if (rem > 0)  // 1 or 2 digits remaining
            bb.appendBits(Integer.parseInt(digits.substring(i)), rem * 3 + 1);
        return new QrSegment(Mode.NUMERIC, digits.length(), bb);
    }

    /**
     * Returns a segment representing an Extended Channel Interpretation
     * (ECI) designator with the specified assignment value.
     * @param assignVal the ECI assignment number (see the AIM ECI specification)
     * @return a segment containing the data
     * @throws IllegalArgumentException if the value is outside the range [0, 10<sup>6</sup>)
     */
    public static QrSegment makeEci(int assignVal) {
        BitBuffer bb = new BitBuffer();
        if (0 <= assignVal && assignVal < (1 << 7))
            bb.appendBits(assignVal, 8);
        else if ((1 << 7) <= assignVal && assignVal < (1 << 14)) {
            bb.appendBits(2, 2);
            bb.appendBits(assignVal, 14);
        } else if ((1 << 14) <= assignVal && assignVal < 1000000) {
            bb.appendBits(6, 3);
            bb.appendBits(assignVal, 21);
        } else
            throw new IllegalArgumentException("ECI assignment value out of range");
        return new QrSegment(Mode.ECI, 0, bb);
    }
	
	
	
	/*---- Instance fields ----*/

    /** The mode indicator for this segment. Never {@code null}. */
    public final Mode mode;

    /** The length of this segment's unencoded data, measured in characters. Always zero or positive. */
    public final int numChars;

    /** The data bits of this segment. Accessed through getBits(). Not {@code null}. */
    final BitBuffer data;
	
	
	/*---- Constructor ----*/

    /**
     * Creates a new QR Code data segment with the specified parameters and data.
     * @param md the mode, which is not {@code null}
     * @param numCh the data length in characters, which is non-negative
     * @param data the data bits of this segment, which is not {@code null}
     * @throws NullPointerException if the mode or bit buffer is {@code null}
     * @throws IllegalArgumentException if the character count is negative
     */
    public QrSegment(Mode md, int numCh, BitBuffer data) {
        if (numCh < 0)
            throw new IllegalArgumentException("Invalid value");
        mode = md;
        numChars = numCh;
        this.data = data.clone();  // Make defensive copy
    }
	
	
	/*---- Methods ----*/

    /**
     * Returns the data bits of this segment.
     * @return the data bits of this segment (not {@code null})
     */
    public BitBuffer getBits() {
        return data.clone();  // Make defensive copy
    }


    // Package-private helper function.
    static int getTotalBits(List<QrSegment> segs, int version) {
        if (version < 1 || version > 40)
            throw new IllegalArgumentException("Version number out of range");

        long result = 0;
        for (QrSegment seg : segs) {
            int ccbits = seg.mode.numCharCountBits(version);
            // Fail if segment length value doesn't fit in the length field's bit-width
            if (seg.numChars >= (1 << ccbits))
                return -1;
            result += 4L + ccbits + seg.data.bitLength();
            if (result > Integer.MAX_VALUE)
                return -1;
        }
        return (int)result;
    }
	
	
	/*---- Constants ----*/

    /** Can test whether a string is encodable in numeric mode (such as by using {@link #makeNumeric(String)}). */
    public static final Pattern NUMERIC_REGEX = Pattern.compile("[0-9]*");

    /** Can test whether a string is encodable in alphanumeric mode (such as by using makeAlphanumeric(String)). */
    public static final Pattern ALPHANUMERIC_REGEX = Pattern.compile("[A-Z0-9 $%*+./:-]*");

    /** The set of all legal characters in alphanumeric mode, where each character value maps to the index in the string. */
    private static final String ALPHANUMERIC_CHARSET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:";
	
	
	
	/*---- Public helper enumeration ----*/

    /**
     * The mode field of a segment. Immutable. Provides methods to retrieve closely related values.
     */
    public enum Mode {
		
		/*-- Constants --*/

        NUMERIC     (0x1, 10, 12, 14),
        ALPHANUMERIC(0x2,  9, 11, 13),
        BYTE        (0x4,  8, 16, 16),
        KANJI       (0x8,  8, 10, 12),
        ECI         (0x7,  0,  0,  0);
		
		
		/*-- Fields --*/

        /** An unsigned 4-bit integer value (range 0 to 15) representing the mode indicator bits for this mode object. */
        final int modeBits;

        private final int[] numBitsCharCount;
		
		
		/*-- Constructor --*/

        Mode(int mode, int... ccbits) {
            this.modeBits = mode;
            numBitsCharCount = ccbits;
        }
		
		
		/*-- Method --*/

        /**
         * Returns the bit width of the segment character count field for this mode object at the specified version number.
         * @param ver the version number, which is between 1 to 40, inclusive
         * @return the number of bits for the character count, which is between 8 to 16, inclusive
         * @throws IllegalArgumentException if the version number is out of range
         */
        int numCharCountBits(int ver) {
            if      ( 1 <= ver && ver <=  9)  return numBitsCharCount[0];
            else if (10 <= ver && ver <= 26)  return numBitsCharCount[1];
            else if (27 <= ver && ver <= 40)  return numBitsCharCount[2];
            else  throw new IllegalArgumentException("Version number out of range");
        }

    }

}