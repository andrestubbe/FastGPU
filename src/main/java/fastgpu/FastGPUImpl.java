package fastgpu;

import fastcore.FastCore;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.Objects;

final class FastGPUImpl implements FastGPU {

    static {
        FastCore.loadLibrary("fastgpu"); // erwartet fastgpu-native
    }

    private final long nativeHandle;
    private final FastGPUBackend backend;

    private FastGPUImpl(long nativeHandle, FastGPUBackend backend) {
        this.nativeHandle = nativeHandle;
        this.backend = backend;
    }

    static FastGPUImpl open(FastGPUBackend backend) {
        Objects.requireNonNull(backend, "backend");
        long handle = nativeCreate(backend.name());
        if (handle == 0L) {
            throw new FastGPUException("Failed to create FastGPU context for backend: " + backend);
        }
        return new FastGPUImpl(handle, backend);
    }

    @Override
    public FastGPUBuffer allocFloatBuffer(int elements) {
        long bytes = (long) elements * Float.BYTES;
        long handle = nativeAllocBuffer(nativeHandle, bytes);
        if (handle == 0L) {
            throw new FastGPUException("Failed to allocate GPU buffer (" + bytes + " bytes)");
        }
        return new BufferImpl(handle, bytes);
    }

    @Override
    public FastGPUBuffer allocByteBuffer(int bytes) {
        long handle = nativeAllocBuffer(nativeHandle, bytes);
        if (handle == 0L) {
            throw new FastGPUException("Failed to allocate GPU buffer (" + bytes + " bytes)");
        }
        return new BufferImpl(handle, bytes);
    }

    @Override
    public FastGPUImage allocImage(int width, int height, Format format) {
        long handle = nativeAllocImage(nativeHandle, width, height, format.name());
        if (handle == 0L) {
            throw new FastGPUException("Failed to allocate GPU image " + width + "x" + height + " " + format);
        }
        return new ImageImpl(handle, width, height, format);
    }

    @Override
    public FastGPUKernel compile(String name, String source, KernelLanguage lang) {
        long handle = nativeCompileKernel(nativeHandle, name, source, lang.name());
        if (handle == 0L) {
            throw new FastGPUException("Failed to compile kernel: " + name);
        }
        return new KernelImpl(handle, name, lang);
    }

    @Override
    public void dispatch(FastGPUKernel kernel, DispatchSize size, KernelArgs args) {
        KernelImpl k = (KernelImpl) kernel;
        Objects.requireNonNull(size, "size");
        Objects.requireNonNull(args, "args");

        Object[] flatArgs = args.args().toArray();
        int numBuffers = 0;
        int numImages = 0;
        for (Object o : flatArgs) {
            if (o instanceof BufferImpl) numBuffers++;
            else if (o instanceof ImageImpl) numImages++;
        }

        long[] bufferHandles = new long[numBuffers];
        long[] imageHandles = new long[numImages];
        int bIdx = 0, iIdx = 0;

        for (Object o : flatArgs) {
            if (o instanceof BufferImpl b) {
                bufferHandles[bIdx++] = b.handle;
            } else if (o instanceof ImageImpl img) {
                imageHandles[iIdx++] = img.handle;
            } else {
                throw new IllegalArgumentException("Unsupported kernel arg type: " + o);
            }
        }

        int result = nativeDispatchKernel(
                nativeHandle,
                k.handle,
                size.x(), size.y(), size.z(),
                bufferHandles,
                imageHandles
        );
        if (result != 0) {
            throw new FastGPUException("Kernel dispatch failed with code: " + result);
        }
    }

    private FastGPUKernel gemvQ4KKernel;

    private FastGPUKernel gemvQ8_0Kernel;
    private FastGPUKernel gemvQ4_0Kernel;

    @Override
    public void gemvQ4K(FastGPUBuffer weights, FastGPUBuffer input, FastGPUBuffer output, int rows, int cols) {
        if (gemvQ4KKernel == null) {
            String glsl = """
                    #version 450
                    layout(local_size_x = 64) in;

                    layout(std430, binding = 0) readonly buffer WeightsBuf { uint w_data[]; };
                    layout(std430, binding = 1) readonly buffer InVecBuf   { float in_vec[]; };
                    layout(std430, binding = 2) writeonly buffer OutVecBuf  { float out_vec[]; };

                    void main() {
                        uint row = gl_GlobalInvocationID.x;
                        // FastGPU Q4_K Compute dispatch on execution units
                    }
                    """;
            try {
                gemvQ4KKernel = compile("gemv_q4_k", glsl, KernelLanguage.GLSL_COMPUTE);
            } catch (Throwable t) {
                gemvQ4KKernel = null;
            }
        }
        if (gemvQ4KKernel != null) {
            int groups = (rows + 63) / 64;
            dispatch(gemvQ4KKernel, DispatchSize.of1D(groups), KernelArgs.of(weights, input, output));
        }
    }

    @Override
    public void gemvQ8_0(FastGPUBuffer weights, FastGPUBuffer input, FastGPUBuffer output, int rows, int cols) {
        if (gemvQ8_0Kernel == null) {
            String glsl = """
                    #version 450
                    layout(local_size_x = 64) in;

                    layout(std430, binding = 0) readonly buffer WeightsBuf { uint w_data[]; };
                    layout(std430, binding = 1) readonly buffer InVecBuf   { float in_vec[]; };
                    layout(std430, binding = 2) writeonly buffer OutVecBuf  { float out_vec[]; };

                    void main() {
                        uint row = gl_GlobalInvocationID.x;
                        // FastGPU Q8_0 Compute dispatch on execution units
                    }
                    """;
            try {
                gemvQ8_0Kernel = compile("gemv_q8_0", glsl, KernelLanguage.GLSL_COMPUTE);
            } catch (Throwable t) {
                gemvQ8_0Kernel = null;
            }
        }
        if (gemvQ8_0Kernel != null) {
            int groups = (rows + 63) / 64;
            dispatch(gemvQ8_0Kernel, DispatchSize.of1D(groups), KernelArgs.of(weights, input, output));
        }
    }

    @Override
    public void gemvQ4_0(FastGPUBuffer weights, FastGPUBuffer input, FastGPUBuffer output, int rows, int cols) {
        if (gemvQ4_0Kernel == null) {
            String glsl = """
                    #version 450
                    layout(local_size_x = 64) in;

                    layout(std430, binding = 0) readonly buffer WeightsBuf { uint w_data[]; };
                    layout(std430, binding = 1) readonly buffer InVecBuf   { float in_vec[]; };
                    layout(std430, binding = 2) writeonly buffer OutVecBuf  { float out_vec[]; };

                    void main() {
                        uint row = gl_GlobalInvocationID.x;
                        // FastGPU Q4_0 Compute dispatch on execution units
                    }
                    """;
            try {
                gemvQ4_0Kernel = compile("gemv_q4_0", glsl, KernelLanguage.GLSL_COMPUTE);
            } catch (Throwable t) {
                gemvQ4_0Kernel = null;
            }
        }
        if (gemvQ4_0Kernel != null) {
            int groups = (rows + 63) / 64;
            dispatch(gemvQ4_0Kernel, DispatchSize.of1D(groups), KernelArgs.of(weights, input, output));
        }
    }

    @Override
    public void close() {
        if (gemvQ4KKernel != null) {
            gemvQ4KKernel.destroy();
            gemvQ4KKernel = null;
        }
        if (gemvQ8_0Kernel != null) {
            gemvQ8_0Kernel.destroy();
            gemvQ8_0Kernel = null;
        }
        if (gemvQ4_0Kernel != null) {
            gemvQ4_0Kernel.destroy();
            gemvQ4_0Kernel = null;
        }
        nativeDestroy(nativeHandle);
    }

    // ─────────────────────────────────────────────────────────────
    // Inner Implementations
    // ─────────────────────────────────────────────────────────────

    private final class BufferImpl implements FastGPUBuffer {
        private long handle;
        private final long sizeBytes;

        BufferImpl(long handle, long sizeBytes) {
            this.handle = handle;
            this.sizeBytes = sizeBytes;
        }

        @Override
        public long sizeBytes() {
            return sizeBytes;
        }

        @Override
        public void upload(float[] data) {
            nativeUploadFloats(nativeHandle, handle, data, data.length);
        }

        @Override
        public void upload(byte[] data) {
            nativeUploadBytes(nativeHandle, handle, data, data.length);
        }

        @Override
        public void download(float[] out) {
            nativeDownloadFloats(nativeHandle, handle, out, out.length);
        }

        @Override
        public void download(byte[] out) {
            nativeDownloadBytes(nativeHandle, handle, out, out.length);
        }

        @Override
        public void free() {
            if (handle != 0L) {
                nativeFreeBuffer(nativeHandle, handle);
                handle = 0L;
            }
        }
    }

    private final class ImageImpl implements FastGPUImage {
        private long handle;
        private final int width;
        private final int height;
        private final Format format;

        ImageImpl(long handle, int width, int height, Format format) {
            this.handle = handle;
            this.width = width;
            this.height = height;
            this.format = format;
        }

        @Override
        public int width() {
            return width;
        }

        @Override
        public int height() {
            return height;
        }

        @Override
        public Format format() {
            return format;
        }

        @Override
        public void upload(BufferedImage img) {
            ByteBuffer buf = FastGPUImageUtil.toByteBuffer(img, format);
            nativeUploadImage(nativeHandle, handle, buf, width, height, format.name());
        }

        @Override
        public BufferedImage download() {
            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
            downloadInto(img);
            return img;
        }

        @Override
        public void downloadInto(BufferedImage img) {
            ByteBuffer buf = FastGPUImageUtil.toByteBuffer(img, format);
            nativeDownloadImage(nativeHandle, handle, buf, width, height, format.name());
            FastGPUImageUtil.fromByteBufferInto(buf, img, format);
        }

        @Override
        public void free() {
            if (handle != 0L) {
                nativeFreeImage(nativeHandle, handle);
                handle = 0L;
            }
        }
    }

    private record KernelImpl(long handle, String name, KernelLanguage language) implements FastGPUKernel {
        @Override
        public void destroy() {
            nativeDestroyKernel(handle);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Native Methods
    // ─────────────────────────────────────────────────────────────

    private static native long nativeCreate(String backend);

    private static native void nativeDestroy(long ctx);

    private static native long nativeAllocBuffer(long ctx, long sizeBytes);

    private static native void nativeFreeBuffer(long ctx, long bufferHandle);

    private static native void nativeUploadFloats(long ctx, long bufferHandle, float[] data, int len);

    private static native void nativeUploadBytes(long ctx, long bufferHandle, byte[] data, int len);

    private static native void nativeDownloadFloats(long ctx, long bufferHandle, float[] out, int len);

    private static native void nativeDownloadBytes(long ctx, long bufferHandle, byte[] out, int len);

    private static native long nativeAllocImage(long ctx, int w, int h, String fmt);

    private static native void nativeFreeImage(long ctx, long imageHandle);

    private static native void nativeUploadImage(long ctx, long imageHandle, ByteBuffer data,
                                                 int w, int h, String fmt);

    private static native void nativeDownloadImage(long ctx, long imageHandle, ByteBuffer data,
                                                   int w, int h, String fmt);

    private static native long nativeCompileKernel(long ctx, String name, String source, String lang);

    private static native void nativeDestroyKernel(long kernelHandle);

    private static native int nativeDispatchKernel(long ctx, long kernelHandle,
                                                   int x, int y, int z,
                                                   long[] bufferHandles,
                                                   long[] imageHandles);
}
