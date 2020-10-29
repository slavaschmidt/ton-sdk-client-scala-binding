#include <stdint.h>
#include <jni.h>
#include "tonclient.h"
#include "TonSdkJniBridge.h"
/*
 * Class:     TonSdkJniBridge
 * Method:    tcCreateContext
 * Signature: (Ljava/lang/String;)[B
 */

JNIEXPORT jbyteArray JNICALL Java_TonSdkJniBridge_tcCreateContext
  (JNIEnv *env, jobject jobj, jstring string) {
    const char* chars = (*env)->GetStringUTFChars(env, string, NULL);
    const size_t length = (size_t)(*env)->GetStringUTFLength(env, string);
    const tc_string_data_t string_data = {chars, length};

    const tc_string_handle_t* json_ptr = tc_create_context(string_data);
    const tc_string_data_t json = tc_read_string(json_ptr);

    const jbyteArray array = (*env)->NewByteArray(env, json.len);
    (*env)->SetByteArrayRegion(env, array,0,json.len,(jbyte*)json.content);
    (*env)->ReleaseStringUTFChars(env, string, chars);
    tc_destroy_string(json_ptr);

    return array;
  }

/*
 * Class:     TonSdkJniBridge
 * Method:    tcDestroyContext
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_TonSdkJniBridge_tcDestroyContext
  (JNIEnv * env, jobject jobj, jlong context) {
    tc_destroy_context((uint32_t)context);
  }
