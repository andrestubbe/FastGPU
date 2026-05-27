package fastgpu;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

final class FastGPUImageUtil {

    static ByteBuffer toByteBuffer(BufferedImage img, Format fmt) {
        int w = img.getWidth();
        int h = img.getHeight();
        BufferedImage argb = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = argb.createGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();

        ByteBuffer buf = ByteBuffer.allocateDirect(w * h * 4);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgba = argb.getRGB(x, y);
                buf.put((byte) ((rgba >> 16) & 0xFF)); // R
                buf.put((byte) ((rgba >> 8) & 0xFF));  // G
                buf.put((byte) (rgba & 0xFF));         // B
                buf.put((byte) ((rgba >> 24) & 0xFF)); // A
            }
        }
        buf.flip();
        return buf;
    }

    static void fromByteBufferInto(ByteBuffer buf, BufferedImage img, Format fmt) {
        int w = img.getWidth();
        int h = img.getHeight();
        int[] pixels = new int[w * h];
        byte[] raw = new byte[w * h * 4];
        
        buf.rewind();
        buf.get(raw); // Bulk JNI copy (extremely fast)
        
        for (int i = 0; i < w * h; i++) {
            int idx = i * 4;
            int r = raw[idx] & 0xFF;
            int g = raw[idx + 1] & 0xFF;
            int b = raw[idx + 2] & 0xFF;
            int a = raw[idx + 3] & 0xFF;
            pixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }
        img.setRGB(0, 0, w, h, pixels, 0, w);
    }

    private FastGPUImageUtil() {}
}
