# TODO: FastGPU 0.2.0 — Multi-GPU Vulkan Compute Engine

Baut auf `FastGPUImpl`, `vk_context.cpp`, `vk_buffer.cpp`, `vk_memory.cpp` und `volk.c` auf.

## 1. Multi-Device Detection & Enumeration
- [ ] In `vk_context.cpp`: Unterstützung für alle physischen GPUs via `vkEnumeratePhysicalDevices`.
- [ ] `FastGPUDevice`: Repräsentiert ein einzelnes Device (Index, Name, VRAM-Größe, Memory-Types).
- [ ] `FastGPUCluster`: Listet und verwaltet alle verfügbaren Compute-GPUs (z.B. Intel Iris Xe + diskrete GPU).

## 2. Layer-wise Sharding für LLM-Inferenz
- [ ] Aufteilung der Transformer-Layer auf mehrere Devices (z.B. Layer 0..15 auf GPU 0, 16..31 auf GPU 1).
- [ ] Asynchrone Command Buffer Submissions pro Device zur Minimierung von Idle-Zeiten.

## 3. Shared Memory & Cross-GPU Transfer
- [ ] Vulkan External Memory (`VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_FD_BIT` / `VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT`) für Zero-Copy KV-Cache-Zugriff.
- [ ] Direkter Austausch zwischen JVM MemorySegment und GPU VRAM Buffers ohne PCIe-Roundtrips.

## 4. Integration mit FastAIModel
- [ ] Anbindung an `FastAIStreamingModel` & `StreamingTransformerEngine` für Multi-GPU Layer Execution.
