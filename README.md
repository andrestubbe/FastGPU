# FastGPU 0.1.1 [ALPHA-2026-08] — High-Performance Native GPU Acceleration for Java

[![Status](https://img.shields.io/badge/status-0.1.1-brightgreen.svg)](https://github.com/andrestubbe/FastGPU/releases/tag/0.1.1)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://www.java.com)
[![Platform](https://img.shields.io/badge/Platform-Windows%2010+-lightgrey.svg)]()
[![JitPack](https://img.shields.io/badge/JitPack-0.1.1-green.svg)](https://jitpack.io/#andrestubbe/FastGPU)

---

**🎮 Advanced GPU-accelerated computing and rendering for the FastJava ecosystem. Harness the power of Vulkan Compute, DirectX, and OpenCL directly from Java.**

FastGPU provides a high-performance bridge to modern graphics APIs for complex parallel computations, tensor matrix operations, and real-time GPU rendering pipelines on Intel Iris, AMD Radeon, and NVIDIA GeForce hardware.

![Showcase](https://raw.githubusercontent.com/andrestubbe/FastGPU/main/docs/screenshot2.png)

---

## Quick Start

```java
import fastgpu.FastGPU;

public class FastGpuDemo {
    public static void main(String[] args) {
        // Initialize native Vulkan / Metal compute context
        try (FastGPU gpu = new FastGPU()) {
            System.out.println("==================================================");
            System.out.println("⚡ FastGPU Engine Initialized Successfully");
            System.out.println("Active Hardware GPU: " + gpu.getDeviceName());
            System.out.println("Vulkan Compute API: " + gpu.getVulkanVersion());
            System.out.println("==================================================");
        }
    }
}
```

---

## Table of Contents

- [Quick Start](#quick-start--example)
- [Why FastGPU?](#why-fastgpu)
- [Key Features](#key-features)
- [Real-World Use Cases](#real-world-use-cases)
- [Installation](#installation)
- [Demo Launchers](#demo-launchers)
- [Documentation](#documentation)
- [Platform Support](#platform-support)
- [License](#license)
- [Related Projects](#related-projects)

---

## Why FastGPU?

Standard Java graphics wrappers add heavy object overhead and JNI marshaling bottlenecks. FastGPU solves this by:

- **Vulkan Compute & OpenCL Backend**: Offload parallel matrix operations and GLSL compute kernels directly onto Intel Iris, AMD Radeon, and NVIDIA GeForce GPUs.
- **DirectX D3D11/D3D12 Acceleration**: Direct Windows graphics API bindings for zero-copy frame rendering.
- **Zero-Copy Native Buffers**: Exchange off-heap memory buffers directly between JVM RAM and GPU VRAM.

---

## Key Features

- **🌋 Vulkan 1.3 & Apple Metal Compute Engine**: Low-overhead SPIR-V & Metal Shading Language (MSL) compute pipeline dispatching for local AI matrix acceleration (**FastAIModel** & **FastAI**).
- **⚡ FlashAttention & Tensor Matrix Acceleration**: Fused compute kernels for 4-bit KV-cache and matrix multiplications on Intel Iris Xe, AMD Radeon, NVIDIA RTX, and Apple M1/M2/M3/M4 chips.
- **⚙️ Cross-Platform Metal & OpenCL Support**: Universal GPU acceleration across integrated Intel Iris Xe graphics, discrete GPUs, and macOS Apple Silicon Unified Memory architectures.
- **📥 Zero-Copy Unified Memory & VRAM Buffers**: Exchange off-heap memory buffers directly between JVM RAM and GPU VRAM at up to 300 GB/s.
- **📦 Bundled Multi-Platform Binaries**: Pre-compiled native C++ libraries (`fastgpu.dll`, `libfastgpu.dylib`, `libfastgpu.so`).

---

## Real-World Use Cases

- 🧠 **LLM GGUF Model Offloading**: Accelerate **[FastAIModel](https://github.com/andrestubbe/FastAIModel)** and **[FastAI](https://github.com/andrestubbe/FastAI)** transformer matrix multiplications on Intel Iris Xe, NVIDIA RTX, and Apple Silicon (M1–M4) via Vulkan & Metal.
- 🌊 **Real-Time Particle & Grid Physics**: Run parallel 3D fluid simulations and grid physics directly on GPU compute shaders (`run-demo2.bat`).
- 🎨 **High-Performance Vision Rendering**: Render 4K image frames from **[FastImage](https://github.com/andrestubbe/FastImage)** without CPU bottlenecks.

---

## Installation

### Prerequisites

For runtime GLSL kernel compilation, FastGPU recommends installing the Vulkan SDK:

```bash
winget install KhronosGroup.VulkanSDK
```

### Option 1: Maven (Recommended)

Add the JitPack repository and the dependency stack to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <!-- FastGPU Native Acceleration Engine -->
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>fastgpu</artifactId>
        <version>0.1.1</version>
    </dependency>

    <!-- FastCore Unified JNI Loader -->
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>FastCore</artifactId>
        <version>0.1.0</version>
    </dependency>
</dependencies>
```

### Option 2: Gradle (via JitPack)

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.andrestubbe:fastgpu:0.1.1'
    implementation 'com.github.andrestubbe:FastCore:0.1.0'
}
```

---

## Demo Launchers

- `run-demo.bat` — Launches the standard FastGPU compute shader test.
- `run-demo2.bat` — Launches the real-time fast fluid physics demo from `examples/Demo2`.
- `run-mandelbrot.bat` — Launches the GPU-accelerated Mandelbrot fractal renderer.

---

## Documentation

* **[CHANGELOG.md](docs/CHANGELOG.md)**: Release notes and version history.
* **[REFERENCE.md](docs/REFERENCE.md)**: Core API reference manual.
* **[PHILOSOPHY.md](docs/PHILOSOPHY.md)**: Engineering rationale for low-overhead GPU acceleration.
* **[COMPILE.md](docs/COMPILE.md)**: Full compilation guide (MSVC C++17 build chain + Vulkan SDK).
* **[ROADMAP.md](docs/ROADMAP.md)**: Future development goals.

---

## Platform Support

| Platform | Status |
|:---|:---:|
| Windows 10/11 (x64) | ✅ Fully Supported (Vulkan, DirectX, OpenCL) |
| Linux | 🔄 Planned (Vulkan, OpenCL) |
| macOS | 🔄 Planned (Metal) |

---

## License

MIT License — See [LICENSE](LICENSE) file for details.

---

## Related Projects

- [FastCore](https://github.com/andrestubbe/FastCore) — Native JNI loader for FastJava libraries
- [FastAIModel](https://github.com/andrestubbe/FastAIModel) — Native local LLM and embedding inference engine
- [FastImage](https://github.com/andrestubbe/FastImage) — Native SIMD image processing engine

---

Part of the FastJava Ecosystem — Making the JVM faster. Small package. Maximum speed. Zero bloat. ⚡
