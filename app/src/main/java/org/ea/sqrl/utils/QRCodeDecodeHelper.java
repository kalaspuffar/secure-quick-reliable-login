package org.ea.sqrl.utils;

import com.google.zxing.FormatException;
import com.google.zxing.common.DecoderResult;
import com.google.zxing.qrcode.decoder.Version;

public class QRCodeDecodeHelper {
    public static byte[] decode(byte[] bytes) throws FormatException {
        DecoderResult dr = QRCodeDecodedBitStreamParser.decode(bytes, Version.getVersionForNumber(1), null, null);
        byte[] qrCode = new byte[0];
        for(byte[] newSegment : dr.getByteSegments()) {
            qrCode = EncryptionUtils.combine(qrCode, newSegment);
        }
        return qrCode;
    }
}
