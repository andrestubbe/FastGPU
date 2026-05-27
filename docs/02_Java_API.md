# FastGPU: Java API & Architektur

Die Java-API ist die einzige Schicht, die der Entwickler (und ein KI-Agent) jemals zu Gesicht bekommt. Sie ist strikt getrennt vom C++ Backend.

## Core Interfaces

### 1. FastGPU (Context)
Der Einstiegspunkt für alle GPU-Operationen.
```java
public interface FastGPU {
    static FastGPU openDefault() { return new FastGPUImpl(); }

    FastGPUBuffer allocFloat(int elements);
    FastGPUBuffer allocByte(int bytes);
    FastGPUImage allocImage(int w, int h, Format fmt);

    FastGPUKernel compile(String name, String source, KernelLanguage lang);
    void dispatch(FastGPUKernel kernel, DispatchSize size, KernelArgs args);
}
```

### 2. FastGPUBuffer & Image
Verwaltung des GPU-Speichers (Zero-Copy).
```java
public interface FastGPUBuffer {
    void upload(float[] data);
    void download(float[] out);
    long size();
    void free();
}
```

### 3. Dispatch & Kernel Arguments
```java
public record DispatchSize(int x, int y, int z) {
    public static DispatchSize of1D(int x) { return new DispatchSize(x, 1, 1); }
    public static DispatchSize of2D(int x, int y) { return new DispatchSize(x, y, 1); }
}
```

## Projektstruktur (Maven Multi-Module)
Das Projekt wird in übersichtliche Module unterteilt:
- `fastgpu-core`: Java API und Interfaces.
- `fastgpu-native`: C++ Vulkan Backend und JNI C-Bridge.
- `fastgpu-ir`: Der Lambda-to-IR Compiler.
- `fastgpu-spirv`: SPIR-V Generierung.
- `fastgpu-benchmarks`: Performance-Tests (VectorAdd, GEMM).
- `fastgpu-demos`: Anwendungsbeispiele (Blur, Mandelbrot).
