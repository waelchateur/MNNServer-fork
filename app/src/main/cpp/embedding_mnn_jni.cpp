#include <android/asset_manager_jni.h>
#include <android/bitmap.h>
#include <android/log.h>
#include <jni.h>
#include <string>
#include <utility>
#include <vector>
#include <thread>
#include <mutex>
#include <ostream>
#include <sstream>
#include <mutex>
#include <ostream>
#include <sstream>
#include <mutex>
#include <string>
#include <chrono>
#include "mls_log.h"
#include "MNN/expr/ExecutorScope.hpp"
#include "nlohmann/json.hpp"
#include "utf8_stream_processor.hpp"
#include "embedding_session.h"

using MNN::Transformer::Embedding;
using json = nlohmann::json;

extern "C" {

JNIEXPORT jlong JNICALL Java_io_kindbrave_mnnserver_engine_MNNEmbedding_initNative(JNIEnv *env,
                                                                                        jobject thiz,
                                                                                        jstring modelDir,
                                                                                        jstring mergeConfigStr,
                                                                                        jstring configJsonStr) {
    const char* model_dir = env->GetStringUTFChars(modelDir, nullptr);
    auto model_dir_str = std::string(model_dir);
    const char* config_json_cstr = env->GetStringUTFChars(configJsonStr, nullptr);
    const char* merged_config_cstr = env->GetStringUTFChars(mergeConfigStr, nullptr);
    json merged_config  = json::parse(merged_config_cstr);
    json extra_json_config = json::parse(config_json_cstr);
    env->ReleaseStringUTFChars(modelDir, model_dir);
    env->ReleaseStringUTFChars(configJsonStr, config_json_cstr);
    env->ReleaseStringUTFChars(mergeConfigStr, merged_config_cstr);
    MNN_DEBUG("createEmbedding BeginLoad %s", model_dir);
    auto embedding_session = new mls::EmbeddingSession(model_dir_str, merged_config, extra_json_config);
    embedding_session->Load();
    MNN_DEBUG("createEmbedding EndLoad %ld ", reinterpret_cast<jlong>(embedding_session));
    return reinterpret_cast<jlong>(embedding_session);
}

extern "C"
JNIEXPORT void JNICALL
Java_io_kindbrave_mnnserver_engine_MNNEmbedding_updateMaxNewTokensNative(JNIEnv *env, jobject thiz,
                                                                           jlong llm_ptr,
                                                                           jint max_new_tokens) {
    auto *embedding = reinterpret_cast<mls::EmbeddingSession *>(llm_ptr);
    if (embedding) {
        embedding->SetMaxNewTokens(max_new_tokens);
    }

}

extern "C"
JNIEXPORT jfloatArray JNICALL
Java_io_kindbrave_mnnserver_engine_MNNEmbedding_embedding(JNIEnv *env, jobject thiz,
                                                              jlong llm_ptr,
                                                              jstring text) {
    auto *embedding = reinterpret_cast<mls::EmbeddingSession *>(llm_ptr);
    const char *text_cstr = env->GetStringUTFChars(text, nullptr);
    jfloatArray result = nullptr;
    
    if (embedding) {
        MNN::Express::VARP vec_0 = embedding->embedding(text_cstr);
        auto size = vec_0->getInfo()->size;
        auto ptr = vec_0->readMap<float>();
        
        result = env->NewFloatArray(size);
        env->SetFloatArrayRegion(result, 0, size, ptr);
    }
    
    env->ReleaseStringUTFChars(text, text_cstr);
    return result;
}

JNIEXPORT void JNICALL Java_io_kindbrave_mnnserver_engine_MNNEmbedding_releaseNative(JNIEnv* env,
                                                                               jobject thiz,
                                                                               jlong objecPtr) {
    MNN_DEBUG("Java_com_alibaba_mnnllm_android_ChatSession_releaseNative\n");
    auto* embedding = reinterpret_cast<mls::EmbeddingSession*>(objecPtr);
    delete embedding;
}
}