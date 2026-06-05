package fastgpu;

import fastimage.FastImage;
import java.awt.image.BufferedImage;

/**
 * Bridge-Layer between CPU-bound FastImage and GPU-bound FastGPU.
 * 
 * Enables zero-coupling data transfers between the SIMD image engine
 * and the Compute GPU engine.
 */
public final class FastImageGPU {

    private FastImageGPU() {
        // static utility class
    }

    /**
     * Uploads a CPU-bound FastImage to the GPU.
     * 
     * @param gpu FastGPU context
     * @param img FastImage to upload
     * @return FastGPUImage representing the uploaded data on the GPU
     */
    public static FastGPUImage upload(FastGPU gpu, FastImage img) {
        BufferedImage bi = img.toBufferedImage();
        FastGPUImage gpuImg = gpu.allocImage(bi.getWidth(), bi.getHeight(), Format.RGBA8);
        gpuImg.upload(bi);
        return gpuImg;
    }

    /**
     * Downloads a GPU-bound FastGPUImage back into a CPU-bound FastImage.
     * 
     * @param gpuImg FastGPUImage to download
     * @return FastImage containing the downloaded data
     */
    public static FastImage download(FastGPUImage gpuImg) {
        BufferedImage bi = gpuImg.download();
        return FastImage.fromBufferedImage(bi);
    }
}
