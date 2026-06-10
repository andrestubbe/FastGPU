package fastgpu;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class FluidDemo extends JPanel {

    private static final int WIDTH = 1920;
    private static final int HEIGHT = 1080;
    private final FastGPU gpu;
    private final FastGPUKernel kernel;
    private final FastGPUImage gpuOutput;
    private final FastGPUBuffer paramBuffer;
    private final BufferedImage currentFrame;
    private long startTime;
    private int frameCount;
    private long lastFpsTime;
    private JFrame parentFrame;

    public FluidDemo(JFrame frame) {
        this.parentFrame = frame;
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);

        gpu = FastGPU.openDefault();
        gpuOutput = gpu.allocImage(WIDTH, HEIGHT, Format.RGBA8);
        paramBuffer = gpu.allocFloatBuffer(4);
        currentFrame = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_4BYTE_ABGR);

        String glslSource = """
                #version 450
                layout(local_size_x = 16, local_size_y = 16) in;

                layout(std430, binding = 0) buffer Params {
                    float time;
                    float aspect;
                    float padding0;
                    float padding1;
                };

                layout(rgba8, binding = 4) uniform writeonly image2D outputImg;

                vec3 palette(float x) {
                    const float PI = 3.14159265;
                    return vec3(
                        0.5 + 0.5 * cos(PI * (x + 0.00)),
                        0.5 + 0.5 * cos(PI * (x + 0.33)),
                        0.5 + 0.5 * cos(PI * (x + 0.66))
                    );
                }

                void main() {
                    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);
                    ivec2 size = imageSize(outputImg);
                    if (pos.x >= size.x || pos.y >= size.y) return;

                    vec2 uv = (vec2(pos) / vec2(size)) * 2.0 - 1.0;
                    uv.x *= aspect;
                    float t = time * 0.7;

                    float r = length(uv);
                    float a = atan(uv.y, uv.x);
                    float pulse = sin(r * 8.0 - t * 5.0) * 0.5 + 0.5;
                    float twist = sin(a * 3.0 + t * 2.0) * 0.5 + 0.5;
                    float waves = sin(uv.x * 5.0 + t) * 0.5 + 0.5;

                    float q = pulse * twist * waves;
                    vec3 col = palette(q);
                    col *= 1.0 - smoothstep(0.8, 1.0, r);

                    imageStore(outputImg, pos, vec4(col, 1.0));
                }
                """;

        kernel = gpu.compile("fluid", glslSource, KernelLanguage.GLSL_COMPUTE);
        startTime = System.nanoTime();
        lastFpsTime = startTime;

        Thread renderThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                renderFrame();
                try {
                    Thread.sleep(8);
                } catch (InterruptedException ignored) {
                    break;
                }
            }
        }, "FastGPU-FluidDemo");
        renderThread.setDaemon(true);
        renderThread.start();
    }

    private void renderFrame() {
        float time = (System.nanoTime() - startTime) / 1_000_000_000f;
        paramBuffer.upload(new float[]{time, (float) HEIGHT / WIDTH, 0f, 0f});

        gpu.dispatch(
                kernel,
                DispatchSize.of2D((WIDTH + 15) / 16, (HEIGHT + 15) / 16),
                KernelArgs.of(paramBuffer, gpuOutput)
        );

        gpuOutput.downloadInto(currentFrame);
        repaint();
        updateFps();
    }

    private void updateFps() {
        frameCount++;
        long now = System.nanoTime();
        if (now - lastFpsTime >= 1_000_000_000L) {
            int fps = frameCount;
            frameCount = 0;
            lastFpsTime = now;
            SwingUtilities.invokeLater(() -> {
                if (parentFrame != null) {
                    parentFrame.setTitle(String.format("FastGPU Fluid Demo - %d FPS", fps));
                }
            });
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(currentFrame, 0, 0, getWidth(), getHeight(), null);
    }

    public static void main(String[] args) {
        System.setProperty("sun.java2d.uiScale", "1.0");
        System.setProperty("sun.java2d.opengl", "true");
        System.out.println("Starting FastGPU Fluid Demo...");
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("FastGPU Fluid Demo");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            FluidDemo demo = new FluidDemo(frame);
            frame.add(demo);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
