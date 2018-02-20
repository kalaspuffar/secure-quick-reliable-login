package org.ea.sqrl.jni;

/**
 * This is a JNI bridge to the GRC AesGcm functionallity implemented by Steve and used when native
 * implementation is not available on the platform. In Android Oreo and above AESGCM is present on
 * the platform.
 *
 * @author Daniel Persson
 */
public class Grc_aesgcm {
    public static native int gcm_initialize();

    public static native int gcm_setkey(byte[] key, int keysize);

    public static native int gcm_auth_decrypt(
            byte[] iv, int iv_len,
            byte[] add, int add_len,
            byte[] input, byte[] output, int length,
            byte[] tag, int tag_len
    );

    public static native int gcm_encrypt_and_tag(
            byte[] iv, int iv_len,
            byte[] add, int add_len,
            byte[] input, byte[] output, int length,
            byte[] tag, int tag_len
    );

    public static native void gcm_zero_ctx();

    static {
        System.loadLibrary("grc-aesgcm");
    }
}
