# FastGPU API Reference Manual

`FastGPU` provides native Vulkan 1.3 compute capabilities, buffer management, and quantized GEMV acceleration for Java applications.

---

## Interface: `fastgpu.FastGPU`

Implements `AutoCloseable` for deterministic GPU resource management.

### Factory Methods

- `static FastGPU openDefault()`  
  Initializes the native Vulkan compute context using the best available physical device (`FastGPUBackend.AUTO`).

- `static FastGPU open(FastGPUBackend backend)`  
  Initializes the native context targeting a specific backend.

### Buffer & Image Management

- `FastGPUBuffer allocFloatBuffer(int elements)`  
  Allocates a host-visible, device-accessible float storage buffer.

- `FastGPUBuffer allocByteBuffer(int bytes)`  
  Allocates a raw byte storage buffer.

- `FastGPUBuffer importHostBuffer(long nativeMemoryAddress, long bytes)`  
  Imports or views host memory directly into a GPU buffer handle.

- `FastGPUImage allocImage(int width, int height, Format format)`  
  Allocates a GPU storage image for parallel compute shaders.

### Compute Shader Dispatching

- `FastGPUKernel compile(String name, String source, KernelLanguage lang)`  
  Compiles a GLSL (`KernelLanguage.GLSL_COMPUTE`) or SPIR-V kernel into a reusable Vulkan compute pipeline.

- `void dispatch(FastGPUKernel kernel, DispatchSize size, KernelArgs args)`  
  Dispatches compute workgroups to the GPU execution units via `vkCmdDispatch`.

### High-Level LLM / Transformer GEMV Kernels

- `void gemvQ4K(FastGPUBuffer weights, FastGPUBuffer input, FastGPUBuffer output, int rows, int cols)`  
  Dispatches fused matrix-vector multiplication for GGUF Type 12 (`Q4_K`) weights.

- `void gemvQ8_0(FastGPUBuffer weights, FastGPUBuffer input, FastGPUBuffer output, int rows, int cols)`  
  Dispatches fused matrix-vector multiplication for GGUF Type 8 (`Q8_0`) weights.

- `void gemvQ4_0(FastGPUBuffer weights, FastGPUBuffer input, FastGPUBuffer output, int rows, int cols)`  
  Dispatches fused matrix-vector multiplication for GGUF Type 2 (`Q4_0`) weights.

- `void close()`  
  Frees all native GPU compute contexts, compiled pipelines, and Vulkan device memory handles.

