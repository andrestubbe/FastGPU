package fastgpu;

import fastcore.FastCore;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.Objects;

final class FastGPUImpl implements FastGPU {

    static {
        FastCore.loadNative("fastgpu"); // erwartet fastgpu-native
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
        long[] bufferHandles = new long[flatArgs.length];
        for (int i = 0; i < flatArgs.length; i++) {
            Object o = flatArgs[i];
            if (o instanceof BufferImpl b) {
                bufferHandles[i] = b.handle;
            } else if (o instanceof ImageImpl img) {
                bufferHandles[i] = img.handle;
            } else {
                throw new IllegalArgumentException("Unsupported kernel arg type: " + o);
            }
        }

        int result = nativeDispatchKernel(
                nativeHandle,
                k.handle,
                size.x(), size.y(), size.z(),
                bufferHandles
        );
        if (result != 0) {
            throw new FastGPUException("Kernel dispatch failed with code: " + result);
        }
    }

    @Override
    public void close() {
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
            ByteBuffer buf = ByteBuffer.allocateDirect(width * height * 4); // RGBA8 baseline
            nativeDownloadImage(nativeHandle, handle, buf, width, height, format.name());
            return FastGPUImageUtil.fromByteBuffer(buf, width, height, format);
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
                                                   long[] bufferHandles);
}
