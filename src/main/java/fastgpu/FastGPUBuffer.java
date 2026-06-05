package fastgpu;

public interface FastGPUBuffer {

    long sizeBytes();

    void upload(float[] data);

    void upload(byte[] data);

    void download(float[] out);

    void download(byte[] out);

    void free();
}
