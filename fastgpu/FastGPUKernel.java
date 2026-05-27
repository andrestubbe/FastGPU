package fastgpu;

public interface FastGPUKernel {

    String name();

    KernelLanguage language();

    void destroy();
}
