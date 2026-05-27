# FastGPU: Vision & Konzept

## Kernidee
FastGPU ist das universelle GPU-Compute-Backend für das FastJava-Ökosystem. 
Es ist **ausschließlich für Berechnungen (Compute)** gedacht, nicht für Rendering, UI oder 3D-Grafiken.

### Was FastGPU macht:
- **GPU-Compute (Kernels):** Massiv parallele Operationen auf Arrays, Bildern, Matrizen (z.B. Matrix-Multiplikation, Convolution, Blur, FFT, AI-Preprocessing).
- **Zero-Copy Buffers:** Vermeidung unnötiger Transfers zwischen CPU und GPU durch `FastGPUBuffer` und `FastGPUImage`.
- **Backend-Abstraktion:** Entwickelt für Vulkan Compute (primär), mit Fallback-Möglichkeiten auf OpenCL oder Metal (via MoltenVK).

### Abgrenzung
- **FastGPU:** Reine Rechenleistung (CUDA-Ersatz).
- **FastGraphics:** Rendering (OpenGL/Vulkan-Graphics-Ersatz).
- **FastWindow:** Fensterverwaltung und UI.

## Warum Vulkan Compute?
1. **Maximale Kontrolle:** Direkte Kontrolle über Memory, Descriptor Sets und Workgroups.
2. **Plattformunabhängig:** Läuft auf Windows, Linux, Android, SteamDeck, etc.
3. **SPIR-V Unterstützung:** Perfekt als universelles IR-Format für die geplante Lambda-to-GPU-Pipeline.

## Abgrenzung zu Project Babylon / HAT
Während Project Babylon (OpenJDK) versucht, Java-Entwicklern durch High-Level-Abstraktionen den Zugang zu GPUs zu erleichtern, geht FastGPU den "FastJava-Weg":
- Minimalistisch, deterministisch und mit vollem Fokus auf Performance.
- Zero-Overhead durch direkten JNI-Vulkan-Zugriff statt breiter Abstraktion.
