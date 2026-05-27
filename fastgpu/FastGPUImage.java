package fastgpu;

import java.awt.image.BufferedImage;

public interface FastGPUImage {

    int width();

    int height();

    Format format();

    void upload(BufferedImage img);

    BufferedImage download();

    void free();
}
