//
// Created by kindbrave on 2025/5/31.
//
#include <android/asset_manager_jni.h>
#include <android/log.h>
#include <jni.h>
#include <string>
#include <memory>
#include <mutex>
#include "mls_log.h"
#include "include/asr/asr.hpp"

using namespace MNN::Transformer;

extern "C" {

JNIEXPORT jlong JNICALL Java_io_kindbrave_mnnserver_engine_MNNAsr_initNative(
        JNIEnv *env,
        jobject thiz,
        jstring configPath) {

    const char* config_path = env->GetStringUTFChars(configPath, nullptr);
    auto config_path_str = std::string(config_path);
    env->ReleaseStringUTFChars(configPath, config_path);

    MNN_DEBUG("createASR BeginLoad %s", config_path_str.c_str());
    auto asr = Asr::createASR(config_path_str);
    MNN_DEBUG("createASR EndLoad %ld ", reinterpret_cast<jlong>(asr));

    asr->load();

    return reinterpret_cast<jlong>(asr);
}

JNIEXPORT void JNICALL
Java_io_kindbrave_mnnserver_engine_MNNAsr_recognizeFromFileStreamNative(
        JNIEnv *env,
        jobject thiz,
        jlong asr_ptr,
        jstring wavFilePath,
        jobject callback) {

    auto *asr = reinterpret_cast<Asr *>(asr_ptr);
    if (!asr) return;

    const char *wav_path = env->GetStringUTFChars(wavFilePath, nullptr);

    // 获取回调类和方法ID
    jclass callbackClass = env->GetObjectClass(callback);
    jmethodID onPartialResult = env->GetMethodID(callbackClass, "onPartialResult", "(Ljava/lang/String;)V");
    jmethodID onFinalResult = env->GetMethodID(callbackClass, "onFinalResult", "(Ljava/lang/String;)V");

    // C++ lambda 回调
    asr->online_recognize_stream(
            wav_path,
            [&](const std::string &partial) {
                jstring jpartial = env->NewStringUTF(partial.c_str());
                env->CallVoidMethod(callback, onPartialResult, jpartial);
                env->DeleteLocalRef(jpartial);
            },
            [&](const std::string &final_result) {
                jstring jfinal = env->NewStringUTF(final_result.c_str());
                env->CallVoidMethod(callback, onFinalResult, jfinal);
                env->DeleteLocalRef(jfinal);
            }
    );

    env->ReleaseStringUTFChars(wavFilePath, wav_path);
}

JNIEXPORT void JNICALL Java_io_kindbrave_mnnserver_engine_MNNAsr_releaseNative(
        JNIEnv* env,
        jobject thiz,
        jlong asr_ptr) {

    MNN_DEBUG("Java_io_kindbrave_mnnserver_engine_MNNAsr_releaseNative");
    auto* asr = reinterpret_cast<Asr*>(asr_ptr);
    delete asr;
}

} // extern "C"