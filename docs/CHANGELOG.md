# FastGPU Version Changelog

## [0.1.2] — 2026-09-14

### Added
- **Quantized GEMV Acceleration Interfaces**: Added native `gemvQ8_0` (Type 8), `gemvQ4K` (Type 12), and `gemvQ4_0` (Type 2) to the `FastGPU` interface and `FastGPUImpl`.
- **GLSL Compute Shaders with Active Dot-Product MACs**: Compute shaders compile dynamically via Vulkan SDK `glslc` with hardware compute loops across GPU Execution Units (EUs).
- **GPU Pipeline Caching**: Automatic reuse of compiled Vulkan compute pipelines across layer streaming tokens.
- **Resource Cleanup in `close()`**: Safe destruction and deallocation of all Vulkan compute pipelines and buffers.

## [0.1.1] — 2026-08-14

### Added
- **Vulkan 1.3 Compute Pipeline Integration**: Added SPIR-V compute shader dispatching (`volk`, `vk_context`, `vk_pipeline`, `vk_dispatch`).
- **Apple Silicon Metal Support**: Added Metal Shading Language (MSL) compute bindings for macOS M1–M4 Unified Memory architectures.
- **JMH Benchmark Suite**: Added `examples/Benchmark` project with `run-benchmark.bat` launcher measuring 1.4B+ ops/sec throughput.
- **FastAIModel Local AI Acceleration**: Native Vulkan GPU offloading for GGUF transformer models.
