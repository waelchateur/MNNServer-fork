//
//  asr.hpp
//
//  Created by MNN on 2024/10/31.
//  ZhaodeWang
//

#ifndef ASR_hpp
#define ASR_hpp

#include <vector>
#include <memory>
#include <string>
#include <fstream>
#include <sstream>
#include <iostream>
#include <streambuf>
#include <functional>
#include <unordered_map>

#include <MNN/expr/Expr.hpp>
#include <MNN/expr/Module.hpp>
#include <MNN/expr/MathOp.hpp>
#include <MNN/expr/NeuralNetWorkOp.hpp>

namespace MNN {
    namespace Transformer {
        class AsrConfig;
        class Tokenizer;
        class WavFrontend;
        class OnlineCache;

        class MNN_PUBLIC Asr {
        public:
            static Asr* createASR(const std::string& config_path);
            Asr(std::shared_ptr<AsrConfig> config) : config_(config), cache_(nullptr) {}
            virtual ~Asr();
            void load();
            std::string recognize(Express::VARP speech);
            void online_recognize(const std::string& wav_file);
            void online_recognize_stream(
                    const std::string &wav_file,
                    std::function<void(const std::string &)> on_partial,
                    std::function<void(const std::string &)> on_final);
            void offline_recognize(const std::string& wav_file);
        private:
            void init_cache(int batch_size = 1);
            Express::VARP add_overlap_chunk(Express::VARP feats);
            Express::VARP position_encoding(Express::VARP sample);
            Express::VARPS cif_search(Express::VARP enc, Express::VARP alpha);
            std::string decode(Express::VARP logits);
            std::string infer(Express::VARP feats);
        private:
            std::shared_ptr<AsrConfig> config_;
            std::shared_ptr<Tokenizer> tokenizer_;
            std::shared_ptr<WavFrontend> frontend_;
            std::shared_ptr<MNN::Express::Executor::RuntimeManager> runtime_manager_;
            std::vector<std::shared_ptr<MNN::Express::Module>> modules_;
            std::shared_ptr<OnlineCache> cache_;
            int feats_dims_;
            std::vector<int> chunk_size_;
        };

    }
}

#endif // ASR_hpp
