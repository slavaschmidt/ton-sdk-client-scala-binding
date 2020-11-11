#include <stdint.h>
#include <jni.h>
#include <stdio.h>
#include "tonclient.h"
#include "ton_sdk_client_jni_Binding.h"

typedef void (*j_tc_response_handler_t)(
    JNIEnv *env,
    jobject callbackInterface,
    uint32_t request_id,
    tc_string_data_t params_json,
    uint32_t response_type,
    bool finished);

tc_string_data_t from_jstring(JNIEnv *env, jstring string) {
    const char* chars = (*env)->GetStringUTFChars(env, string, NULL);
    const size_t length = (size_t)(*env)->GetStringUTFLength(env, string);
    const tc_string_data_t data = {chars, length};
    return data;
}

jbyteArray to_byte_array(JNIEnv *env, const tc_string_data_t string) {
    const jbyteArray array = (*env)->NewByteArray(env, string.len);
    (*env)->SetByteArrayRegion(env, array, 0, string.len, (jbyte*)string.content);
    return array;
}

jstring to_jstring_from_data(JNIEnv* env, const tc_string_data_t string) {
    jclass str_class = (*env)->FindClass(env, "Ljava/lang/String;");
    jmethodID ctor_id = (*env)->GetMethodID(env, str_class, "<init>", "([BLjava/lang/String;)V");
    jstring encoding = (*env)->NewStringUTF(env, "utf-8");
    jbyteArray buf = to_byte_array(env, string);
    return (jstring)(*env)->NewObject(env, str_class, ctor_id, buf, encoding);
}

jstring to_jstring(JNIEnv* env, const tc_string_handle_t* string_ptr) {
    const tc_string_data_t string = tc_read_string(string_ptr);
    const jstring result = to_jstring_from_data(env, string);
    tc_destroy_string(string_ptr);
    return result;
}

JavaVM* jvm;
jmethodID handler;
jclass bridge_cls;

void init_handler() {
    JNIEnv* env;
    (*jvm)->AttachCurrentThread(jvm, (void**)&env, NULL);
    (*env)->ExceptionClear(env);
    bridge_cls = (*env)->FindClass(env, "ton/sdk/client/jni/Binding");
    handler = (*env)->GetStaticMethodID(env, bridge_cls, "handle", "(JLjava/lang/String;JZ)V");
    (*jvm)->DetachCurrentThread(jvm);
}

jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    jvm = vm;
    init_handler();
    return JNI_VERSION_1_1;
}

void java_callback(uint32_t request_id, tc_string_data_t params_json, uint32_t response_type, bool finished) {
    JNIEnv* env;
    (*jvm)->AttachCurrentThread(jvm, (void**)&env, NULL);
    (*env)->ExceptionClear(env);
    if (handler == NULL || bridge_cls == NULL) {
        init_handler();
    }
    jstring data = to_jstring_from_data(env, params_json);
    (*env)->CallStaticVoidMethod(env, bridge_cls, handler, (jlong)request_id, data, (jlong)response_type, (jboolean)finished);
    if ((*env)->ExceptionOccurred(env)) {
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
    }
    (*jvm)->DetachCurrentThread(jvm);
}

/*
 * Class:     ton_sdk_client_jni_Binding
 * Method:    tcCreateContext
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_ton_sdk_client_jni_Binding_tcCreateContext
  (JNIEnv *env, jobject jobj, jstring config) {
    const tc_string_data_t config_data = from_jstring(env, config);
    const tc_string_handle_t* json_ptr = tc_create_context(config_data);
    (*env)->ReleaseStringUTFChars(env, config, config_data.content);
    return to_jstring(env, json_ptr);
  }

/*
 * Class:     ton_sdk_client_jni_Binding
 * Method:    tcDestroyContext
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_ton_sdk_client_jni_Binding_tcDestroyContext
  (JNIEnv * env, jobject jobj, jlong context) {
    tc_destroy_context((uint32_t)context);
  }

/*
 * Class:     ton_sdk_client_jni_Binding
 * Method:    tcRequest
 * Signature: (JLjava/lang/String;Ljava/lang/String;J)V
 */
JNIEXPORT void JNICALL Java_ton_sdk_client_jni_Binding_tcRequest
  (JNIEnv * env, jobject jobj, const jlong context, jstring name, jstring params, jlong request) {
    const tc_string_data_t name_data = from_jstring(env, name);
    const tc_string_data_t params_data = from_jstring(env, params);
    tc_request((uint32_t)context, name_data, params_data, (uint32_t)request, java_callback);
    (*env)->ReleaseStringUTFChars(env, name, name_data.content);
    (*env)->ReleaseStringUTFChars(env, params, params_data.content);
  }

/*
 * Class:     ton_sdk_client_jni_Binding
 * Method:    tcRequestSync
 * Signature: (JLjava/lang/String;Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_ton_sdk_client_jni_Binding_tcRequestSync
  (JNIEnv * env, jobject jobj, jlong context, jstring name, jstring params) {
      const tc_string_data_t name_data = from_jstring(env, name);
      const tc_string_data_t params_data = from_jstring(env, params);
      const tc_string_handle_t* json_ptr = tc_request_sync((uint32_t)context, name_data, params_data);
      (*env)->ReleaseStringUTFChars(env, name, name_data.content);
      (*env)->ReleaseStringUTFChars(env, params, params_data.content);
      return to_jstring(env, json_ptr);
  }



