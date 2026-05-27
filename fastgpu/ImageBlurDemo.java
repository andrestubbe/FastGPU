package fastgpu;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class ImageBlurDemo {

    public static void main(String[] args) {
        System.out.println("Starting FastGPU ImageBlurDemo (v0.1.0)");

        // 1. Initialize FastGPU Context
        try (FastGPU gpu = FastGPU.openDefault()) {
            System.out.println("FastGPU Context opened successfully!");

            int width = 512;
            int height = 512;
            System.out.println("Allocating images of size " + width + "x" + height + "...");

            // 2. Allocate Images
            FastGPUImage inputImage = gpu.allocImage(width, height, Format.RGBA8);
            FastGPUImage outputImage = gpu.allocImage(width, height, Format.RGBA8);

            // 3. Prepare Data (Draw something on a BufferedImage)
            BufferedImage javaInputImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = javaInputImage.createGraphics();
            g2d.setColor(Color.RED);
            g2d.fillRect(100, 100, 300, 300);
            g2d.setColor(Color.BLUE);
            g2d.fillOval(200, 200, 100, 100);
            g2d.dispose();

            // 4. Upload Data to GPU (or CPU stub)
            System.out.println("Uploading image...");
            inputImage.upload(javaInputImage);

            // 5. Compile Kernel (Gaussian Blur example)
            String glslSource = """
                    #version 450
                    layout(local_size_x = 16, local_size_y = 16) in;
                    
                    layout(rgba8, binding = 0) uniform readonly image2D imgInput;
                    layout(rgba8, binding = 1) uniform writeonly image2D imgOutput;
                    
                    void main() {
                        ivec2 pos = ivec2(gl_GlobalInvocationID.xy);
                        // Dummy blur logic
                        vec4 color = imageLoad(imgInput, pos);
                        imageStore(imgOutput, pos, color);
                    }
                    """;

            System.out.println("Compiling Kernel...");
            FastGPUKernel kernel = gpu.compile("image_blur", glslSource, KernelLanguage.GLSL_COMPUTE);

            // 6. Dispatch Kernel
            System.out.println("Dispatching Kernel...");
            gpu.dispatch(
                    kernel,
                    DispatchSize.of3D(width / 16, height / 16, 1),
                    KernelArgs.of(inputImage, outputImage)
            );

            // 7. Download Results
            System.out.println("Downloading results...");
            BufferedImage javaOutputImage = outputImage.download();

            // 8. Verify
            System.out.println("Result Check (Center pixel):");
            int centerRGB = javaOutputImage.getRGB(width / 2, height / 2);
            System.out.printf("  Center Pixel Color (ARGB): %08X%n", centerRGB);

            System.out.println("Demo completed successfully!");

            // 9. Cleanup
            inputImage.free();
            outputImage.free();
            kernel.destroy();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
