package fastgpu;

import java.util.Arrays;

public class VectorAddDemo {

    public static void main(String[] args) {
        System.out.println("Starting FastGPU VectorAddDemo (v0.1.0)");

        // 1. Initialize FastGPU Context
        try (FastGPU gpu = FastGPU.openDefault()) {
            System.out.println("FastGPU Context opened successfully!");

            int elements = 1024;
            System.out.println("Allocating buffers for " + elements + " elements...");

            // 2. Allocate Buffers
            FastGPUBuffer bufferA = gpu.allocFloatBuffer(elements);
            FastGPUBuffer bufferB = gpu.allocFloatBuffer(elements);
            FastGPUBuffer bufferC = gpu.allocFloatBuffer(elements); // Result buffer

            // 3. Prepare Data
            float[] dataA = new float[elements];
            float[] dataB = new float[elements];
            for (int i = 0; i < elements; i++) {
                dataA[i] = i;
                dataB[i] = i * 2;
            }

            // 4. Upload Data to GPU (or CPU stub)
            System.out.println("Uploading data...");
            bufferA.upload(dataA);
            bufferB.upload(dataB);

            // 5. Compile Kernel
            String glslSource = """
                    #version 450
                    layout(local_size_x = 256) in;
                    
                    layout(std430, binding = 0) buffer BufA { float a[]; };
                    layout(std430, binding = 1) buffer BufB { float b[]; };
                    layout(std430, binding = 2) buffer BufC { float c[]; };
                    
                    void main() {
                        uint id = gl_GlobalInvocationID.x;
                        c[id] = a[id] + b[id];
                    }
                    """;
            
            System.out.println("Compiling Kernel...");
            FastGPUKernel kernel = gpu.compile("vector_add", glslSource, KernelLanguage.GLSL_COMPUTE);

            // 6. Dispatch Kernel
            System.out.println("Dispatching Kernel...");
            gpu.dispatch(
                    kernel,
                    DispatchSize.of1D(elements / 256), // dispatch size based on local_size_x
                    KernelArgs.of(bufferA, bufferB, bufferC)
            );

            // 7. Download Results
            System.out.println("Downloading results...");
            float[] result = new float[elements];
            bufferC.download(result);

            // 8. Verify
            System.out.println("Result Check (First 5 elements):");
            for (int i = 0; i < 5; i++) {
                System.out.printf("  %d + %d = %f%n", (int)dataA[i], (int)dataB[i], result[i]);
            }
            
            System.out.println("Demo completed successfully!");

            // 9. Cleanup
            bufferA.free();
            bufferB.free();
            bufferC.free();
            kernel.destroy();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
