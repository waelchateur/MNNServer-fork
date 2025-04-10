#include <android/asset_manager_jni.h>
#include <android/log.h>
#include <jni.h>
#include <string>
#include <utility>
#include <vector>
#include <mutex>
#include <ostream>
#include <sstream>
#include "llm/llm.hpp"
#include "mls_log.h"
#include "MNN/expr/ExecutorScope.hpp"

using MNN::Transformer::Embedding;

extern "C" {

JNIEXPORT jlong JNICALL Java_io_kindbrave_mnnserver_session_MNN_initEmbeddingNative(JNIEnv* env, jobject thiz,
                                                                                    jstring rootCacheDir,
                                                                                    jstring modelDir,
                                                                                    jboolean backend) {
    const char* root_cache_dir = env->GetStringUTFChars(rootCacheDir, 0);
    const char* model_dir = env->GetStringUTFChars(modelDir, 0);
    auto model_dir_str = std::string(model_dir);
    std::string root_cache_dir_str = std::string(root_cache_dir);
    env->ReleaseStringUTFChars(modelDir, model_dir);
    env->ReleaseStringUTFChars(rootCacheDir, root_cache_dir);
    
    MNN_DEBUG("createEmbedding BeginLoad %s", model_dir_str.c_str());
    
    bool use_mmap = !root_cache_dir_str.empty();
    bool use_opencl = backend;
    
    MNN::BackendConfig backendConfig;
    auto executor = MNN::Express::Executor::newExecutor(MNN_FORWARD_CPU, backendConfig, 1);
    MNN::Express::ExecutorScope s(executor);
    
    auto embedding = Embedding::createEmbedding(model_dir_str, false);
    
    std::string extra_config = use_mmap ? R"({"use_mmap":true)" : R"({"use_mmap":false)";
    if (use_opencl) {
        extra_config += R"(,"backend_type":"opencl")";
    } else {
        extra_config += R"(,"backend_type":"cpu")";
    }
    
    if (use_mmap) {
        std::string temp_dir = root_cache_dir_str + R"(/tmp)";
        extra_config += R"(,"tmp_path":")" + temp_dir + R"(")";
    }
    
    extra_config = extra_config + R"(})";
    MNN_DEBUG("extra_config: %s", extra_config.c_str());
    embedding->set_config(extra_config);
    MNN_DEBUG("dumped config: %s", embedding->dump_config().c_str());
    
    embedding->load();
    MNN_DEBUG("createEmbedding EndLoad %ld ", reinterpret_cast<jlong>(embedding));
    return reinterpret_cast<jlong>(embedding);
}

JNIEXPORT jfloatArray JNICALL Java_io_kindbrave_mnnserver_session_MNN_getTextEmbeddingNative(JNIEnv* env, jobject thiz,
                                                                                                     jlong embeddingPtr,
                                                                                                     jstring inputStr) {
    Embedding* embedding = reinterpret_cast<Embedding*>(embeddingPtr);
    if (!embedding) {
        return nullptr;
    }
    
    const char* input_str = env->GetStringUTFChars(inputStr, nullptr);
    std::string text(input_str);
    env->ReleaseStringUTFChars(inputStr, input_str);
    
    MNN_DEBUG("getTextEmbedding for text: %s", text.c_str());
    
    auto embedding_var = embedding->txt_embedding(text);
    auto embedding_ptr = embedding_var->readMap<float>();
    int dim = embedding->dim();
    
    jfloatArray result = env->NewFloatArray(dim);
    env->SetFloatArrayRegion(result, 0, dim, embedding_ptr);
    
    return result;
}

JNIEXPORT jfloat JNICALL Java_io_kindbrave_mnnserver_session_MNN_computeSimilarityNative(JNIEnv* env, jobject thiz,
                                                                                                 jlong embeddingPtr,
                                                                                                 jstring inputStr1,
                                                                                                 jstring inputStr2) {
    Embedding* embedding = reinterpret_cast<Embedding*>(embeddingPtr);
    if (!embedding) {
        return 0.0f;
    }
    
    const char* input_str1 = env->GetStringUTFChars(inputStr1, nullptr);
    const char* input_str2 = env->GetStringUTFChars(inputStr2, nullptr);
    
    std::string text1(input_str1);
    std::string text2(input_str2);
    
    env->ReleaseStringUTFChars(inputStr1, input_str1);
    env->ReleaseStringUTFChars(inputStr2, input_str2);
    
    MNN_DEBUG("computeSimilarity between: %s and %s", text1.c_str(), text2.c_str());
    
    auto embedding_var1 = embedding->txt_embedding(text1);
    auto embedding_var2 = embedding->txt_embedding(text2);
    
    float similarity = Embedding::dist(embedding_var1, embedding_var2);
    
    return similarity;
}

JNIEXPORT void JNICALL Java_io_kindbrave_mnnserver_session_MNN_releaseEmbeddingNative(JNIEnv* env,
                                                                                     jobject thiz,
                                                                                     jlong embeddingPtr) {
    MNN_DEBUG("Java_com_alibaba_mnnllm_android_EmbeddingSession_releaseNative\n");
    Embedding* embedding = reinterpret_cast<Embedding*>(embeddingPtr);
    delete embedding;
}

} // extern "C"