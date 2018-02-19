package org.ea.sqrl.jni;

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
