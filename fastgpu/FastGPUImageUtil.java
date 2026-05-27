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

    static BufferedImage fromByteBuffer(ByteBuffer buf, int w, int h, Format fmt) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int r = buf.get() & 0xFF;
                int g = buf.get() & 0xFF;
                int b = buf.get() & 0xFF;
                int a = buf.get() & 0xFF;
                int argb = (a << 24) | (r << 16) | (g << 8) | b;
                img.setRGB(x, y, argb);
            }
        }
        return img;
    }

    private FastGPUImageUtil() {}
}
