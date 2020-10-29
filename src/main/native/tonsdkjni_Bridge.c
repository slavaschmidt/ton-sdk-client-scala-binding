#include <stdint.h>
#include <jni.h>
#include <stdio.h>
#include "tonclient.h"
#include "tonsdkjni_Bridge.h"

typedef void (*j_tc_response_handler_t)(
    JNIEnv *env,
    jobject callbackInterface,
    uint32_t request_id,
    tc_string_data_t params_json,
    uint32_t response_type,
    bool finished);

static tc_string_data_t from_jstring(JNIEnv *env, jstring string) {
    const char* chars = (*env)->GetStringUTFChars(env, string, NULL);
    const size_t length = (size_t)(*env)->GetStringUTFLength(env, string);
    const tc_string_data_t data = {chars, length};
    return data;
}

static jbyteArray to_byte_array(JNIEnv *env, const tc_string_data_t string) {
    const jbyteArray array = (*env)->NewByteArray(env, string.len);
    (*env)->SetByteArrayRegion(env, array, 0, string.len, (jbyte*)string.content);
    return array;
}

static jstring to_jstring_from_data(JNIEnv* env, const tc_string_data_t string) {
    jclass str_class = (*env)->FindClass(env, "Ljava/lang/String;");
    jmethodID ctor_id = (*env)->GetMethodID(env, str_class, "<init>", "([BLjava/lang/String;)V");
    jstring encoding = (*env)->NewStringUTF(env, "utf-8");
    jbyteArray buf = to_byte_array(env, string);
    return (jstring)(*env)->NewObject(env, str_class, ctor_id, buf, encoding);
}

static jstring to_jstring(JNIEnv* env, const tc_string_handle_t* string_ptr) {
    const tc_string_data_t string = tc_read_string(string_ptr);
    const jstring result = to_jstring_from_data(env, string);
    tc_destroy_string(string_ptr);
    return result;
}

JavaVM* jvm;

static void java_callback(uint32_t request_id, tc_string_data_t params_json, uint32_t response_type, bool finished) {
    JNIEnv* env;
    (*jvm)->AttachCurrentThread(jvm, (void**)&env, NULL);
    (*env)->ExceptionClear(env);
    jclass bridgeCls = (*env)->FindClass(env, "tonsdkjni/Bridge");
    jmethodID bridgeNew = (*env)->GetMethodID(env, bridgeCls, "<init>", "()V");
    jobject bridge = (*env)->NewObject(env, bridgeCls, bridgeNew);
    jmethodID get_handler = (*env)->GetMethodID(env, bridgeCls, "handler", "(J)Ltonsdkjni/ResponseHandler;");
    jobject handler = (*env)->CallStaticObjectMethod(env, bridge, get_handler, (jlong)request_id);
    jclass handler_cls = (*env)->GetObjectClass(env, handler);
    jmethodID method = (*env)->GetMethodID(env, handler_cls, "apply", "(JLjava/lang/String;JZ)V");
    jstring data = to_jstring_from_data(env, params_json);
    (*env)->CallVoidMethod(env, handler, method, (jlong)request_id, data, (jlong)response_type, (jboolean)finished);
    if ((*env)->ExceptionOccurred(env)) {
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
    }
    (*jvm)->DetachCurrentThread(jvm);
}

/*
 * Class:     tonsdkjni_Bridge
 * Method:    tcCreateContext
 * Signature: (Ljava/lang/String;)[B
 */
JNIEXPORT jstring JNICALL Java_tonsdkjni_Bridge_tcCreateContext
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
  (JNIEnv * env, jobject jobj, const jlong context, jstring name, jstring params, jlong request) {
    const tc_string_data_t name_data = from_jstring(env, name);
    const tc_string_data_t params_data = from_jstring(env, params);
    (*env)->GetJavaVM(env, &jvm);
    tc_request((uint32_t)context, name_data, params_data, (uint32_t)request, java_callback);
    (*env)->ReleaseStringUTFChars(env, name, name_data.content);
    (*env)->ReleaseStringUTFChars(env, params, params_data.content);
  }

/*
 * Class:     tonsdkjni_Bridge
 * Method:    tcRequestSync
 * Signature: (JLjava/lang/String;Ljava/lang/String;)[B
 */
JNIEXPORT jstring JNICALL Java_tonsdkjni_Bridge_tcRequestSync
  (JNIEnv * env, jobject jobj, jlong context, jstring name, jstring params) {
      const tc_string_data_t name_data = from_jstring(env, name);
      const tc_string_data_t params_data = from_jstring(env, params);
      const tc_string_handle_t* json_ptr = tc_request_sync((uint32_t)context, name_data, params_data);
      (*env)->ReleaseStringUTFChars(env, name, name_data.content);
      (*env)->ReleaseStringUTFChars(env, params, params_data.content);
      return to_jstring(env, json_ptr);
  }



