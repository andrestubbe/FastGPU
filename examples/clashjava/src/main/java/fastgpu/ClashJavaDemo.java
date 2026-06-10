package fastgpu;

import fasttheme.FastTheme;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ClashJavaDemo extends JPanel implements ActionListener {

    private static final int WIDTH = 1200;
    private static final int HEIGHT = 800;
    private static final int NUM_CIRCLES = 100;
    private static final int FPS_TARGET = 60;

    private final Scene scene;
    private final Timer timer;
    private int frames;
    private long lastFpsTime;
    private int fps;
    private int maxSteps;

    public ClashJavaDemo() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        scene = new Scene(WIDTH, HEIGHT);
        timer = new Timer(1000 / FPS_TARGET, this);
        timer.start();
        lastFpsTime = System.nanoTime();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        scene.render(g2);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Consolas", Font.PLAIN, 14));
        g2.drawString("Clash Java Demo", 16, 24);
        g2.drawString("Objects: " + scene.numObjects(), 16, 44);
        g2.drawString("Tests: " + scene.numTests(), 16, 64);
        g2.drawString("Max steps: " + maxSteps, 16, 84);
        g2.drawString("Energy: " + String.format("%.10f", scene.kineticEnergy()), 16, 104);
        g2.drawString("FPS: " + fps, 16, 124);
        g2.dispose();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        scene.step(1.0f / FPS_TARGET);
        repaint();
        frames++;
        long now = System.nanoTime();
        if (now - lastFpsTime >= 1_000_000_000L) {
            fps = frames;
            frames = 0;
            maxSteps = scene.getResetMaxSteps();
            lastFpsTime = now;
        }
    }

    public static void main(String[] args) {
        System.setProperty("sun.java2d.opengl", "true");
        System.setProperty("sun.java2d.uiScale", "1.0");
        if (args != null && args.length > 0 && "smoketest".equals(args[0])) {
            runSmokeTest(5);
            return;
        }

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Clash Java Demo");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.add(new ClashJavaDemo());
            frame.pack();
            frame.setLocationRelativeTo(null);

            long hwnd = FastTheme.getWindowHandle(frame);
            FastTheme.setTitleBarDarkMode(hwnd, true);
            FastTheme.setTitleBarColor(hwnd, 0, 0, 0);
            FastTheme.setTitleBarTextColor(hwnd, 255, 255, 255);

            frame.setVisible(true);
        });
    }

    private static void runSmokeTest(int seconds) {
        Scene scene = new Scene(WIDTH, HEIGHT);
        int frames = 0;
        long start = System.nanoTime();
        long lastPrint = start;
        float dt = 1.0f / FPS_TARGET;
        long end = start + seconds * 1_000_000_000L;
        while (System.nanoTime() < end) {
            scene.step(dt);
            frames++;
            long now = System.nanoTime();
            if (now - lastPrint >= 1_000_000_000L) {
                int outside = 0;
                for (MovingCircle c : scene.movingCircles) {
                    if (c.x < 0f || c.x > scene.width || c.y < 0f || c.y > scene.height) outside++;
                }
                System.out.printf("SmokeTest: frames=%d energy=%.6f maxSteps=%d outside=%d%n", frames, scene.kineticEnergy(), scene.getResetMaxSteps(), outside);
                frames = 0;
                lastPrint = now;
            }
        }
    }

    private static final class Scene {
        private static final float MIN_TIME_THRESHOLD = 1e-6f;
        private static final int MAX_STEPS = 10000;

        private final int width;
        private final int height;
        private final List<MovingCircle> movingCircles = new ArrayList<>();
        private final List<SceneObject> fixedObjects = new ArrayList<>();
        private final List<ContactPair> testPairs = new ArrayList<>();
        private boolean needsCompile = false;
        private int maxIterations;

        Scene(int width, int height) {
            this.width = width;
            this.height = height;

            Random random = new Random(12345);
            for (int i = 0; i < NUM_CIRCLES; i++) {
                int radius = random.nextInt(28) + 4;
                float x = random.nextInt(width - radius * 2) + radius;
                float y = random.nextInt(height - radius * 2) + radius;
                float vx = (float) ((random.nextDouble() * 0.5 - 0.25) * 1000.0);
                float vy = (float) ((random.nextDouble() * 0.5 - 0.25) * 1000.0);
                movingCircles.add(new MovingCircle(x, y, radius, vx, vy));
            }

            addFrame(0, 0, width, height);
            addFixedPolygon(new float[]{300, 700, 600, 240}, new float[]{100, 200, 500, 400});
            fixedObjects.add(new FixedCircle(800, 400, 64));
            needsCompile = true;
        }

        void step(float dt) {
            if (needsCompile) {
                compile();
            }

            float remaining = dt;
            int steps = 0;
            while (remaining > 1e-7f) {
                Contact contact = nextContact(new Contact(remaining, null, null));
                if (contact.when >= remaining) {
                    integrate(remaining);
                    break;
                }
                integrate(contact.when);
                contact.repel();
                remaining -= contact.when;
                if (++steps > MAX_STEPS) {
                    throw new IllegalStateException("Solver took too long");
                }
            }
            maxIterations = Math.max(maxIterations, steps);
        }

        void render(Graphics2D g2) {
            g2.setColor(new Color(0x08, 0x08, 0x08));
            g2.fillRect(0, 0, width, height);

            g2.setColor(new Color(0xBD, 0xC2, 0xAD));
            for (MovingCircle circle : movingCircles) {
                circle.render(g2);
            }

            g2.setStroke(new BasicStroke(1.0f));
            g2.setColor(new Color(0xF9, 0xD2, 0x53));
            for (SceneObject fixedObject : fixedObjects) {
                fixedObject.wireframe(g2);
            }
        }

        private void addFrame(int x0, int y0, int x1, int y1) {
            // frame walls should be solid (not one-way gates) to keep circles inside
            fixedObjects.add(new FixedLine(x0, y0, x1, y0, false));
            fixedObjects.add(new FixedLine(x1, y0, x1, y1, false));
            fixedObjects.add(new FixedLine(x1, y1, x0, y1, false));
            fixedObjects.add(new FixedLine(x0, y1, x0, y0, false));
        }

        private void addFixedPolygon(float[] xPoints, float[] yPoints) {
            for (int i = 0; i < xPoints.length; i++) {
                int next = (i + 1) % xPoints.length;
                fixedObjects.add(new FixedPoint(xPoints[i], yPoints[i]));
                fixedObjects.add(new FixedLine(xPoints[i], yPoints[i], xPoints[next], yPoints[next], false));
            }
        }

        private void compile() {
            testPairs.clear();
            for (int i = 0; i < movingCircles.size(); i++) {
                MovingCircle moving = movingCircles.get(i);
                for (int j = i + 1; j < movingCircles.size(); j++) {
                    testPairs.add(new ContactPair(moving, movingCircles.get(j)));
                }
                for (SceneObject fixedObject : fixedObjects) {
                    testPairs.add(new ContactPair(moving, fixedObject));
                }
            }
            needsCompile = false;
        }

        int numTests() {
            return testPairs.size();
        }

        int numObjects() {
            return movingCircles.size() + fixedObjects.size();
        }

        int getResetMaxSteps() {
            int result = maxIterations;
            maxIterations = 0;
            return result;
        }

        float kineticEnergy() {
            float energy = 0f;
            for (MovingCircle circle : movingCircles) {
                float squared = circle.vx * circle.vx + circle.vy * circle.vy;
                if (squared != 0f) {
                    energy += squared * circle.mass;
                }
            }
            return energy * 0.5f;
        }

        private Contact nextContact(Contact contact) {
            Contact nearest = contact;
            for (ContactPair pair : testPairs) {
                nearest = pair.other.proximate(nearest, pair.moving);
            }
            return nearest;
        }

        private void integrate(float time) {
            for (MovingCircle circle : movingCircles) {
                circle.move(time);
            }
        }

        private interface SceneObject {
            Contact proximate(Contact nearest, MovingCircle moving);
            void repelMovingObject(MovingCircle moving);
            void wireframe(Graphics2D g2);
        }

        private static final class Contact {
            private final float when;
            private final MovingCircle moving;
            private final SceneObject other;
            private static final Contact NEVER = new Contact(Float.POSITIVE_INFINITY, null, null);

            Contact(float when, MovingCircle moving, SceneObject other) {
                this.when = when;
                this.moving = moving;
                this.other = other;
            }

            static Contact compare(Contact nearest, float when, MovingCircle moving, SceneObject other) {
                return when >= MIN_TIME_THRESHOLD && when < nearest.when ? new Contact(when, moving, other) : nearest;
            }

            static Contact proximate(Contact current, Contact other) {
                if (other == NEVER) {
                    return current;
                }
                if (current == NEVER) {
                    return other;
                }
                return other.when < current.when ? other : current;
            }

            void repel() {
                other.repelMovingObject(moving);
            }
        }

        private static final class ContactPair {
            private final MovingCircle moving;
            private final SceneObject other;

            ContactPair(MovingCircle moving, SceneObject other) {
                this.moving = moving;
                this.other = other;
            }
        }

        private static final class MovingCircle implements SceneObject {
            private final int radius;
            private final Color color;
            private final float mass;
            private final float inverseMass;
            private float x;
            private float y;
            private float vx;
            private float vy;

            MovingCircle(float x, float y, int radius, float vx, float vy) {
                this.x = x;
                this.y = y;
                this.radius = radius;
                this.vx = vx;
                this.vy = vy;
                this.color = new Color(0xBD, 0xC2, 0xAD);
                this.mass = radius * radius;
                this.inverseMass = 1.0f / this.mass;
            }

            @Override
            public Contact proximate(Contact nearest, MovingCircle moving) {
                return moving.proximateMovingCircle(nearest, this);
            }

            private Contact proximateMovingCircle(Contact nearest, MovingCircle other) {
                float vx = other.vx - this.vx;
                float vy = other.vy - this.vy;
                float vs = vx * vx + vy * vy;
                if (vs == 0f) {
                    return nearest;
                }

                float ex = this.x - other.x;
                float ey = this.y - other.y;
                float ev = ex * vy - ey * vx;
                float rr = this.radius + other.radius;
                float sq = vs * rr * rr - ev * ev;
                if (sq < 0f) {
                    return nearest;
                }

                float when = -((float) Math.sqrt(sq) - ey * vy - ex * vx) / vs;
                return Contact.compare(nearest, when, this, other);
            }

            private void repelMovingCircle(MovingCircle other) {
                float distance = this.radius + other.radius;
                if (distance == 0f) {
                    return;
                }

                float nx = (this.x - other.x) / distance;
                float ny = (this.y - other.y) / distance;
                float relative = (this.vx - other.vx) * nx + (this.vy - other.vy) * ny;
                float e = 2.0f * relative / (this.inverseMass + other.inverseMass);
                float ex = nx * e;
                float ey = ny * e;

                this.vx -= ex * this.inverseMass;
                this.vy -= ey * this.inverseMass;
                other.vx += ex * other.inverseMass;
                other.vy += ey * other.inverseMass;
            }

            @Override
            public void repelMovingObject(MovingCircle moving) {
                repelMovingCircle(moving);
            }

            @Override
            public void wireframe(Graphics2D g2) {
                g2.fillOval(Math.round(x - radius), Math.round(y - radius), radius * 2, radius * 2);
            }

            void move(float dt) {
                x += vx * dt;
                y += vy * dt;
            }

            void render(Graphics2D g2) {
                g2.setColor(color);
                wireframe(g2);
            }
        }

        private static final class FixedLine implements SceneObject {
            private final float x0;
            private final float y0;
            private final float x1;
            private final float y1;
            private final boolean gate;

            FixedLine(float x0, float y0, float x1, float y1, boolean gate) {
                this.x0 = x0;
                this.y0 = y0;
                this.x1 = x1;
                this.y1 = y1;
                this.gate = gate;
            }

            @Override
            public Contact proximate(Contact nearest, MovingCircle moving) {
                float dx = x1 - x0;
                float dy = y1 - y0;
                float ud = moving.vy * dx - moving.vx * dy;
                if (gate && ud <= 0f) {
                    return nearest;
                }
                if (ud == 0f) {
                    return nearest;
                }

                float dd = (float) Math.hypot(dx, dy) * Math.signum(ud);
                float px = (moving.x - x0) - dy / dd * moving.radius;
                float py = (moving.y - y0) + dx / dd * moving.radius;
                float ua = (moving.vy * px - moving.vx * py) / ud;
                if (ua < 0f || ua > 1f) {
                    return nearest;
                }

                float when = (dy * px - dx * py) / ud;
                return Contact.compare(nearest, when, moving, this);
            }

            @Override
            public void repelMovingObject(MovingCircle moving) {
                float dx = x1 - x0;
                float dy = y1 - y0;
                float dd = (float) Math.hypot(dx, dy);
                if (dd == 0f) {
                    return;
                }

                float nx = dy / dd;
                float ny = -dx / dd;
                float e = 2.0f * (nx * moving.vx + ny * moving.vy);
                moving.vx -= nx * e;
                moving.vy -= ny * e;
            }

            @Override
            public void wireframe(Graphics2D g2) {
                g2.drawLine(Math.round(x0), Math.round(y0), Math.round(x1), Math.round(y1));
            }
        }

        private static final class FixedPoint implements SceneObject {
            private final float x;
            private final float y;

            FixedPoint(float x, float y) {
                this.x = x;
                this.y = y;
            }

            @Override
            public Contact proximate(Contact nearest, MovingCircle moving) {
                float dx = x - moving.x;
                float dy = y - moving.y;
                float vx = moving.vx;
                float vy = moving.vy;
                float vs = vx * vx + vy * vy;
                if (vs == 0f) {
                    return nearest;
                }

                float ev = dx * vy - dy * vx;
                float sq = vs * moving.radius * moving.radius - ev * ev;
                if (sq < 0f) {
                    return nearest;
                }

                float when = -(float) ((Math.sqrt(sq) - dy * vy - dx * vx) / vs);
                return Contact.compare(nearest, when, moving, this);
            }

            @Override
            public void repelMovingObject(MovingCircle moving) {
                float dx = moving.x - x;
                float dy = moving.y - y;
                float dd = (float) Math.hypot(dx, dy);
                if (dd == 0f) {
                    return;
                }

                float nx = dx / dd;
                float ny = dy / dd;
                float e = 2.0f * (nx * moving.vx + ny * moving.vy);
                moving.vx -= nx * e;
                moving.vy -= ny * e;
            }

            @Override
            public void wireframe(Graphics2D g2) {
            }
        }

        private static final class FixedCircle implements SceneObject {
            private final float cx;
            private final float cy;
            private final float radius;

            FixedCircle(float cx, float cy, float radius) {
                this.cx = cx;
                this.cy = cy;
                this.radius = radius;
            }

            @Override
            public Contact proximate(Contact nearest, MovingCircle moving) {
                return Contact.proximate(proximateMovingCircleSigned(nearest, moving, 1), proximateMovingCircleSigned(nearest, moving, -1));
            }

            private Contact proximateMovingCircleSigned(Contact nearest, MovingCircle moving, int sign) {
                float dx = cx - moving.x;
                float dy = cy - moving.y;
                float rr = moving.radius + radius * sign;
                float vx = moving.vx;
                float vy = moving.vy;
                float vs = vx * vx + vy * vy;
                if (vs == 0f) {
                    return nearest;
                }

                float ev = dx * vy - dy * vx;
                float sq = vs * rr * rr - ev * ev;
                if (sq < 0f) {
                    return nearest;
                }

                float when = (-((float) Math.sqrt(sq) * sign) + dy * vy + dx * vx) / vs;
                return Contact.compare(nearest, when, moving, this);
            }

            @Override
            public void repelMovingObject(MovingCircle moving) {
                float dx = moving.x - cx;
                float dy = moving.y - cy;
                float dd = (float) Math.hypot(dx, dy);
                if (dd == 0f) {
                    return;
                }

                float nx = dx / dd;
                float ny = dy / dd;
                float e = 2.0f * (nx * moving.vx + ny * moving.vy);
                moving.vx -= nx * e;
                moving.vy -= ny * e;
            }

            @Override
            public void wireframe(Graphics2D g2) {
                g2.drawOval(Math.round(cx - radius), Math.round(cy - radius), Math.round(radius * 2), Math.round(radius * 2));
            }
        }
    }
}
