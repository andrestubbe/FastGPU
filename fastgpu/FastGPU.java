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

    FastGPUImage allocImage(int width, int height, Format format);

    FastGPUKernel compile(String name, String source, KernelLanguage lang);

    void dispatch(FastGPUKernel kernel, DispatchSize size, KernelArgs args);

    @Override
    void close();
}
