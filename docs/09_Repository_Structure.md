# FastGPU: Finale Projektstruktur (v0.3 / v1.0)

Das Dokument definiert eine strikte, aufgeräumte Multi-Module-Architektur für FastGPU. Hier ist der komplette geplante Verzeichnisbaum, den wir im Projekt aufbauen sollten:

```text
fastgpu/
├── pom.xml (Root POM)
│
├── fastgpu-core/
│   ├── pom.xml
│   └── src/main/java/fastgpu/
│       ├── FastGPU.java
│       ├── FastGPUBuffer.java
│       ├── FastGPUImage.java
│       ├── FastGPUKernel.java
│       ├── DispatchSize.java
│       ├── KernelArgs.java
│       └── Format.java
│
├── fastgpu-native/
│   ├── pom.xml
│   ├── CMakeLists.txt
│   └── src/
│       ├── vulkan/
│       │   ├── vk_context.cpp
│       │   ├── vk_memory.cpp
│       │   ├── vk_buffers.cpp
│       │   ├── vk_descriptors.cpp
│       │   ├── vk_pipeline.cpp
│       │   ├── vk_dispatch.cpp
│       │   └── vk_shader.cpp
│       ├── cuda/                (Ab v0.5)
│       ├── spirv/
│       │   ├── spirv_compiler.cpp
│       │   └── spirv_compiler.h
│       └── jni/
│           ├── fastgpu_jni.cpp
│           └── fastgpu_jni.h
│
├── fastgpu-ir/
│   ├── pom.xml
│   └── src/main/java/fastgpu/ir/
│       ├── FastIRNode.java
│       ├── FastIRCompiler.java
│       └── FastLambdaCompiler.java
│
├── fastgpu-spirv/
│   ├── pom.xml
│   └── src/main/java/fastgpu/spirv/
│       ├── SpirvCompiler.java
│       └── SpirvModule.java
│
├── fastgpu-benchmarks/
│   ├── pom.xml
│   └── src/main/java/bench/
│       ├── VectorAddBench.java
│       ├── GemmBench.java
│       ├── BlurBench.java
│       └── MandelbrotBench.java
│
├── fastgpu-demos/
│   ├── pom.xml
│   └── src/main/java/demo/
│       ├── BlurDemo.java
│       ├── MandelbrotDemo.java
│       └── ParticlesDemo.java
│
└── fastgpu-agent-kit/
    ├── README.md
    ├── schema/
    │   └── fastgpu-command-schema.json
    ├── demos/
    │   ├── vector_add.json
    │   └── ...
    ├── benchmarks/
    └── failures/
```

Diese Struktur trennt strikt zwischen der Java-API (`core`), dem C++ Backend (`native`), experimentellen Compilern (`ir`, `spirv`), ausführbaren Beispielen (`demos`, `benchmarks`) und den Daten für KI-Agenten (`agent-kit`).
