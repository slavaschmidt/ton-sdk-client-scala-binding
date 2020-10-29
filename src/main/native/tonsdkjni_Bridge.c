#include <stdint.h>
#include <jni.h>
#include "tonclient.h"
#include "tonsdkjni_Bridge.h"

static tc_string_data_t from_jstring(JNIEnv *env, jstring string) {
    const char* chars = (*env)->GetStringUTFChars(env, string, NULL);
    const size_t length = (size_t)(*env)->GetStringUTFLength(env, string);
    const tc_string_data_t data = {chars, length};
    return data;
}

static jbyteArray to_jstring(JNIEnv *env, const tc_string_handle_t* string_ptr) {
    const tc_string_data_t string = tc_read_string(string_ptr);
    const jbyteArray array = (*env)->NewByteArray(env, string.len);
    (*env)->SetByteArrayRegion(env, array, 0, string.len, (jbyte*)string.content);
    tc_destroy_string(string_ptr);
    return array;
}

/*
 * Class:     tonsdkjni_Bridge
 * Method:    tcCreateContext
 * Signature: (Ljava/lang/String;)[B
 */
JNIEXPORT jbyteArray JNICALL Java_tonsdkjni_Bridge_tcCreateContext
  (JNIEnv *env, jobject jobj, jstring config) {
    const tc_string_data_t config_data = from_jstring(env, config);
    const tc_string_handle_t* json_ptr = tc_create_context(config_data);
    (*env)->ReleaseStringUTFChars(env, config, config_data.content);
    return to_jstring(env, json_ptr);
  }

/*
 * Class:     tonsdkjni_Bridge
 * Method:    tcDestroyContext
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_tonsdkjni_Bridge_tcDestroyContext
  (JNIEnv * env, jobject jobj, jlong context) {
    tc_destroy_context((uint32_t)context);
  }


/*
 * Class:     tonsdkjni_Bridge
 * Method:    tcRequest
 * Signature: (JLjava/lang/String;Ljava/lang/String;JLscala/Function4;)V
 */
JNIEXPORT void JNICALL Java_tonsdkjni_Bridge_tcRequest
  (JNIEnv * env, jobject jobj, jlong context, jstring name, jstring params, jlong request, jobject handler) {
    // TODO
  }

/*
 * Class:     tonsdkjni_Bridge
 * Method:    tcRequestSync
 * Signature: (JLjava/lang/String;Ljava/lang/String;)[B
 */
JNIEXPORT jbyteArray JNICALL Java_tonsdkjni_Bridge_tcRequestSync
  (JNIEnv * env, jobject jobj, jlong context, jstring name, jstring params) {
      const tc_string_data_t name_data = from_jstring(env, name);
      const tc_string_data_t params_data = from_jstring(env, params);
      const tc_string_handle_t* json_ptr = tc_request_sync((uint32_t)context, name_data, params_data);
      (*env)->ReleaseStringUTFChars(env, name, name_data.content);
      (*env)->ReleaseStringUTFChars(env, params, params_data.content);
      return to_jstring(env, json_ptr);
  }



