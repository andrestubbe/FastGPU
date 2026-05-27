# FastGPU: Vulkan Backend & JNI

Das Low-Level Backend ist vollständig in C++ geschrieben und über JNI mit der Java-API verbunden. **KI-Agenten sollten diesen Code niemals dynamisch generieren.**

## Architektur-Ebenen

### 1. JNI Bridge (`fastgpu_jni.cpp`)
Das Bindeglied zwischen Java und C++.
- `nativeCreate()`: Erzeugt den VulkanContext.
- `nativeAllocBuffer()`: Allokiert Device-Memory.
- `nativeUpload()` / `nativeDownload()`: Datentransfer via Staging-Buffers.
- `nativeDispatch()`: Führt den Kernel aus.

### 2. Vulkan Context (`vk_context.cpp`)
Initialisiert die Vulkan-Instanz, wählt das Physical Device, erzeugt Queue, Command Pools und Descriptor Pools.

### 3. Memory Management (`vk_memory.cpp`, `vk_buffers.cpp`)
Implementiert Memory Pools zur Reduzierung von Allokations-Overhead:
- `HostVisiblePool`: Für Staging und kleine Datentransfers.
- `DeviceLocalPool`: Für den eigentlichen Compute-Speicher.
- Suballocation über Free-Lists.

### 4. Command & Descriptor Management
- **Persistent Command Buffers:** Anstatt für jeden Dispatch neue Command Buffer zu erzeugen, werden diese pro Thread gepoolt und wiederverwendet.
- **Descriptor Set Cache:** Schnelles Binden von Argumenten (Buffers/Images) an den Shader über einen globalen `VkDescriptorPool`.

### 5. Pipeline & Shader (`vk_pipeline.cpp`, `vk_shader.cpp`)
Kompiliert SPIR-V Binaries in `VkShaderModule` und erstellt die `VkComputePipeline`.
Nutzt `shaderc` oder `glslang` zur Laufzeitkompilierung von GLSL Compute Shadern nach SPIR-V, falls der Input nicht direkt SPIR-V ist.
