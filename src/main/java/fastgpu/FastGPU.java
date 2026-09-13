package fastgpu;

public interface FastGPU extends AutoCloseable {

    static FastGPU openDefault() {
        return FastGPUImpl.open(FastGPUBackend.AUTO);
    }

    static FastGPU open(FastGPUBackend backend) {
        return FastGPUImpl.open(backend);
    }

    FastGPUBuffer allocFloatBuffer(int elements);

    FastGPUBuffer allocByteBuffer(int bytes);

    default FastGPUBuffer importHostBuffer(long nativeMemoryAddress, long bytes) {
        return allocByteBuffer((int) Math.min(bytes, Integer.MAX_VALUE));
    }

    FastGPUImage allocImage(int width, int height, Format format);

    FastGPUKernel compile(String name, String source, KernelLanguage lang);

    void dispatch(FastGPUKernel kernel, DispatchSize size, KernelArgs args);

    default void gemvQ4K(FastGPUBuffer weights, FastGPUBuffer input, FastGPUBuffer output, int rows, int cols) {
        // High-level fused GEMV interface for GGUF Type 12 (Q4_K)
    }

    @Override
    void close();
}
