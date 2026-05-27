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
    private float centerX = -0.5f;
    private float centerY = 0.0f;
    private float zoom = 1.0f;

    private boolean isDragging = false;
    private int lastMouseX = 0;
    private int lastMouseY = 0;
    private boolean isMousePressed = false;
    private boolean isShiftPressed = false;
    private float targetZoomVel = 0.0f;
    private float zoomVel = 0.0f;
    
    private float panVelX = 0.0f;
    private float panVelY = 0.0f;
    private float currentMaxIter = 128.0f;

    private static final int WIDTH = 1130;
    private static final int HEIGHT = 640;

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
                    float dyn_max_iter;
                };
                
                layout(rgba8, binding = 4) uniform writeonly image2D outputImg;
                
                void main() {
                    ivec2 pos = ivec2(gl_GlobalInvocationID.xy);
                    ivec2 size = imageSize(outputImg);
                    if(pos.x >= size.x || pos.y >= size.y) return;
                    
                    vec3 totalColor = vec3(0.0);
                    float aspectZoom = 1.5 / zoom;
                    int max_iter = 128;
                    
                    vec2 uv = vec2(pos) / vec2(size) * 2.0 - 1.0;
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
                        color = vec3(1.0); // Inside is white
                    } else {
                        // Continuous smooth iteration count
                        float smoothed = float(i) + 1.0 - log2(log2(z.x*z.x + z.y*z.y) / 2.0);
                        float intensity = smoothed / float(max_iter);
                        intensity = clamp(pow(intensity, 1.2) * 2.0, 0.0, 1.0); 
                        color = vec3(intensity);
                    }
                    
                    imageStore(outputImg, pos, vec4(color, 1.0));
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

        // Render Loop (Uncapped / High FPS)
        Thread renderThread = new Thread(() -> {
            while (true) {
                renderFrame();
                try {
                    Thread.sleep(4); // target roughly 250 FPS logical cap, rendering is GPU limited
                } catch (InterruptedException ex) {
                    break;
                }
            }
        });
        renderThread.setDaemon(true);
        renderThread.start();
    }

    private void renderFrame() {
        // Handle input momentum
        if (isMousePressed) {
            targetZoomVel = isShiftPressed ? -0.01f : 0.01f;
            
            // Steer towards mouse position
            float dx = (lastMouseX - WIDTH / 2.0f) / (float) WIDTH;
            float dy = (lastMouseY - HEIGHT / 2.0f) / (float) HEIGHT;
            
            // Add momentum in the direction of the mouse
            panVelX += dx * 0.002f;
            panVelY += dy * 0.002f;
            
        } else {
            targetZoomVel = 0.0f;
        }
        
        // Accelerate / Decelerate zoom
        zoomVel += (targetZoomVel - zoomVel) * 0.05f;
        zoom *= (1.0f + zoomVel);
        
        // Decelerate pan smoothly
        panVelX *= 0.95f;
        panVelY *= 0.95f;
        
        centerX += panVelX * 3.0f / zoom * ((float)WIDTH/HEIGHT);
        centerY += panVelY * 3.0f / zoom;
        
        paramBuffer.upload(new float[]{centerX, centerY, zoom, 128.0f});

        gpu.dispatch(
                kernel,
                DispatchSize.of2D((WIDTH + 15) / 16, (HEIGHT + 15) / 16),
                KernelArgs.of(paramBuffer, gpuOutput)
        );

        if (currentFrame == null) {
            currentFrame = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_4BYTE_ABGR);
        }
        gpuOutput.downloadInto(currentFrame);
        repaint();

        // FPS Counter
        frameCount++;
        long now = System.nanoTime();
        if (now - lastTime >= 1_000_000_000L) {
            final int currentFps = frameCount;
            SwingUtilities.invokeLater(() -> {
                if (parentFrame != null) {
                    parentFrame.setTitle(String.format("FastGPU Real-Time Mandelbrot - %d FPS", currentFps));
                }
            });
            frameCount = 0;
            lastTime = now;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (currentFrame != null) {
            g.drawImage(currentFrame, 0, 0, this);
        }
    }

    public static void main(String[] args) {
        System.setProperty("sun.java2d.uiScale", "1.0");
        System.setProperty("sun.java2d.opengl", "true"); // Enable hardware acceleration for Java2D
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
