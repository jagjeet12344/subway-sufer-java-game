import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import javax.swing.*;

public class SubwaySurfers extends JPanel implements ActionListener, KeyListener {

    static final int W = 700, H = 800;
    static final int[] LANE_X = {80, 260, 440};
    static final int TRACK_TOP = H / 2;

    int playerLane = 1;
    float playerX = LANE_X[1];
    float playerY = 600;
    int playerW = 70, playerH = 90;

    boolean jumping = false;
    boolean sliding = false;
    int slideTimer = 0;
    float jumpVel = 0;
    float jumpOffset = 0;
    float playerTilt = 0;

    int worldSpeed = 10;
    int score = 0;
    int highScore = 0;
    int nextSpeedScore = 150;

    int roadOffset = 0;
    int frameCount = 0;

    boolean gameStarted = false;
    boolean gameRunning = true;

    ArrayList<Obstacle> obstacles = new ArrayList<>();
    ArrayList<Coin> coins = new ArrayList<>();

    Random random = new Random();
    Timer timer;

    static class Obstacle {
        float x, y;
        int w, h, type;

        Obstacle(float x, float y, int w, int h, int type) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.type = type;
        }

        Rectangle getRect() {
            return new Rectangle((int)x, (int)y, w, h);
        }
    }

    static class Coin {
        float x, y, spin;
        int size;

        Coin(float x, float y, int size) {
            this.x = x;
            this.y = y;
            this.size = size;
        }
    }

    public SubwaySurfers() {
        setPreferredSize(new Dimension(W, H));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        timer = new Timer(16, this);
        timer.start();
    }

    void resetGame() {
        obstacles.clear();
        coins.clear();

        score = 0;
        worldSpeed = 10;
        nextSpeedScore = 150;

        playerLane = 1;
        playerX = LANE_X[1];
        playerY = 600;

        jumping = false;
        sliding = false;
        jumpVel = 0;
        jumpOffset = 0;
        slideTimer = 0;
        playerTilt = 0;

        frameCount = 0;
        roadOffset = 0;

        gameStarted = true;
        gameRunning = true;

        requestFocusInWindow();
    }

    void spawnObstacle() {
        int lane = random.nextInt(3);
        int type = random.nextInt(100);

        if (type < 45) {
            obstacles.add(new Obstacle(
                LANE_X[lane] + 30, -80, 60, 60, 0
            ));
        } else if (type < 75) {
            obstacles.add(new Obstacle(
                LANE_X[lane] + 20, -120, 80, 100, 1
            ));
        } else {
            obstacles.add(new Obstacle(
                LANE_X[lane] + 10, -250, 100, 200, 2
            ));
        }
    }

    void spawnCoin() {
        int lane = random.nextInt(3);
        int count = 3 + random.nextInt(3);

        for (int i = 0; i < count; i++) {
            coins.add(new Coin(
                LANE_X[lane] + 55,
                -60 - i * 60,
                30
            ));
        }
    }

    Rectangle getPlayerRect() {
        if (sliding) {
            return new Rectangle(
                (int)playerX + 8,
                (int)playerY + playerH / 2,
                playerW - 16,
                playerH / 2
            );
        }

        int y = (int)(playerY + jumpOffset);

        return new Rectangle(
            (int)playerX + 12,
            y + 8,
            playerW - 24,
            playerH - 16
        );
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        );

        drawSky(g2);
        drawTrack(g2);

        for (Coin c : coins) {
            drawCoin(g2, c);
        }

        for (Obstacle o : obstacles) {
            drawObstacle(g2, o);
        }

        if (gameRunning || !gameStarted) {
            drawPlayer(g2);
        }

        drawHUD(g2);

        if (!gameStarted) {
            drawStartScreen(g2);
        } else if (!gameRunning) {
            drawGameOver(g2);
        }
    }

    void drawSky(Graphics2D g) {
        GradientPaint sky = new GradientPaint(
            0, 0, new Color(255, 130, 80),
            0, TRACK_TOP, new Color(120, 40, 130)
        );

        g.setPaint(sky);
        g.fillRect(0, 0, W, TRACK_TOP);

        g.setColor(new Color(255, 230, 120, 40));
        g.fillOval(470, 40, 220, 220);

        g.setColor(new Color(255, 240, 150));
        g.fillOval(520, 90, 120, 120);

        g.setColor(new Color(255, 255, 255, 40));

        for (int i = 0; i < 3; i++) {
            int x = (int)((i * 250 + frameCount * 0.3f) % (W + 200)) - 100;
            g.fillOval(x, 60 + i * 30, 140, 40);
        }
    }

    void drawTrack(Graphics2D g) {
        GradientPaint ground = new GradientPaint(
            0, TRACK_TOP, new Color(70, 70, 90),
            0, H, new Color(15, 15, 25)
        );

        g.setPaint(ground);
        g.fillRect(0, TRACK_TOP, W, H - TRACK_TOP);

        g.setColor(new Color(100, 100, 115));
        g.fillRect(40, TRACK_TOP, 10, H - TRACK_TOP);
        g.fillRect(W - 50, TRACK_TOP, 10, H - TRACK_TOP);

        g.setStroke(new BasicStroke(3));
        g.setColor(new Color(220, 220, 230, 90));

        for (int i = 0; i < 2; i++) {
            int x = LANE_X[i] + 180;

            for (int y = TRACK_TOP - 40 + roadOffset; y < H; y += 40) {
                g.drawLine(x, y, x, y + 20);
            }
        }
    }

    void drawObstacle(Graphics2D g, Obstacle o) {
        if (o.type == 2) {
            GradientPaint grad = new GradientPaint(
                o.x, o.y, new Color(70, 130, 230),
                o.x, o.y + o.h, new Color(20, 50, 130)
            );

            g.setPaint(grad);
            g.fillRoundRect((int)o.x, (int)o.y, o.w, o.h, 15, 15);

            g.setColor(new Color(160, 220, 255));

            for (int i = 0; i < 3; i++) {
                g.fillRoundRect(
                    (int)o.x + 10,
                    (int)o.y + 15 + i * 60,
                    o.w - 20, 35, 8, 8
                );
            }

            g.setColor(Color.YELLOW);
            g.fillOval((int)o.x + 8, (int)o.y + o.h - 18, 14, 14);
            g.fillOval((int)o.x + o.w - 22, (int)o.y + o.h - 18, 14, 14);

        } else {
            Color c1 = o.type == 0
                ? new Color(230, 90, 90)
                : new Color(240, 140, 60);

            Color c2 = o.type == 0
                ? new Color(140, 20, 20)
                : new Color(150, 60, 10);

            GradientPaint grad = new GradientPaint(
                o.x, o.y, c1, o.x, o.y + o.h, c2
            );

            g.setPaint(grad);
            g.fillRoundRect((int)o.x, (int)o.y, o.w, o.h, 12, 12);

            g.setColor(new Color(255, 240, 200, 130));
            g.fillRoundRect(
                (int)o.x + 6, (int)o.y + 6,
                o.w - 12, 12, 6, 6
            );

            g.setColor(Color.BLACK);
            g.drawRoundRect((int)o.x, (int)o.y, o.w, o.h, 12, 12);

            g.setColor(Color.YELLOW);
            g.setFont(new Font("Arial", Font.BOLD, 22));
            g.drawString("!", (int)o.x + o.w / 2 - 5,
                (int)o.y + o.h / 2 + 8);
        }
    }

    void drawCoin(Graphics2D g, Coin c) {
        int cx = (int)c.x + c.size / 2;
        int cy = (int)c.y + c.size / 2;

        int width = (int)(Math.abs(Math.cos(c.spin)) * c.size) + 6;
        width = Math.max(width, 6);

        g.setColor(new Color(255, 210, 50, 70));
        g.fillOval(cx - 19, cy - 19, 38, 38);

        GradientPaint grad = new GradientPaint(
            cx - width / 2f, cy, Color.YELLOW,
            cx + width / 2f, cy, new Color(210, 130, 0)
        );

        g.setPaint(grad);
        g.fillOval(cx - width / 2, cy - c.size / 2,
            width, c.size);

        g.setColor(new Color(170, 100, 0));
        g.drawOval(cx - width / 2, cy - c.size / 2,
            width, c.size);

        if (width > 14) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 16));
            g.drawString("$", cx - 5, cy + 6);
        }
    }

    void drawPlayer(Graphics2D g) {
        AffineTransform old = g.getTransform();

        int drawH = sliding ? playerH / 2 : playerH;
        int drawY = sliding
            ? (int)playerY + playerH / 2
            : (int)(playerY + jumpOffset);

        g.setColor(new Color(0, 0, 0, 90));
        g.fillOval(
            (int)playerX + 5,
            drawY + drawH - 5,
            playerW - 10, 14
        );

        g.translate(
            playerX + playerW / 2.0,
            drawY + drawH / 2.0
        );

        g.rotate(Math.toRadians(playerTilt));
        g.translate(0, Math.sin(frameCount * 0.3) * 2);

        int px = -playerW / 2;
        int py = -drawH / 2;

        GradientPaint body = new GradientPaint(
            px, py, new Color(255, 110, 110),
            px, py + drawH, new Color(190, 25, 25)
        );

        g.setPaint(body);
        g.fillRoundRect(px, py, playerW, drawH, 18, 18);

        if (!sliding) {
            GradientPaint shirt = new GradientPaint(
                px, py + 22, new Color(70, 210, 255),
                px, py + drawH, new Color(20, 90, 200)
            );

            g.setPaint(shirt);
            g.fillRoundRect(
                px + 6, py + 28,
                playerW - 12, drawH / 2 - 6, 12, 12
            );

            g.setColor(new Color(255, 220, 180));
            g.fillOval(px + 15, py + 5, 40, 36);

            g.setColor(new Color(70, 35, 15));
            g.fillArc(px + 12, py + 2, 46, 32, 0, 180);

            g.setColor(Color.BLACK);
            g.fillOval(px + 24, py + 20, 6, 6);
            g.fillOval(px + 38, py + 20, 6, 6);
        }

        g.setColor(new Color(100, 100, 255));
        g.fillRoundRect(px + 6, py + drawH - 28, 24, 28, 8, 8);
        g.fillRoundRect(px + playerW - 30, py + drawH - 28, 24, 28, 8, 8);

        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(3));
        g.drawRoundRect(px, py, playerW, drawH, 18, 18);

        g.setTransform(old);
    }

    void drawHUD(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 140));
        g.fillRoundRect(15, 15, 190, 65, 15, 15);

        g.setColor(Color.YELLOW);
        g.setFont(new Font("Arial", Font.BOLD, 22));
        g.drawString("Score: " + score, 30, 45);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString("Best: " + highScore, 30, 68);

        g.setColor(new Color(0, 0, 0, 140));
        g.fillRoundRect(W - 210, 15, 195, 65, 15, 15);

        g.setColor(Color.CYAN);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString("Speed: " + worldSpeed, W - 195, 40);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 11));
        g.drawString("LEFT/RIGHT Move", W - 195, 60);
    }

    void drawStartScreen(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRect(0, 0, W, H);

        g.setColor(new Color(255, 220, 60));
        g.setFont(new Font("Arial", Font.BOLD, 60));

        String title = "SUBWAY RUN";
        FontMetrics fm = g.getFontMetrics();

        g.drawString(title, (W - fm.stringWidth(title)) / 2, 260);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        fm = g.getFontMetrics();

        String[] instructions = {
            "LEFT / RIGHT  Move between lanes",
            "UP  Jump over obstacles",
            "DOWN  Slide under obstacles"
        };

        for (int i = 0; i < instructions.length; i++) {
            String s = instructions[i];
            g.drawString(s, (W - fm.stringWidth(s)) / 2, 350 + i * 40);
        }

        g.setColor(new Color(80, 255, 150));
        g.setFont(new Font("Arial", Font.BOLD, 24));
        fm = g.getFontMetrics();

        String start = "Press SPACE to Start";
        g.drawString(start, (W - fm.stringWidth(start)) / 2, 540);
    }

    void drawGameOver(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 190));
        g.fillRect(0, 0, W, H);

        g.setColor(new Color(255, 70, 70));
        g.setFont(new Font("Arial", Font.BOLD, 70));

        String title = "GAME OVER";
        FontMetrics fm = g.getFontMetrics();

        g.drawString(title, (W - fm.stringWidth(title)) / 2, 300);

        g.setColor(Color.YELLOW);
        g.setFont(new Font("Arial", Font.BOLD, 36));
        fm = g.getFontMetrics();

        String sc = "Score: " + score;
        g.drawString(sc, (W - fm.stringWidth(sc)) / 2, 380);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 22));
        fm = g.getFontMetrics();

        String best = "Best: " + highScore;
        g.drawString(best, (W - fm.stringWidth(best)) / 2, 430);

        g.setColor(new Color(80, 255, 150));
        String restart = "Press SPACE to Restart";
        g.drawString(restart, (W - fm.stringWidth(restart)) / 2, 510);
    }

    void updateGame() {
        frameCount++;
        roadOffset = (roadOffset + worldSpeed) % 40;

        float targetX = LANE_X[playerLane];
        float diff = targetX - playerX;

        if (Math.abs(diff) < 1) {
            playerX = targetX;
            playerTilt *= 0.7f;
        } else {
            playerX += diff * 0.25f;
            playerTilt = diff > 0 ? 12 : -12;
        }

        if (jumping) {
            jumpVel += 1.1f;
            jumpOffset += jumpVel;

            if (jumpOffset >= 0) {
                jumpOffset = 0;
                jumpVel = 0;
                jumping = false;
            }
        }

        if (sliding) {
            slideTimer++;

            if (slideTimer >= 25) {
                sliding = false;
                slideTimer = 0;
            }
        }

        for (Obstacle o : obstacles) {
            o.y += worldSpeed;
        }

        for (Coin c : coins) {
            c.y += worldSpeed;
            c.spin += 0.15f;
        }

        obstacles.removeIf(o -> o.y > H + 50);
        coins.removeIf(c -> c.y > H + 50);

        Rectangle playerRect = getPlayerRect();

        for (Obstacle o : obstacles) {
            if (!playerRect.intersects(o.getRect())) {
                continue;
            }

            if (o.type == 0 && jumping) {
                continue;
            }

            if (o.type == 1 && sliding) {
                continue;
            }

            endGame();
            return;
        }

        Iterator<Coin> iterator = coins.iterator();

        while (iterator.hasNext()) {
            Coin c = iterator.next();

            Rectangle coinRect = new Rectangle(
                (int)c.x, (int)c.y, c.size, c.size
            );

            if (playerRect.intersects(coinRect)) {
                iterator.remove();
                score += 10;
            }
        }

        if (frameCount % 40 == 0) {
            spawnObstacle();
        }

        if (frameCount % 90 == 0) {
            spawnCoin();
        }

        if (score >= nextSpeedScore && worldSpeed < 22) {
            worldSpeed++;
            nextSpeedScore += 150;
        }
    }

    void endGame() {
        gameRunning = false;

        if (score > highScore) {
            highScore = score;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameStarted && gameRunning) {
            updateGame();
        } else {
            frameCount++;

            for (Coin c : coins) {
                c.spin += 0.15f;
            }
        }

        repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (key == KeyEvent.VK_SPACE) {
            if (!gameStarted || !gameRunning) {
                resetGame();
            }
            return;
        }

        if (!gameStarted || !gameRunning) {
            return;
        }

        if (key == KeyEvent.VK_LEFT && playerLane > 0) {
            playerLane--;
        }

        if (key == KeyEvent.VK_RIGHT && playerLane < 2) {
            playerLane++;
        }

        if (key == KeyEvent.VK_UP && !jumping && !sliding) {
            jumping = true;
            jumpVel = -20;
            jumpOffset = 0;
        }

        if (key == KeyEvent.VK_DOWN && !sliding && !jumping) {
            sliding = true;
            slideTimer = 0;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    @Override
    public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame window = new JFrame("Subway Surfers 2D");

            SubwaySurfers game = new SubwaySurfers();

            window.add(game);
            window.pack();
            window.setResizable(false);
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            window.setLocationRelativeTo(null);
            window.setVisible(true);

            game.requestFocusInWindow();
        });
    }
}
