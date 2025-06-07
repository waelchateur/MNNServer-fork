//
// Created by ruoyi.sjd on 2025/4/18.
//

#include "embedding_session.h"
#include <utility>
#include "MNN/MNNForwardType.h"
#include "MNN/expr/ExecutorScope.hpp"
#include "mls_log.h"
#include "mls_config.h"
#include "utf8_stream_processor.hpp"
#include "llm_stream_buffer.hpp"
#include <audio/audio.hpp>

namespace mls {

EmbeddingSession::EmbeddingSession(std::string model_path, json config, json extra_config):
        model_path_(std::move(model_path)), config_(std::move(config)), extra_config_(std::move(extra_config)) {
    max_new_tokens_ = config_.contains("max_new_tokens") ?  config_["max_new_tokens"].get<int>() : 2048;
}

void EmbeddingSession::Load() {
    std::string root_cache_dir_str = extra_config_["mmap_dir"];
    bool use_mmap = !extra_config_["mmap_dir"].get<std::string>().empty();
    MNN::BackendConfig backendConfig;
    auto executor = MNN::Express::Executor::newExecutor(MNN_FORWARD_CPU, backendConfig, 1);
    MNN::Express::ExecutorScope s(executor);
    embedding_ = Embedding::createEmbedding(model_path_);
    json config = config_;
    config["use_mmap"] = use_mmap;
    if (use_mmap) {
        std::string temp_dir = root_cache_dir_str;
        config["tmp_path"] = temp_dir;
    }
    current_config_ = config;
    auto config_str = config.dump();
    MNN_DEBUG("extra_config: %s", config_str.c_str());
    embedding_->set_config(config_str);
    MNN_DEBUG("dumped config: %s", embedding_->dump_config().c_str());
    embedding_->load();
}

EmbeddingSession::~EmbeddingSession() {
    delete embedding_;
}

void EmbeddingSession::SetMaxNewTokens(int i) {
    max_new_tokens_ = i;
}

MNN::Express::VARP EmbeddingSession::embedding(const std::string& text_cstr) {
    if (embedding_) {
        return embedding_->txt_embedding(text_cstr);
    }
    return nullptr;
}

std::vector<int> EmbeddingSession::encode(const std::string& query) {
    if (embedding_) {
        return embedding_->tokenizer_encode(query);
    }
    return {};
}

}