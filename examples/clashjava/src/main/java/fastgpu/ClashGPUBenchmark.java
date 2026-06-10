package fastgpu;

import java.util.Random;

public class ClashGPUBenchmark {

    private static final int OBJECT_COUNT = 65536;
    private static final int ITERATIONS = 200;
    private static final float DT = 0.016f;
    private static final float WIDTH = 1280f;
    private static final float HEIGHT = 720f;

    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("       FastGPU Clash GPU Benchmark");
        System.out.println("==========================================");
        System.out.printf("Simulating %,d moving objects for %,d steps%n", OBJECT_COUNT, ITERATIONS);

        float[] state = createInitialState(OBJECT_COUNT, WIDTH, HEIGHT);
        cpuBenchmark(state.clone());
        gpuBenchmark(state);
    }

    private static float[] createInitialState(int count, float width, float height) {
        Random random = new Random(12345);
        float[] state = new float[count * 4];
        for (int i = 0; i < count; i++) {
            int base = i * 4;
            state[base] = random.nextFloat() * width;
            state[base + 1] = random.nextFloat() * height;
            state[base + 2] = (random.nextFloat() * 2f - 1f) * 320f;
            state[base + 3] = (random.nextFloat() * 2f - 1f) * 320f;
        }
        return state;
    }

    private static void cpuBenchmark(float[] state) {
        System.out.println("\n[CPU] Running boundary bounce simulation...");
        warmupCpu(state.clone());
        long start = System.nanoTime();
        runCpuSteps(state, OBJECT_COUNT, ITERATIONS, WIDTH, HEIGHT, DT);
        long end = System.nanoTime();
        double ms = (end - start) / 1_000_000.0;
        System.out.printf("    CPU Time: %.2f ms (%d iterations)%n", ms, ITERATIONS);
    }

    private static void warmupCpu(float[] state) {
        runCpuSteps(state, OBJECT_COUNT, 20, WIDTH, HEIGHT, DT);
    }

    private static void runCpuSteps(float[] state, int count, int iterations, float width, float height, float dt) {
        for (int step = 0; step < iterations; step++) {
            for (int i = 0; i < count; i++) {
                int base = i * 4;
                float x = state[base];
                float y = state[base + 1];
                float vx = state[base + 2];
                float vy = state[base + 3];

                x += vx * dt;
                y += vy * dt;

                if (x < 0f) {
                    x = 0f;
                    vx = Math.abs(vx);
                } else if (x > width) {
                    x = width;
                    vx = -Math.abs(vx);
                }

                if (y < 0f) {
                    y = 0f;
                    vy = Math.abs(vy);
                } else if (y > height) {
                    y = height;
                    vy = -Math.abs(vy);
                }

                state[base] = x;
                state[base + 1] = y;
                state[base + 2] = vx;
                state[base + 3] = vy;
            }
        }
    }

    private static void gpuBenchmark(float[] initialState) {
        System.out.println("\n[GPU] Running FastGPU boundary bounce benchmark...");
        try (FastGPU gpu = FastGPU.openDefault()) {
            int elementCount = initialState.length;
            FastGPUBuffer stateBuffer = gpu.allocFloatBuffer(elementCount);
            FastGPUBuffer paramsBuffer = gpu.allocFloatBuffer(4);

            stateBuffer.upload(initialState);
            paramsBuffer.upload(new float[]{WIDTH, HEIGHT, DT, OBJECT_COUNT});

            String glslSource = """
                    #version 450
                    layout(local_size_x = 256) in;

                    layout(std430, binding = 0) buffer State { float state[]; };
                    layout(std430, binding = 1) buffer Params { float width; float height; float dt; float count; };

                    void main() {
                        uint id = gl_GlobalInvocationID.x;
                        if (id >= uint(count)) {
                            return;
                        }

                        uint offset = id * 4u;
                        float x = state[offset + 0u];
                        float y = state[offset + 1u];
                        float vx = state[offset + 2u];
                        float vy = state[offset + 3u];

                        x += vx * dt;
                        y += vy * dt;

                        if (x < 0.0) {
                            x = 0.0;
                            vx = abs(vx);
                        } else if (x > width) {
                            x = width;
                            vx = -abs(vx);
                        }

                        if (y < 0.0) {
                            y = 0.0;
                            vy = abs(vy);
                        } else if (y > height) {
                            y = height;
                            vy = -abs(vy);
                        }

                        state[offset + 0u] = x;
                        state[offset + 1u] = y;
                        state[offset + 2u] = vx;
                        state[offset + 3u] = vy;
                    }
                    """;

            FastGPUKernel kernel = gpu.compile("clash_bounce", glslSource, KernelLanguage.GLSL_COMPUTE);
            int dispatchCount = (OBJECT_COUNT + 255) / 256;

            // Warmup
            for (int i = 0; i < 10; i++) {
                gpu.dispatch(kernel, DispatchSize.of1D(dispatchCount), KernelArgs.of(stateBuffer, paramsBuffer));
            }

            long start = System.nanoTime();
            for (int i = 0; i < ITERATIONS; i++) {
                gpu.dispatch(kernel, DispatchSize.of1D(dispatchCount), KernelArgs.of(stateBuffer, paramsBuffer));
            }
            long end = System.nanoTime();

            float[] result = new float[initialState.length];
            stateBuffer.download(result);

            double ms = (end - start) / 1_000_000.0;
            System.out.printf("    GPU Compute Time: %.2f ms (%d iterations)%n", ms, ITERATIONS);
            System.out.printf("    GPU Throughput: %.2f million updates/sec%n",
                    OBJECT_COUNT * (double) ITERATIONS / (ms / 1000.0) / 1_000_000.0);
            System.out.printf("    Sample state [0]: x=%.2f y=%.2f vx=%.2f vy=%.2f%n",
                    result[0], result[1], result[2], result[3]);

            stateBuffer.free();
            paramsBuffer.free();
            kernel.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
