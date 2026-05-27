# FastGPU: Advanced Features (v0.4 & v0.5)

Das Dokument enthält weitreichende Planungen für fortgeschrittene Versionen von FastGPU, die über den grundlegenden Compute-Dispatch hinausgehen. 

## FastGPU v0.4: Async Compute & Graphs
Ab v0.4 geht es in Richtung High-End GPU-Scheduling mit null CPU-Overhead.

### 1. Async Compute (`FastGPUStream`)
Operationen blockieren die CPU nicht, sondern reihen sich in einen Queue-Stream ein (vergleichbar mit CUDA Streams, aber Vulkan-basiert).
- **GPU Events / Timeline Semaphores** regeln die Synchronisation.
- **Multi-Queue Scheduling:** Compute und Transfer laufen parallel.

### 2. Command Graphs (`FastGPUGraph`)
Ein Command Graph baut eine feste Pipeline von Kernels auf.
- **Prinzip:** Einmal aufbauen -> beliebig oft dispatchen.
- **Ergebnis:** 0 CPU-Overhead beim Aufruf (`exec.run();`).

### 3. Persistent Pipelines & Zero-Copy
- Pipelines werden gecached und wiederverwendet.
- `gpu.copy(outA, inB);` erlaubt direkte GPU-zu-GPU Kopien ohne den Umweg über die CPU (Zero-Copy).

---

## FastGPU v0.5: Multi-Backend (CUDA & PTX)
Die ultimative Version: FastGPU ist nicht mehr nur Vulkan, sondern ein echtes Multi-Backend-System.

### 1. Unified Backend-API
Die Java-API bleibt identisch, aber im Hintergrund wird je nach Hardware das beste Backend gewählt:
```java
public enum FastGPUBackend { VULKAN, CUDA, AUTO }
FastGPU gpu = FastGPU.open(FastGPUBackend.AUTO);
```

### 2. CUDA C++ Backend
Vollständige native Implementierung für NVIDIA-Karten:
- **NVRTC** für On-The-Fly Kompilierung von CUDA-C Code in PTX.
- Driver API für Memory, Module und Kernel-Launch.

### 3. Unified Memory Model & Graphs
- `FastGPUBuffer` abstrahiert `VulkanBuffer` und `CudaBuffer`.
- **Multi-Backend Graphs:** Erlaubt Scheduling und Zero-Copy Transfers zwischen verschiedenen APIs (Vulkan <-> CUDA via Staging).
- **Backend-Capabilities:** API für Agenten, um zur Laufzeit abzufragen, welche Hardware / APIs verfügbar sind.
