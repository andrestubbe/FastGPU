package fastgpu;

import fastimage.FastImage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class MandelbrotDemo extends JPanel {

    private final FastGPU gpu;
    private final FastGPUKernel kernel;
    private final FastGPUImage gpuOutput;
    private final FastGPUBuffer paramBuffer;

    private BufferedImage currentFrame;
    private BufferedImage highResFrame;
    private float centerX = -0.5f;
    private float centerY = 0.0f;
    private float zoom = 1.0f;

    private int lastMouseX = 0;
    private int lastMouseY = 0;
    private boolean isMousePressed = false;
    private boolean isShiftPressed = false;
    private float targetZoomVel = 0.0f;
    private float zoomVel = 0.0f;
    private float panVelX = 0.0f;
    private float panVelY = 0.0f;

    // Quality transition state
    private volatile boolean isMoving = false;
    private volatile float fadeAlpha = 1.0f;
    private volatile boolean highResReady = false;
    private long transitionStartTime = 0;

    private static final int WIDTH = 1130;
    private static final int HEIGHT = 640;

    // Iterations: 64 during flight (fast), 128 at rest (quality + 4xSSAA)
    private static final int ITER_MOVING = 64;
    private static final int ITER_STILL  = 128;

    private long lastTime = System.nanoTime();
    private int frameCount = 0;
    private JFrame parentFrame;

    public MandelbrotDemo(JFrame frame) {
        this.parentFrame = frame;
        setPreferredSize(new Dimension(WIDTH, HEIGHT));

        gpu = FastGPU.openDefault();
        gpuOutput = gpu.allocImage(WIDTH, HEIGHT, Format.RGBA8);
        paramBuffer = gpu.allocFloatBuffer(4); // [cx, cy, zoom, max_iter]

        String glslSource = """
                #version 450
                layout(local_size_x = 16, local_size_y = 16) in;
                
                layout(std430, binding = 0) buffer Params {
                    float cx;
                    float cy;
                    float zoom;
                    float max_iter_f;
                };
                
                layout(rgba8, binding = 4) uniform writeonly image2D outputImg;
                
                void main() {
                    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);
                    ivec2 size = imageSize(outputImg);
                    if(pos.x >= size.x || pos.y >= size.y) return;
                    
                    int max_iter = int(max_iter_f);
                    bool doSSAA = (max_iter_f > 100.0); // 4xSSAA only at 128 iters (still mode)
                    float aspectZoom = 1.5 / zoom;
                    
                    const vec2 offsets[4] = vec2[4](
                        vec2(-0.25, -0.25),
                        vec2( 0.25, -0.25),
                        vec2(-0.25,  0.25),
                        vec2( 0.25,  0.25)
                    );
                    
                    int samples = doSSAA ? 4 : 1;
                    vec3 totalColor = vec3(0.0);
                    
                    for (int s = 0; s < samples; s++) {
                        vec2 jitter = doSSAA ? offsets[s] : vec2(0.0);
                        vec2 uv = (vec2(pos) + jitter + 0.5) / vec2(size) * 2.0 - 1.0;
                        uv.x *= float(size.x) / float(size.y);
                        
                        vec2 c = vec2(cx, cy) + uv * aspectZoom;
                        vec2 z = vec2(0.0);
                        int i;
                        for(i = 0; i < max_iter; i++) {
                            float x = (z.x * z.x - z.y * z.y) + c.x;
                            float y = (2.0 * z.x * z.y) + c.y;
                            z.x = x;
                            z.y = y;
                            if((x * x + y * y) > 4.0) break;
                        }
                        
                        vec3 color;
                        if(i == max_iter) {
                            color = vec3(1.0);
                        } else {
                            float smoothed = float(i) + 1.0 - log2(log2(z.x*z.x + z.y*z.y) / 2.0);
                            float intensity = smoothed / 128.0; // always normalize to 128 for consistent brightness
                            intensity = clamp(pow(intensity, 1.2) * 2.0, 0.0, 1.0);
                            color = vec3(intensity);
                        }
                        totalColor += color;
                    }
                    
                    imageStore(outputImg, pos, vec4(totalColor / float(samples), 1.0));
                }
                """;

        kernel = gpu.compile("mandelbrot", glslSource, KernelLanguage.GLSL_COMPUTE);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                isMousePressed = true;
                isShiftPressed = e.isShiftDown();
                lastMouseX = e.getX();
                lastMouseY = e.getY();
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                isMousePressed = false;
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                lastMouseX = e.getX();
                lastMouseY = e.getY();
            }
            @Override
            public void mouseDragged(MouseEvent e) {
                lastMouseX = e.getX();
                lastMouseY = e.getY();
            }
        });

        addMouseWheelListener(e -> {
            double rotation = e.getPreciseWheelRotation();
            if (rotation < 0) {
                zoomVel += 0.005f;
            } else if (rotation > 0) {
                zoomVel -= 0.005f;
            }
        });

        // Render Loop
        Thread renderThread = new Thread(() -> {
            while (true) {
                renderFrame();
                try {
                    Thread.sleep(4);
                } catch (InterruptedException ex) {
                    break;
                }
            }
        });
        renderThread.setDaemon(true);
        renderThread.start();
    }

    private void renderFrame() {
        // Input momentum
        if (isMousePressed) {
            targetZoomVel = isShiftPressed ? -0.01f : 0.01f;
            float dx = (lastMouseX - WIDTH / 2.0f) / (float) WIDTH;
            float dy = (lastMouseY - HEIGHT / 2.0f) / (float) HEIGHT;
            panVelX += dx * 0.002f;
            panVelY += dy * 0.002f;
        } else {
            targetZoomVel = 0.0f;
        }

        zoomVel += (targetZoomVel - zoomVel) * 0.05f;
        zoom *= (1.0f + zoomVel);
        panVelX *= 0.95f;
        panVelY *= 0.95f;
        centerX += panVelX * 3.0f / zoom * ((float) WIDTH / HEIGHT);
        centerY += panVelY * 3.0f / zoom;

        boolean nowMoving = (Math.abs(zoomVel) > 1e-5f || Math.abs(panVelX) > 1e-5f || Math.abs(panVelY) > 1e-5f || isMousePressed);

        // Transition: moving → stopped → freeze position, start hi-qual render
        if (isMoving && !nowMoving) {
            panVelX = 0.0f;
            panVelY = 0.0f;
            zoomVel = 0.0f;
            highResReady = false;
            fadeAlpha = 0.0f;
        }
        isMoving = nowMoving;

        // Advance crossfade only once first hi-res frame is ready
        if (!isMoving && highResReady && fadeAlpha < 1.0f) {
            float elapsed = (System.nanoTime() - transitionStartTime) / 1_000_000_000f;
            fadeAlpha = Math.min(1.0f, elapsed / 1.5f);
        }

        // Choose iteration count: fast during flight, full quality at rest
        float maxIter = isMoving ? ITER_MOVING : ITER_STILL;
        paramBuffer.upload(new float[]{centerX, centerY, zoom, maxIter});

        gpu.dispatch(
                kernel,
                DispatchSize.of2D((WIDTH + 15) / 16, (HEIGHT + 15) / 16),
                KernelArgs.of(paramBuffer, gpuOutput)
        );

        if (currentFrame == null) currentFrame = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_4BYTE_ABGR);
        gpuOutput.downloadInto(currentFrame);

        if (!isMoving) {
            // Copy into hi-res buffer for crossfade
            if (highResFrame == null) highResFrame = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_4BYTE_ABGR);
            gpuOutput.downloadInto(highResFrame);
            if (!highResReady) {
                highResReady = true;
                transitionStartTime = System.nanoTime();
                fadeAlpha = 0.0f;
            }
        }

        repaint();

        // FPS Counter
        frameCount++;
        long now = System.nanoTime();
        if (now - lastTime >= 1_000_000_000L) {
            final int fps = frameCount;
            SwingUtilities.invokeLater(() -> {
                if (parentFrame != null)
                    parentFrame.setTitle(String.format("FastGPU Real-Time Mandelbrot - %d FPS", fps));
            });
            frameCount = 0;
            lastTime = now;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        BufferedImage base = currentFrame;
        BufferedImage hi   = highResFrame;
        float alpha        = fadeAlpha;

        if (base != null) {
            g2d.drawImage(base, 0, 0, getWidth(), getHeight(), null);
        }

        // Crossfade hi-res (128 iter + SSAA) over lo-qual (64 iter) when stopping
        if (!isMoving && hi != null && highResReady && alpha < 1.0f) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2d.drawImage(hi, 0, 0, getWidth(), getHeight(), null);
            repaint();
        } else if (!isMoving && hi != null && highResReady) {
            g2d.drawImage(hi, 0, 0, getWidth(), getHeight(), null);
        }

        g2d.dispose();
    }

    public static void main(String[] args) {
        System.setProperty("sun.java2d.uiScale", "1.0");
        System.setProperty("sun.java2d.opengl", "true");
        System.out.println("Starting FastGPU Mandelbrot Demo...");
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("FastGPU Real-Time Mandelbrot");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            MandelbrotDemo demo = new MandelbrotDemo(frame);
            frame.add(demo);
            frame.pack();
            frame.setLocationRelativeTo(null);

            long hwnd = fasttheme.FastTheme.getWindowHandle(frame);
            fasttheme.FastTheme.setTitleBarDarkMode(hwnd, true);
            fasttheme.FastTheme.setTitleBarColor(hwnd, 0, 0, 0);
            fasttheme.FastTheme.setTitleBarTextColor(hwnd, 255, 255, 255);

            frame.setVisible(true);
        });
    }
}
