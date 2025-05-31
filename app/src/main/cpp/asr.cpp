//
//  asr.cpp
//
//  Created by MNN on 2024/10/31.
//  ZhaodeWang
//

#include "include/asr/asr.hpp"
#include "include/asr/asrconfig.hpp"
#include "include/asr/tokenizer.hpp"

#include <audio/audio.hpp>

#include <cmath>
#include <complex>
#include <random>

using namespace MNN::Express;
namespace MNN {
    namespace Transformer {

#define DIV_UP(a, b) (((a) + (b) - 1) / (b))

        static void dump_impl(const float* signal, size_t size, int row = 0) {
            if (row) {
                constexpr int lines = 3;
                int col = size / row;
                printf("# %d, %d: [\n", row, col);
                for (int i = 0; i < lines; i++) {
                    for (int j = 0; j < 3; j++) {
                        printf("%f, ", signal[i * col + j]);
                    }
                    printf("..., ");
                    for (int j = col - 3; j < col; j++) {
                        printf("%f, ", signal[i * col + j]);
                    }
                    printf("\n");
                }
                printf("..., \n");
                for (int i = row - lines; i < row; i++) {
                    for (int j = 0; j < 3; j++) {
                        printf("%f, ", signal[i * col + j]);
                    }
                    printf("..., ");
                    for (int j = col - 3; j < col; j++) {
                        printf("%f, ", signal[i * col + j]);
                    }
                    printf("\n");
                }
                printf("]\n");
            } else {
                printf("# %lu: [", size);
                for (int i = 0; i < 3; i++) {
                    printf("%f, ", signal[i]);
                }
                printf("..., ");
                for (int i = size - 3; i < size; i++) {
                    printf("%f, ", signal[i]);
                }
                printf("]\n");
            }
        }

        static void dump(const std::vector<float> &signal, int row = 0) { dump_impl(signal.data(), signal.size(), row); }

        void dump_var(VARP var) {
            auto dims = var->getInfo()->dim;
            bool isfloat = true;
            printf("{\ndtype = ");
            if (var->getInfo()->type == halide_type_of<float>()) {
                printf("float");
                isfloat = true;
            } else if (var->getInfo()->type == halide_type_of<int>()) {
                printf("int");
                isfloat = false;
            }
            printf("\nformat = %d\n", var->getInfo()->order);
            printf("\ndims = [");
            for (int i = 0; i < dims.size(); i++) {
                printf("%d ", dims[i]);
            }
            printf("]\n");

            if (isfloat) {
                if ((dims.size() > 2 && dims[1] > 1 && dims[2] > 1) || (dims.size() == 2 && dims[0] > 1 && dims[1] > 1)) {
                    int row = dims[dims.size() - 2];
                    if (dims.size() > 2 && dims[0] > 1) {
                        row *= dims[0];
                    }
                    dump_impl(var->readMap<float>(), var->getInfo()->size, row);
                } else {
                    printf("data = [");
                    auto total = var->getInfo()->size;
                    if (total > 32) {
                        for (int i = 0; i < 5; i++) {
                            printf("%f ", var->readMap<float>()[i]);
                        }
                        printf("..., ");
                        for (int i = total - 5; i < total; i++) {
                            printf("%f ", var->readMap<float>()[i]);
                        }
                    } else {
                        for (int i = 0; i < total; i++) {
                            printf("%f ", var->readMap<float>()[i]);
                        }
                    }
                    printf("]\n}\n");
                }
            } else {
                printf("data = [");
                int size = var->getInfo()->size > 10 ? 10 : var->getInfo()->size;
                for (int i = 0; i < size; i++) {
                    printf("%d ", var->readMap<int>()[i]);
                }
                printf("]\n}\n");
            }
        }

        struct OnlineCache {
            int start_idx = 0;
            bool is_final = false;
            bool last_chunk = false;
            std::vector<int> chunk_size;
            VARP cif_hidden;
            VARP cif_alphas;
            VARP feats;
            std::vector<VARP> decoder_fsmn;
            std::vector<int> tokens;
        };

        class WavFrontend {
        public:
            WavFrontend(std::shared_ptr<AsrConfig> config) : config_(config) {
                mean_ = config->mean();
                var_ = config->var();
            }
            ~WavFrontend() = default;
            VARP apply_lfr(VARP samples);
            VARP apply_cmvn(VARP samples);
            VARP extract_feat(VARP samples);
        private:
            std::shared_ptr<AsrConfig> config_;
            std::vector<float> mean_;
            std::vector<float> var_;
            float dither_ = 1.0;
            int frame_length_ms_ = 25;
            int frame_shift_ms_ = 10;
            int sampling_rate = 16000;
            float preemphasis_coefficient = 0.97;
            int num_bins_ = 80;
            int lfr_m_ = 7;
            int lfr_n_ = 6;
            int feats_dims_ = 560;
        };

        VARP WavFrontend::apply_cmvn(VARP samples) {
            auto mean = _Const(mean_.data(), {static_cast<int>(mean_.size())});
            auto var  = _Const(var_.data(), {static_cast<int>(mean_.size())});
            samples = (samples + mean) * var;
            return samples;
        }

        VARP WavFrontend::apply_lfr(VARP samples) {
            auto dim = samples->getInfo()->dim;
            int row = dim[0];
            int padding_len = (lfr_m_ - 1) / 2;
            int t_lfr = DIV_UP(row, lfr_n_);
            std::vector<int> lfr_regions = {
                    // region 0
                    0,                               // src offset
                    1, 0, 1,                         // src strides
                    0,                               // dst offset
                    1, num_bins_, 1,                 // dst strides
                    1, padding_len, num_bins_,       // dst sizes
                    // region 1
                    0,                                  // src offset
                    1, num_bins_, 1,                    // src strides
                    padding_len * num_bins_,            // dst offset
                    1, num_bins_, 1,                    // dst strides
                    1, lfr_m_ - padding_len, num_bins_, // dst sizes
                    // region 2
                    (lfr_n_ - padding_len) * num_bins_, // src offset
                    lfr_n_ * num_bins_, num_bins_, 1,   // src strides
                    lfr_m_ * num_bins_,                 // dst offset
                    lfr_m_ * num_bins_, num_bins_, 1,   // dst strides
                    t_lfr, lfr_m_, num_bins_            // dst sizes
            };
            samples = _Raster({samples, samples, samples}, lfr_regions, {1, t_lfr, lfr_m_ * num_bins_});
            return samples;
        }

        VARP WavFrontend::extract_feat(VARP waveforms) {
            waveforms = waveforms * _Scalar<float>(32768);
            auto feature = AUDIO::fbank(waveforms);
            feature = apply_lfr(feature);
            feature = apply_cmvn(feature);
            return feature;
        }

        VARP Asr::position_encoding(VARP samples) {
            auto ptr = (float*)samples->readMap<float>();
            auto dims = samples->getInfo()->dim;
            int length = dims[1];
            int feat_dims = dims[2];
            constexpr float neglog_timescale = -0.03301197265941284;
            for (int i = 0; i < length; i++) {
                int offset = i + 1 + cache_->start_idx;
                for (int j = 0; j < feat_dims / 2; j++) {
                    float inv_timescale = offset * std::exp(j * neglog_timescale);
                    ptr[i * feat_dims + j]                 += std::sin(inv_timescale);
                    ptr[i * feat_dims + j + feat_dims / 2] += std::cos(inv_timescale);
                }
            }
            cache_->start_idx += length;
            return samples;
        }

        template <typename T>
        static inline VARP _var(std::vector<T> vec, const std::vector<int>& dims) {
            return _Const(vec.data(), dims, NHWC, halide_type_of<T>());
        }
        static inline VARP _zeros(const std::vector<int>& dims) {
            std::vector<float> data(std::accumulate(dims.begin(), dims.end(), 1, std::multiplies<int>()), 0);
            return _Const(data.data(), dims, NCHW, halide_type_of<float>());
        }

        void Asr::init_cache(int batch_size) {
            cache_.reset(new OnlineCache);
            cache_->start_idx = 0;
            cache_->is_final = false;
            cache_->last_chunk = false;
            cache_->chunk_size = chunk_size_;
            cache_->cif_hidden = _zeros({batch_size, 1, config_->encoder_output_size()});
            cache_->cif_alphas = _zeros({batch_size, 1});
            cache_->feats = _zeros({batch_size, chunk_size_[0] + chunk_size_[2], feats_dims_});
            for (int i = 0; i < config_->fsmn_layer(); i++) {
                cache_->decoder_fsmn.emplace_back(_zeros({batch_size, config_->fsmn_dims(), config_->fsmn_lorder()}));
            }
        }

        VARP Asr::add_overlap_chunk(VARP feats) {
            if (!cache_) return feats;
            feats = _Concat({cache_->feats, feats}, 1);
            if (cache_->is_final) {
                cache_->feats = _Slice(feats, _var<int>({0, -chunk_size_[0], 0}, {3}), _var<int>({-1, -1, -1}, {3}));
                if (!cache_->last_chunk) {
                    int padding_length = std::accumulate(chunk_size_.begin(), chunk_size_.end(), 0) - feats->getInfo()->dim[1];
                    feats = _Pad(feats, _var<int>({0, 0, 0, padding_length, 0, 0}, {3, 2}));
                }
            } else {
                cache_->feats = _Slice(feats, _var<int>({0, -(chunk_size_[0] + chunk_size_[2]), 0}, {3}), _var<int>({-1, -1, -1}, {3}));
            }
            return feats;
        }

        VARPS Asr::cif_search(VARP hidden, VARP alphas) {
            auto chunk_alpha_ptr = const_cast<float*>(alphas->readMap<float>());
            for (int i = 0; i < alphas->getInfo()->size; i++) {
                if (i < chunk_size_[0] || i >= chunk_size_[0] + chunk_size_[1]) {
                    chunk_alpha_ptr[i] = 0.f;
                }
            }
            if (cache_->last_chunk) {
                int hidden_size = hidden->getInfo()->dim[2];
                auto tail_hidden = _zeros({1, 1, hidden_size});
                auto tail_alphas = _var<float>({config_->tail_threshold()}, {1, 1});
                hidden = _Concat({cache_->cif_hidden, hidden, tail_hidden}, 1);
                alphas = _Concat({cache_->cif_alphas, alphas, tail_alphas}, 1);
            } else {
                hidden = _Concat({cache_->cif_hidden, hidden}, 1);
                alphas = _Concat({cache_->cif_alphas, alphas}, 1);
            }
            auto alpha_ptr = alphas->readMap<float>();

            auto dims = hidden->getInfo()->dim;
            int batch_size = dims[0], len_time = dims[1], hidden_size = dims[2];
            auto frames = _zeros({hidden_size});
            float cif_threshold = config_->cif_threshold();
            float integrate = 0.f;
            std::vector<VARP> list_frame;
            for (int t = 0; t < len_time; t++) {
                float alpha = alpha_ptr[t];
                auto hidden_t = _GatherV2(hidden, _var<int>({t}, {1}), _Scalar<int>(1));
                if (alpha + integrate < cif_threshold) {
                    integrate += alpha;
                    frames = frames + _Scalar<float>(alpha) * hidden_t;
                } else {
                    frames = frames + _Scalar<float>(cif_threshold - integrate) * hidden_t;
                    list_frame.push_back(frames);
                    integrate += alpha;
                    integrate -= cif_threshold;
                    frames = _Scalar<float>(integrate) * hidden_t;
                }
            }
            // update cache
            cache_->cif_alphas = _var<float>({integrate}, {1, 1});
            cache_->cif_hidden = integrate > 0.f ? (frames / _Scalar<float>(integrate)) : frames;
            return list_frame;
        }



        std::string Asr::decode(MNN::Express::VARP logits) {
            int token_num = logits->getInfo()->dim[1];
            auto token_ptr = _ArgMax(logits, -1)->readMap<int>();
            std::string text;
            for (int i = 0; i < token_num; i++) {
                int token = token_ptr[i];
                if (tokenizer_->is_special(token)) {
                    continue;
                }
                cache_->tokens.push_back(token);
                auto symbol = tokenizer_->decode(token);
                // end with '@@'
                if (symbol.size() > 2 && symbol.back() == '@' && symbol[symbol.size() - 2] == '@') {
                    symbol = std::string(symbol.data(), symbol.size() - 2);
                } else {
                    if (reinterpret_cast<const uint8_t *>(symbol.c_str())[0] < 0x80) {
                        symbol.append(" ");
                    }
                }
                text.append(symbol);
            }
            return text;
        }

        std::string Asr::infer(VARP feats) {
            auto enc_len = _Input({1}, NCHW, halide_type_of<int>());
            enc_len->writeMap<int>()[0] = feats->getInfo()->dim[1];
            auto encoder_outputs = modules_[0]->onForward({feats, enc_len});
            auto alphas = encoder_outputs[0];
            auto enc = encoder_outputs[1];
            enc_len = encoder_outputs[2];
            auto acoustic_embeds_list = cif_search(enc, alphas);
            if (acoustic_embeds_list.empty()) {
                return "";
            }
            auto acoustic_embeds = _Concat(acoustic_embeds_list, 1);
            int acoustic_embeds_len = static_cast<int>(acoustic_embeds_list.size());
            VARPS decocder_inputs {enc, enc_len, acoustic_embeds, _var<int>({acoustic_embeds_len}, {1})};
            for (auto fsmn : cache_->decoder_fsmn) {
                decocder_inputs.push_back(fsmn);
            }
            auto decoder_outputs = modules_[1]->onForward(decocder_inputs);

            auto logits = decoder_outputs[0];
            for (int i = 0; i < config_->fsmn_layer(); i++) {
                cache_->decoder_fsmn[i] = decoder_outputs[2 + i];
            }
            auto text = decode(logits);
            // printf("%s", text.c_str());
            return text;
        }

// std::string Asr::recognize(std::vector<float>& waveforms) {
        std::string Asr::recognize(VARP waveforms) {
            size_t wave_length = waveforms->getInfo()->size;
            if (wave_length < 16 * 60 && cache_->is_final) {
                cache_->last_chunk = true;
                return infer(cache_->feats);
            }
            // auto t1 = std::chrono::system_clock::now();
            auto feats = frontend_->extract_feat(waveforms);
            // std::cout << "feats time: " << std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now() - t1).count() << std::endl;

            feats = feats * _Scalar<float>(std::sqrt(config_->encoder_output_size()));
            feats = position_encoding(feats);
            if (cache_->is_final) {
                auto dims = feats->getInfo()->dim;
                if (dims[1] + chunk_size_[2] <= chunk_size_[1]) {
                    cache_->last_chunk = true;
                    feats = add_overlap_chunk(feats);
                } else {
                    // first chunk
                    auto feats1 = feats;
                    if (dims[1] > chunk_size_[1]) {
                        feats1 = _Slice(feats, _var<int>({0, 0, 0}, {3}), _var<int>({-1, chunk_size_[1], -1}, {3}));
                    }
                    auto feats_chunk1 = add_overlap_chunk(feats1);
                    auto res1 = infer(feats_chunk1);
                    // last chunk
                    cache_->last_chunk = true;
                    auto feat2 = feats;
                    int start = dims[1] + chunk_size_[2] - chunk_size_[1];
                    if (start != 0) {
                        feat2 = _Slice(feats, _var<int>({0, -start, 0}, {3}), _var<int>({-1, -1, -1}, {3}));
                    }
                    auto feats_chunk2 = add_overlap_chunk(feat2);
                    auto res2 = infer(feats_chunk2);
                    return res1 + res2;
                }
            } else {
                feats = add_overlap_chunk(feats);
            }
            return infer(feats);
        }

        void Asr::online_recognize(const std::string &wav_file) {
            std::cout << "### wav file: " << wav_file << std::endl;
            bool is_ok = false;
#if 0
            std::vector<float> speech = read_wave(wav_file, &sample_rate, &is_ok);
    if (!is_ok) {
        fprintf(stderr, "Failed to read '%s'\n", wav_file.c_str());
        return;
    }
    float mean_val = std::accumulate(speech.begin(), speech.end(), 0.f) / speech.size();
    std::cout << "### wav file: " << wav_file << ", mean_val: " << mean_val << std::endl;
    auto speech_length = speech.size();
#else
            auto audio_file = AUDIO::load(wav_file);
            auto speech = audio_file.first;
            int sample_rate = audio_file.second;
            auto speech_length = speech->getInfo()->size;
            auto speech_ptr = speech->readMap<int>();
            int start = 0;
            int end = speech_length - 1;

            while(start < speech_length)
            {
                if (speech_ptr[start] != 0) {
                    break;
                }
                start++;
            }

            while(end >= 0)
            {
                if (speech_ptr[end] != 0) {
                    break;
                }
                end--;
            }
            speech_length = end - start + 1;
#endif
            int chunk_size = chunk_size_[1] * 960;
            int steps = DIV_UP(speech_length, chunk_size);
            init_cache();
            std::string total = "";
            for (int i = 0; i < steps; i++) {
                int deal_size = chunk_size;
                if (i == steps - 1) {
                    cache_->is_final = true;
                    deal_size = speech_length - i * chunk_size;
                }
                // std::vector<float> chunk(speech.begin() + i * chunk_size, speech.begin() + i * chunk_size + deal_size);
                auto chunk = _Slice(speech, _var<int>({i * chunk_size + start}, {1}), _var<int>({deal_size}, {1}));
                auto res = recognize(chunk);
                std::cout << "preds: " << res << std::endl;
                total += res;
                // std::cout << res;
            }
            std::cout << std::endl;
            std::cout << total << std::endl;
        }

        void Asr::online_recognize_stream(
                const std::string &wav_file,
                std::function<void(const std::string &)> on_partial,
                std::function<void(const std::string &)> on_final) {

            auto audio_file = AUDIO::load(wav_file);
            auto speech = audio_file.first;
            int sample_rate = audio_file.second;
            auto speech_length = speech->getInfo()->size;
            auto speech_ptr = speech->readMap<int>();

            int start = 0;
            int end = speech_length - 1;
            while (start < speech_length && speech_ptr[start] == 0) start++;
            while (end >= 0 && speech_ptr[end] == 0) end--;
            speech_length = end - start + 1;

            int chunk_size = chunk_size_[1] * 960;
            int steps = DIV_UP(speech_length, chunk_size);
            init_cache();

            std::string total = "";
            for (int i = 0; i < steps; i++) {
                int deal_size = chunk_size;
                if (i == steps - 1) {
                    cache_->is_final = true;
                    deal_size = speech_length - i * chunk_size;
                }

                auto chunk = _Slice(speech, _var<int>({i * chunk_size + start}, {1}), _var<int>({deal_size}, {1}));
                auto res = recognize(chunk);

                total += res;

                if (on_partial) {
                    on_partial(res);  // 每步调用 partial
                }
            }

            if (on_final) {
                on_final(total);
            }
        }

        Asr *Asr::createASR(const std::string &config_path) {
            std::shared_ptr<AsrConfig> config(new AsrConfig(config_path));
            return new Asr(config);
        }

        Asr::~Asr() {}

        void Asr::load() {
            feats_dims_ = config_->feats_dims();
            chunk_size_ = config_->chunk_size();
            frontend_.reset(new WavFrontend(config_));
            tokenizer_.reset(Tokenizer::createTokenizer(config_->tokenizer_file()));
            {
                ScheduleConfig config;
                BackendConfig cpuBackendConfig;
                config.type          = MNN_FORWARD_CPU;
                config.numThread     = 4;
                cpuBackendConfig.power = BackendConfig::Power_Low;
                // cpuBackendConfig.memory = BackendConfig::Memory_Low;
                // cpuBackendConfig.precision = BackendConfig::Precision_Low;
                config.backendConfig = &cpuBackendConfig;
                // ExecutorScope::Current()->setGlobalExecutorConfig(config.type, cpuBackendConfig, config.numThread);

                runtime_manager_.reset(Executor::RuntimeManager::createRuntimeManager(config));
                runtime_manager_->setHint(MNN::Interpreter::MEM_ALLOCATOR_TYPE, 0);
                runtime_manager_->setHint(MNN::Interpreter::DYNAMIC_QUANT_OPTIONS, 1); // 1: per batch quant, 2: per tensor quant
            }
            modules_.resize(2);
            Module::Config module_config;
            module_config.shapeMutable = true;
            module_config.rearrange = true;
            std::string decoder_path = "encoder.mnn";
            std::vector<std::string> encoder_inputs {"speech", "enc_len"};
            std::vector<std::string> encoder_outputs {"alphas", "enc", "enc_len"};
            std::vector<std::string> decoder_inputs {"enc", "enc_len", "acoustic_embeds", "acoustic_embeds_len"};
            std::vector<std::string> decoder_outputs {"logits", "sample_ids"};
            for (int i = 0; i < config_->fsmn_layer(); i++) {
                decoder_inputs.emplace_back("in_cache_" + std::to_string(i));
                decoder_outputs.emplace_back("out_cache_" + std::to_string(i));
            }
            // encoder
            modules_[0].reset(Module::load(encoder_inputs, encoder_outputs, config_->encoder_model().c_str(), runtime_manager_, &module_config));
            // decoder
            modules_[1].reset(Module::load(decoder_inputs, decoder_outputs, config_->decoder_model().c_str(), runtime_manager_, &module_config));
        }

    } // namespace Transformer
} // namespace MNN
