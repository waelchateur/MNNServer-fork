//
// Created by ruoyi.sdj on 2025/4/18.
//
#pragma once
#include <vector>
#include <string>
#include "nlohmann/json.hpp"
#include "llm/llm.hpp"

using nlohmann::json;
using MNN::Transformer::Embedding;

namespace mls {

class EmbeddingSession {
public:
    EmbeddingSession(std::string, json config, json extra_config);
    void Load();
    ~EmbeddingSession();
    void SetMaxNewTokens(int i);
    MNN::Express::VARP embedding(const std::string& text_cstr);
    std::vector<int> encode(const std::string& query);

private:
    std::string response_string_for_debug{};
    std::string model_path_;
    json extra_config_{};
    json config_{};
    std::vector<float> waveform{};
    Embedding* embedding_{nullptr};
    std::string prompt_string_for_debug{};
    int max_new_tokens_{2048};
    std::string system_prompt_;
    json current_config_{};
};
}

