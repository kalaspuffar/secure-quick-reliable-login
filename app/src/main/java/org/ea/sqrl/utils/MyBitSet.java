package org.ea.sqrl.utils;

import java.util.Arrays;

/**
 *
 * @author Daniel Persson
 */
public class MyBitSet {
    boolean[] bits;

    private MyBitSet(boolean[] bits) {
        this.bits = bits;
    }

    public MyBitSet(byte[] bytes) {
        bits = new boolean[bytes.length * 8];

        int bitCount = 0;
        for(byte b : bytes) {
            for(int i = 0; i < 8; i++) {
                bits[bitCount] = getBit(b, i);
                bitCount++;
            }
        }
    }

    public MyBitSet get(int from, int to) {
        return new MyBitSet(Arrays.copyOfRange(bits, from, to));
    }

    public boolean getBit(byte b, int position) {
        return ((b >> position) & 1) == 1;
    }

    public int toInt() {
        int val = 0;
        int index = 0;
        for(boolean b : bits) {
            if(index > 32) break;
            if(index > 0) val = val << 1;
            val = val | (b ? 1 : 0);
            index++;
        }
        return val;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(boolean b : bits) {
            sb.append(b ? 1 : 0);
        }
        return sb.toString();
    }
}
