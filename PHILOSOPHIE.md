# The Philosophy of FastGPU

> [!IMPORTANT]
> **"Keine Kopien. Niemals. Kritischer JNI-Pfad. Native-First Performance."**

FastGPU is built on the principle that modern Java applications require **native-first** acceleration for performance-critical operations that the standard JVM APIs don't fully optimize.

## Core Tenets

1. **Native-First Execution (Vulkan)**
   Bypass standard Java layers to reach the physical limits of the hardware using a handcrafted C++ Vulkan Compute Backend.

2. **Zero-Copy JNI Architecture & UMA**
   Minimize JNI transition costs by mapping GPU memory directly into Java (`DirectByteBuffer`). Especially on Unified Memory Architecture (UMA) systems like Intel Iris, we enforce `HOST_VISIBLE | HOST_COHERENT` memory to completely eliminate implicit memory copies between the JVM and the native layer. 

3. **Strict Modularity & The Bridge Pattern**
   FastGPU knows nothing about CPU-bound image processing (`FastImage`). FastGPU is purely a compute backend. CPU-SIMD (FastImage) and GPU-Compute (FastGPU) are strictly separated. Data exchange is handled exclusively via optional Bridge layers (e.g., `FastImageGPU`), ensuring that no module drags down the other.

4. **The "Developer Dream" Workflow**
   Writing compute shaders shouldn't require complex build setups. FastGPU provides dynamic runtime compilation: pass your `GLSL` strings directly from Java, and the C++ backend will invisibly trigger `glslc` to compile them into `SPIR-V` on the fly. Fast iteration, native execution.

5. **Blueprint Consistency**
   As part of the **FastJava** ecosystem, FastGPU adheres to a standardized architecture:
   *   **Native Backend**: Direct C++ implementation via `fastgpu.dll`.
   *   **Unified Loading**: Powered by `FastCore`.
   *   **Premium Quality**: Built for high-performance systems and autonomous agents.

---
**⚡ FastGPU — Powering the next generation of Native Java.**
