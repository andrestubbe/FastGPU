# FastGPU API Reference Manual

`FastGPU` provides native Vulkan 1.3, Apple Metal, DirectX, and OpenCL compute capabilities for Java applications.

---

## Class: `fastgpu.FastGPU`

Implements `AutoCloseable` for deterministic GPU resource management.

### Constructors

- `public FastGPU()`  
  Initializes the native C++ Vulkan 1.3 / Metal GPU compute context and retrieves active device information.

### Methods

- `public String getDeviceName()`  
  Returns the name of the active physical GPU hardware (e.g. `"Intel(R) Iris(R) Xe Graphics"`, `"NVIDIA GeForce RTX 4090"`, `"Apple M3 Pro"`).

- `public String getVulkanVersion()`  
  Returns the active Vulkan API version string supported by the driver (e.g. `"1.3.280"`).

- `public long getNativeHandle()`  
  Returns the raw memory pointer (`uintptr_t`) to the underlying C++ `FastGPUContext` object.

- `public void close()`  
  Frees all native GPU compute contexts, SPIR-V pipeline caches, and Vulkan device memory handles.

---

## Native C++ API (`fastgpu.dll` / `libfastgpu.dylib`)

- `FastGPU_Init()` — Creates Vulkan instance, physical device selector, and logical compute queue.
- `FastGPU_GetDeviceName()` — Fills target string buffer with device hardware string.
- `FastGPU_DispatchCompute()` — Submits SPIR-V / Metal MSL compute shader workloads to the GPU queue.
- `FastGPU_Free()` — Destroys Vulkan compute pipelines and command pools.
