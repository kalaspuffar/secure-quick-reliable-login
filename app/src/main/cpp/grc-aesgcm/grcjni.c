#include <stdlib.h>
#include "grcjni.h"
#include "gcm.h"
#include "aes.h"

gcm_context* workContext;

JNIEXPORT jint JNICALL
Java_org_ea_sqrl_jni_Grc_1aesgcm_gcm_1initialize(JNIEnv *env, jclass type) {
    workContext = malloc(sizeof(gcm_context));
    return gcm_initialize();
}

JNIEXPORT jint JNICALL
Java_org_ea_sqrl_jni_Grc_1aesgcm_gcm_1setkey(JNIEnv *env, jclass type, jbyteArray key_,
        jint keysize) {
    jbyte *key = (*env)->GetByteArrayElements(env, key_, NULL);

    int result = gcm_setkey(workContext, (const unsigned char*)key, keysize);

    (*env)->ReleaseByteArrayElements(env, key_, key, 0);
    return result;
}

JNIEXPORT jint JNICALL
Java_org_ea_sqrl_jni_Grc_1aesgcm_gcm_1auth_1decrypt(JNIEnv *env, jclass type, jbyteArray iv_,
                                                    jint iv_len, jbyteArray add_, jint add_len,
                                                    jbyteArray input_, jbyteArray output_,
                                                    jint length, jbyteArray tag_, jint tag_len) {
    jbyte *iv = (*env)->GetByteArrayElements(env, iv_, NULL);
    jbyte *add = (*env)->GetByteArrayElements(env, add_, NULL);
    jbyte *input = (*env)->GetByteArrayElements(env, input_, NULL);
    jbyte *output = (*env)->GetByteArrayElements(env, output_, NULL);
    jbyte *tag = (*env)->GetByteArrayElements(env, tag_, NULL);

    int result = gcm_auth_decrypt(workContext,
                                  (const unsigned char*)iv, iv_len,
                                  (const unsigned char*)add, add_len,
                                  (const unsigned char*)input, (unsigned char*)output, length,
                                  (const unsigned char*)tag, tag_len
    );

    (*env)->SetByteArrayRegion(env, output_, 0, length, output);

    (*env)->ReleaseByteArrayElements(env, iv_, iv, 0);
    (*env)->ReleaseByteArrayElements(env, add_, add, 0);
    (*env)->ReleaseByteArrayElements(env, input_, input, 0);
    (*env)->ReleaseByteArrayElements(env, output_, output, 0);
    (*env)->ReleaseByteArrayElements(env, tag_, tag, 0);

    return result;
}

JNIEXPORT jint JNICALL
Java_org_ea_sqrl_jni_Grc_1aesgcm_gcm_1encrypt_1and_1tag(JNIEnv *env, jclass type, jbyteArray iv_,
                                                        jint iv_len, jbyteArray add_, jint add_len,
                                                        jbyteArray input_, jbyteArray output_,
                                                        jint length, jbyteArray tag_,
                                                        jint tag_len) {
    jbyte *iv = (*env)->GetByteArrayElements(env, iv_, NULL);
    jbyte *add = (*env)->GetByteArrayElements(env, add_, NULL);
    jbyte *input = (*env)->GetByteArrayElements(env, input_, NULL);
    jbyte *output = (*env)->GetByteArrayElements(env, output_, NULL);
    jbyte *tag = (*env)->GetByteArrayElements(env, tag_, NULL);

    int result = gcm_crypt_and_tag(workContext,
           ENCRYPT,
           (const unsigned char*)iv, iv_len,
           (const unsigned char*)add, add_len,
           (const unsigned char*)input, (unsigned char*)output, length,
           (unsigned char*)tag, tag_len
    );

    (*env)->SetByteArrayRegion(env, output_, 0, length, output);
    (*env)->SetByteArrayRegion(env, tag_, 0, tag_len, tag);

    (*env)->ReleaseByteArrayElements(env, iv_, iv, 0);
    (*env)->ReleaseByteArrayElements(env, add_, add, 0);
    (*env)->ReleaseByteArrayElements(env, input_, input, 0);
    (*env)->ReleaseByteArrayElements(env, output_, output, 0);
    (*env)->ReleaseByteArrayElements(env, tag_, tag, 0);

    return result;
}

JNIEXPORT void JNICALL
Java_org_ea_sqrl_jni_Grc_1aesgcm_gcm_1zero_1ctx(JNIEnv *env, jclass type) {
    gcm_zero_ctx(workContext);
}
