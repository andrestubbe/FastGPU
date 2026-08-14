# FastGPU Version Changelog

## [0.1.1] — 2026-08-14

### Added
- **Vulkan 1.3 Compute Pipeline Integration**: Added SPIR-V compute shader dispatching (`volk`, `vk_context`, `vk_pipeline`, `vk_dispatch`).
- **Apple Silicon Metal Support**: Added Metal Shading Language (MSL) compute bindings for macOS M1–M4 Unified Memory architectures.
- **JMH Benchmark Suite**: Added `examples/Benchmark` project with `run-benchmark.bat` launcher measuring 1.4B+ ops/sec throughput.
- **FastAIModel Local AI Acceleration**: Native Vulkan GPU offloading for GGUF transformer models.
