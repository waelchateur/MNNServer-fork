//
//  asrconfig.hpp
//
//  Created by MNN on 2024/11/08.
//  ZhaodeWang
//

#include "rapidjson/document.h"
#include <rapidjson/writer.h>
#include <rapidjson/stringbuffer.h>

namespace MNN {
    namespace Transformer {

        static inline bool has_suffix(const std::string& str, const std::string& suffix) {
            return str.size() >= suffix.size() &&
                   str.compare(str.size() - suffix.size(), suffix.size(), suffix) == 0;
        }

        static inline std::string base_dir(const std::string& path) {
            size_t pos = path.find_last_of("/\\");
            if (pos == std::string::npos) {
                return "./";
            } else {
                return path.substr(0, pos + 1);
            }
        }

        static inline std::string file_name(const std::string& path) {
            size_t pos = path.find_last_of("/\\");
            if (pos == std::string::npos) {
                return path;
            } else {
                return path.substr(pos + 1);
            }
        }

        bool merge_json(rapidjson::Value& destination, const rapidjson::Value& source,
                        rapidjson::Document::AllocatorType& allocator) {
            if (!source.IsObject() || !destination.IsObject()) {
                return false;
            }

            for (auto it = source.MemberBegin(); it != source.MemberEnd(); ++it) {
                const char* key = it->name.GetString();
                if (destination.HasMember(key)) {
                    if (destination[key].IsObject() && it->value.IsObject()) {
                        // Recursively merge the two JSON objects
                        merge_json(destination[key], it->value, allocator);
                    } else {
                        // Overwrite the value in the destination
                        destination[key].CopyFrom(it->value, allocator);
                    }
                } else {
                    // Add the value to the destination
                    rapidjson::Value newKey(key, allocator);
                    rapidjson::Value newValue;
                    newValue.CopyFrom(it->value, allocator);
                    destination.AddMember(newKey, newValue, allocator);
                }
            }
            return true;
        }

        class rapid_json_wrapper {
        public:
            rapidjson::Document document;
            rapid_json_wrapper() {}
            rapid_json_wrapper(rapidjson::Document doc) : document(std::move(doc)) {}
            static rapid_json_wrapper parse(const std::ifstream& ifile) {
                std::ostringstream ostr;
                ostr << ifile.rdbuf();
                rapidjson::Document document;
                document.Parse(ostr.str().c_str());
                rapid_json_wrapper json_wrapper(std::move(document));
                return json_wrapper;
            }
            static rapid_json_wrapper parse(const char* str) {
                rapidjson::Document document;
                document.Parse(str);
                rapid_json_wrapper json_wrapper(std::move(document));
                return json_wrapper;
            }
            bool merge(const char* str) {
                rapidjson::Document input_doc;
                input_doc.Parse(str);
                if (input_doc.HasParseError()) {
                    return false;
                }
                // merge
                rapidjson::Document::AllocatorType& allocator = document.GetAllocator();
                return merge_json(document, input_doc, allocator);
            }
            std::string dump() {
                rapidjson::StringBuffer buffer;
                rapidjson::Writer<rapidjson::StringBuffer> writer(buffer);
                document.Accept(writer);
                return buffer.GetString();
            }
            // read value
            int value(const char* key, const int& default_value) const {
                if (document.HasMember(key)) {
                    const auto& value = document[key];
                    if (value.IsInt()) return value.GetInt();
                }
                return default_value;
            }
            float value(const char* key, const float& default_value) const {
                if (document.HasMember(key)) {
                    const auto& value = document[key];
                    if (value.IsFloat()) return value.GetFloat();
                }
                return default_value;
            }
            bool value(const char* key, const bool& default_value) const {
                if (document.HasMember(key)) {
                    const auto& value = document[key];
                    if (value.IsBool()) return value.GetBool();
                }
                return default_value;
            }
            std::string value(const char* key, const std::string& default_value) const {
                if (document.HasMember(key)) {
                    const auto& value = document[key];
                    if (value.IsString()) return value.GetString();
                }
                return default_value;
            }
            std::vector<int> value(const char* key, const std::vector<int>& default_value) const {
                if (document.HasMember(key)) {
                    const auto& value = document[key];
                    if (value.IsArray()) {
                        std::vector<int> result;
                        for (auto& v : value.GetArray()) {
                            if (v.IsInt()) {
                                result.push_back(v.GetInt());
                            }
                        }
                        return result;
                    }
                }
                return default_value;
            }
            std::vector<float> value(const char* key, const std::vector<float>& default_value) const {
                if (document.HasMember(key)) {
                    const auto& value = document[key];
                    if (value.IsArray()) {
                        std::vector<float> result;
                        for (auto& v : value.GetArray()) {
                            if (v.IsFloat()) {
                                result.push_back(v.GetFloat());
                            }
                        }
                        return result;
                    }
                }
                return default_value;
            }
            std::string value(const char key[], const char default_value[]) const {
                return value(key, std::string(default_value));
            }
        };

        class AsrConfig {
        public:
            std::string base_dir_;
            rapid_json_wrapper config_, asr_config_;
            AsrConfig() {}
            AsrConfig(const std::string& path) {
                // load config
                if (has_suffix(path, ".json")) {
                    std::ifstream config_file(path);
                    if (config_file.is_open()) {
                        config_ = rapid_json_wrapper::parse(config_file);
                    } else {
                        std::cerr << "Unable to open config file: " << path << std::endl;
                    }
                    base_dir_ = base_dir(path);
                }
                // using config's base_dir
                base_dir_ = config_.value("base_dir", base_dir_);
                // load llm_config for model info
                std::ifstream asr_config_file(asr_config());
                if (asr_config_file.is_open()) {
                    asr_config_ = rapid_json_wrapper::parse(asr_config_file);
                } else {
                    std::cerr << "Unable to open asr_config file: " << asr_config() << std::endl;
                }
            }

            // < model file config start
            std::string asr_config() const {
                return base_dir_ + config_.value("asr_config", "asr_config.json");
            }

            std::string encoder_model() const {
                return base_dir_ + config_.value("encoder_model", "encoder.mnn");
            }

            std::string decoder_model() const {
                return base_dir_ + config_.value("decoder_model", "decoder.mnn");
            }

            std::string block_model(int index) const {
                return base_dir_ + config_.value("block_model", "block_") + std::to_string(index) + ".mnn";
            }

            std::string tokenizer_file() const {
                return base_dir_ + config_.value("tokenizer_file", "tokenizer.txt");
            }
            // model file config end >

            // < backend config start
            std::string backend_type() const {
                return config_.value("backend_type", "cpu");
            }

            int thread_num() const {
                return config_.value("thread_num", 4);
            }

            std::string precision() const {
                return config_.value("precision", "low");
            }
            std::string power() const {
                return config_.value("power", "normal");
            }

            std::string memory() const {
                return config_.value("memory", "low");
            }
            // backend config end >

            // < asr model config start
            int encoder_output_size() const {
                return asr_config_.value("encoder_output_size", 512);
            }

            int fsmn_layer() const {
                return asr_config_.value("fsmn_layer", 16);
            }

            int fsmn_lorder() const {
                return asr_config_.value("fsmn_lorder", 10);
            }

            int fsmn_dims() const {
                return asr_config_.value("fsmn_dims", 512);
            }

            int feats_dims() const {
                return asr_config_.value("feats_dims", 560);
            }

            float cif_threshold() const {
                return asr_config_.value("cif_threshold", (float)1.0);
            }

            float tail_threshold() const {
                return asr_config_.value("tail_threshold", (float)0.45);
            }

            int samp_freq() const {
                return asr_config_.value("samp_freq", 16000);
            }

            std::string window_type() const {
                return asr_config_.value("window_type", "hamming");
            }

            int frame_shift_ms() const {
                return asr_config_.value("frame_shift_ms", 10);
            }

            int frame_length_ms() const {
                return asr_config_.value("frame_length_ms", 25);
            }

            int num_bins() const {
                return asr_config_.value("num_bins", 80);
            }

            float dither() const {
                return asr_config_.value("dither", 0);
            }

            int lfr_m() const {
                return asr_config_.value("lfr_m", 7);
            }

            int lfr_n() const {
                return asr_config_.value("lfr_n", 6);
            }

            std::vector<int> chunk_size() const {
                return asr_config_.value("chunk_size", std::vector<int>{});
            }

            std::vector<float> mean() const {
                return asr_config_.value("mean", std::vector<float>{});
            }

            std::vector<float> var() const {
                return asr_config_.value("var", std::vector<float>{});
            }
            // asr model config end >
        };
    } // Transformer
} // MNN
