# FastGPU: Demos & Benchmarks

Diese Datei enthält die Referenz-Implementierungen und Benchmarks, um die Leistungsfähigkeit von FastGPU zu demonstrieren und als Vorlage für Entwickler (sowie KI-Agenten) zu dienen.

## 1. Benchmarks

### VectorAdd (1D)
Vergleich der Ausführung von 1 Million floats auf CPU vs GPU.
```java
public static void run(int n) {
    FastGPU gpu = FastGPU.openDefault();
    
    FastGPUBuffer a = gpu.allocFloat(n);
    FastGPUBuffer b = gpu.allocFloat(n);
    FastGPUBuffer out = gpu.allocFloat(n);
    
    // Daten hochladen...
    
    FastGPUKernel k = gpu.compile("vector_add", kernelSource, KernelLanguage.GLSL_COMPUTE);
    gpu.dispatch(k, DispatchSize.of1D(n), KernelArgs.of(a, b, out));
    
    // Download und Zeiten vergleichen
}
// Erwarteter Output: VectorAdd n=10000000 CPU=12.3ms GPU=0.8ms
```

### GEMM (Matrix Multiply)
Naive CPU Multiplikation vs GPU Compute Kernel.
```java
public static void run(int size) {
    // 512x512 oder 1024x1024 Matrix
    // Benchmark.gemm(512);
}
// Erwarteter Output: GEMM n=512 CPU=145.0ms GPU=4.2ms
```

---

## 2. Demos

### Image Blur (Gaussian Blur)
Einlesen eines PNGs, GPU-gestützte Unschärfe und Speichern als neues PNG.
```java
public class BlurDemo {
    public static void main(String[] args) throws Exception {
        FastGPU gpu = FastGPU.openDefault();
        
        BufferedImage img = ImageIO.read(new File("input.png"));
        FastGPUImage gpuImg = gpu.uploadImage(img);
        
        // FastGPUKernel für Blur kompilieren und ausführen
        gpu.blur(gpuImg); // High-Level Hilfsfunktion
        
        BufferedImage out = gpuImg.download();
        ImageIO.write(out, "png", new File("out.png"));
    }
}
// Erwarteter Output: Blur 1920x1080 CPU=38.1ms GPU=2.1ms
```

### Mandelbrot (Compute -> Image)
Ein reiner GPU-Compute-Shader, der direkt in einen Image-Buffer schreibt, um ein 4K-Fraktal zu rendern.
```java
public class MandelbrotDemo {
    public static void main(String[] args) {
        FastGPU gpu = FastGPU.openDefault();
        int w = 3840;
        int h = 2160;
        
        FastGPUImage out = gpu.allocImage(w, h, Format.RGBA8);
        FastGPUKernel mandelKernel = gpu.compile("mandelbrot", src, KernelLanguage.GLSL_COMPUTE);
        
        gpu.dispatch(mandelKernel, DispatchSize.of2D(w, h), KernelArgs.of(out));
        
        // Download und rendern/speichern
    }
}
```

### Particle Simulation
Simuliert 100.000 Partikel (Position/Velocity Update) auf der GPU und gibt die Daten für das Rendering (z.B. mit FastWindow/JavaFX) zurück.
```java
public class ParticlesDemo {
    public static void main(String[] args) {
        // Demo.particles(100_000);
        // GPU-Update im Loop, CPU-Render
    }
}
```
