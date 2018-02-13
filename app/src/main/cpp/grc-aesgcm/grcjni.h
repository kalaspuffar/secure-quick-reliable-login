#ifndef SECURE_QUICK_RESPONSE_LOGIN_JNI_H
#define SECURE_QUICK_RESPONSE_LOGIN_JNI_H

#include <jni.h>
#include <android/log.h>

JNIEXPORT jint JNICALL
Java_org_ea_sqrl_jni_Grc_aesgcm_gcm_initialize(JNIEnv* env, jclass type);

JNIEXPORT jint JNICALL
Java_org_ea_sqrl_jni_Grc_1aesgcm_gcm_1setkey(JNIEnv *env, jclass type, jbyteArray key_, jint keysize);

JNIEXPORT jint JNICALL
Java_org_ea_sqrl_jni_Grc_1aesgcm_gcm_1auth_1decrypt(JNIEnv *env, jclass type, jbyteArray iv_,
                                                    jint iv_len, jbyteArray add_, jint add_len,
                                                    jbyteArray input_, jbyteArray output_,
                                                    jint length, jbyteArray tag_, jint tag_len);

#endif //SECURE_QUICK_RESPONSE_LOGIN_JNI_H
