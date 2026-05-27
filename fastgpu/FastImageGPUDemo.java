package fastgpu;

import fastimage.FastImage;
import java.awt.image.BufferedImage;
import java.util.Random;

public class FastImageGPUDemo {

    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("  FastImage <-> FastGPU Benchmark (v0.2.0)");
        System.out.println("==========================================");

        int width = 4096;
        int height = 4096;
        System.out.printf("Generating %dx%d test image (16 Megapixels)...%n", width, height);

        // 1. Generate test image
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Random rand = new Random();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int r = rand.nextInt(256);
                int g = rand.nextInt(256);
                int b = rand.nextInt(256);
                bi.setRGB(x, y, (255 << 24) | (r << 16) | (g << 8) | b);
            }
        }

        FastImage cpuImg = FastImage.fromBufferedImage(bi);

        // ==========================================
        // 2. CPU BENCHMARK (FastImage AVX2/SIMD)
        // ==========================================
        System.out.println("\n[1] Running CPU Benchmark (FastImage SIMD)...");
        long cpuStart = System.nanoTime();
        cpuImg.grayscale();
        long cpuEnd = System.nanoTime();
        double cpuMs = (cpuEnd - cpuStart) / 1_000_000.0;
        System.out.printf("    CPU Grayscale Time: %.2f ms%n", cpuMs);

        // ==========================================
        // 3. GPU BENCHMARK (FastGPU Vulkan Compute)
        // ==========================================
        System.out.println("\n[2] Running GPU Benchmark (FastGPU Vulkan)...");

        try (FastGPU gpu = FastGPU.openDefault()) {
            
            // Upload
            long uploadStart = System.nanoTime();
            FastGPUImage gpuInput = FastImageGPU.upload(gpu, FastImage.fromBufferedImage(bi));
            FastGPUImage gpuOutput = gpu.allocImage(width, height, Format.RGBA8);
            long uploadEnd = System.nanoTime();

            // Compile Kernel
            String glslSource = """
                    #version 450
                    layout(local_size_x = 16, local_size_y = 16) in;
                    
                    layout(rgba8, binding = 4) uniform readonly image2D inputImg;
                    layout(rgba8, binding = 5) uniform writeonly image2D outputImg;
                    
                    void main() {
                        ivec2 pos = ivec2(gl_GlobalInvocationID.xy);
                        ivec2 size = imageSize(inputImg);
                        if(pos.x >= size.x || pos.y >= size.y) return;
                        
                        vec4 color = imageLoad(inputImg, pos);
                        float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
                        imageStore(outputImg, pos, vec4(gray, gray, gray, color.a));
                    }
                    """;
            
            FastGPUKernel kernel = gpu.compile("grayscale", glslSource, KernelLanguage.GLSL_COMPUTE);

            // Dispatch
            long gpuComputeStart = System.nanoTime();
            gpu.dispatch(
                    kernel,
                    DispatchSize.of2D((width + 15) / 16, (height + 15) / 16),
                    KernelArgs.of(gpuInput, gpuOutput)
            );
            // In a real system, we'd vkQueueWaitIdle here, but our JNI dispatch waits internally
            long gpuComputeEnd = System.nanoTime();

            // Download
            long downloadStart = System.nanoTime();
            FastImage invertedCpuImg = FastImageGPU.download(gpuOutput);
            long downloadEnd = System.nanoTime();

            double uploadMs = (uploadEnd - uploadStart) / 1_000_000.0;
            double computeMs = (gpuComputeEnd - gpuComputeStart) / 1_000_000.0;
            double downloadMs = (downloadEnd - downloadStart) / 1_000_000.0;
            double totalGpuMs = uploadMs + computeMs + downloadMs;

            System.out.printf("    GPU Upload Time:   %.2f ms%n", uploadMs);
            System.out.printf("    GPU Compute Time:  %.2f ms%n", computeMs);
            System.out.printf("    GPU Download Time: %.2f ms%n", downloadMs);
            System.out.printf("    ------------------------------%n");
            System.out.printf("    Total GPU Time:    %.2f ms%n", totalGpuMs);

            System.out.println("\n==========================================");
            if (computeMs < cpuMs) {
                System.out.printf("  GPU Compute is %.1fx FASTER than CPU SIMD!%n", cpuMs / computeMs);
            } else {
                System.out.printf("  CPU SIMD is %.1fx FASTER than GPU Compute!%n", computeMs / cpuMs);
            }
            System.out.println("==========================================");

            // Cleanup
            gpuInput.free();
            gpuOutput.free();
            kernel.destroy();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
