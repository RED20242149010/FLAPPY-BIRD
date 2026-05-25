import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class FlappyBird extends JPanel implements ActionListener, KeyListener {

    static final int WINDOW_WIDTH  = 400;
    static final int WINDOW_HEIGHT = 600;

    String currentScreen = "welcome";

    int birdX      = 80;
    int birdY      = 250;
    int birdWidth  = 34;
    int birdHeight = 24;

    double velocity = 0;
    double GRAVITY  = 0.5;
    double JUMP     = -9.0;

    ArrayList<int[]> pipes = new ArrayList<>();
    final int PIPE_WIDTH = 60;
    int GAP        = 150;
    int PIPE_SPEED = 4;

    ArrayList<int[]> coins = new ArrayList<>();
    final int COIN_SIZE = 20;
    int coinScore = 0;

    boolean gameStarted = false;
    boolean gameOver    = false;
    int score           = 0;
    int highScore       = 0;
    String difficulty   = "Normal";

    Timer gameTimer;
    Timer pipeTimer;
    Timer coinTimer;

    int wingFrame  = 0;
    int frameTick  = 0;

    int[] cloudX     = {50, 160, 280, 370};
    int[] cloudY     = {60, 110, 70, 130};
    int groundOffset = 0;

    Rectangle btnEasy   = new Rectangle(100, 260, 200, 45);
    Rectangle btnNormal = new Rectangle(100, 320, 200, 45);
    Rectangle btnHard   = new Rectangle(100, 380, 200, 45);
    Rectangle btnInfo   = new Rectangle(140, 450, 120, 40);
    Rectangle btnBack   = new Rectangle(140, 500, 120, 40);

    String hoverBtn = "";
    Random random = new Random();

    public FlappyBird() {
        setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        setFocusable(true);
        addKeyListener(this);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleClick(e.getX(), e.getY());
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                handleHover(e.getX(), e.getY());
            }
        });

        gameTimer = new Timer(16, this);
        gameTimer.start();

        pipeTimer = new Timer(2000, e -> spawnPipe());
        pipeTimer.start();

        coinTimer = new Timer(3000, e -> spawnCoin());
        coinTimer.start();
    }

    void handleClick(int mx, int my) {
        Point p = new Point(mx, my);
        if (currentScreen.equals("welcome")) {
            if (btnEasy.contains(p)) {
                setDifficulty("Easy");
                startGame();
            } else if (btnNormal.contains(p)) {
                setDifficulty("Normal");
                startGame();
            } else if (btnHard.contains(p)) {
                setDifficulty("Hard");
                startGame();
            } else if (btnInfo.contains(p)) {
                openInfoWindow();
            }
        }
    }

    void handleHover(int mx, int my) {
        Point p = new Point(mx, my);
        if      (btnEasy.contains(p))   hoverBtn = "easy";
        else if (btnNormal.contains(p)) hoverBtn = "normal";
        else if (btnHard.contains(p))   hoverBtn = "hard";
        else if (btnInfo.contains(p))   hoverBtn = "info";
        else if (btnBack.contains(p))   hoverBtn = "back";
        else                            hoverBtn = "";
        repaint();
    }

    void setDifficulty(String mode) {
        difficulty = mode;
        switch (mode) {
            case "Easy":
                GRAVITY    = 0.3;
                JUMP       = -7.5;
                PIPE_SPEED = 2;
                GAP        = 200;
                break;
            case "Normal":
                GRAVITY    = 0.5;
                JUMP       = -9.0;
                PIPE_SPEED = 4;
                GAP        = 150;
                break;
            case "Hard":
                GRAVITY    = 0.65;
                JUMP       = -9.5;
                PIPE_SPEED = 6;
                GAP        = 115;
                break;
        }
    }

    void startGame() {
        currentScreen = "playing";
        restart();
    }

    void spawnPipe() {
        if (!currentScreen.equals("playing")) return;
        if (!gameStarted || gameOver) return;
        int topHeight = 80 + random.nextInt(WINDOW_HEIGHT - GAP - 160);
        pipes.add(new int[]{ WINDOW_WIDTH, topHeight });
    }

    void spawnCoin() {
        if (!currentScreen.equals("playing")) return;
        if (!gameStarted || gameOver) return;
        int coinY = 80 + random.nextInt(WINDOW_HEIGHT - 200);
        coins.add(new int[]{ WINDOW_WIDTH, coinY, 1 });
    }

    void update() {
        if (!currentScreen.equals("playing")) return;
        if (!gameStarted || gameOver) return;

        velocity += GRAVITY;
        birdY    += (int) velocity;

        frameTick++;
        if (frameTick % 8 == 0) {
            wingFrame = (wingFrame + 1) % 3;
        }

        groundOffset = (groundOffset + PIPE_SPEED) % 20;

        for (int i = pipes.size() - 1; i >= 0; i--) {
            int prevX = pipes.get(i)[0];
            pipes.get(i)[0] -= PIPE_SPEED + (score / 5);
            int currX = pipes.get(i)[0];
            if (prevX + PIPE_WIDTH >= birdX && currX + PIPE_WIDTH < birdX) {
                score++;
                if (score > highScore) highScore = score;
            }
            if (pipes.get(i)[0] + PIPE_WIDTH < 0) {
                pipes.remove(i);
            }
        }

        for (int i = 0; i < cloudX.length; i++) {
            cloudX[i] -= 1;
            if (cloudX[i] < -80) cloudX[i] = WINDOW_WIDTH + 20;
        }

        for (int i = coins.size() - 1; i >= 0; i--) {
            coins.get(i)[0] -= PIPE_SPEED + (score / 5);
            int cx = coins.get(i)[0];
            int cy = coins.get(i)[1];
            int active = coins.get(i)[2];
            if (active == 1) {
                Rectangle coinBox = new Rectangle(cx, cy, COIN_SIZE, COIN_SIZE);
                Rectangle birdBox = new Rectangle(birdX + 4, birdY + 4, birdWidth - 8, birdHeight - 8);
                if (coinBox.intersects(birdBox)) {
                    coinScore++;
                    score++;
                    if (score > highScore) highScore = score;
                    coins.remove(i);
                    continue;
                }
            }
            if (cx + COIN_SIZE < 0) {
                coins.remove(i);
            }
        }

        checkCollisions();
    }

    void checkCollisions() {
        if (birdY + birdHeight >= WINDOW_HEIGHT - 60) { gameOver = true; return; }
        if (birdY <= 0)                               { gameOver = true; return; }

        Rectangle birdBox = new Rectangle(birdX + 4, birdY + 4, birdWidth - 8, birdHeight - 8);

        for (int[] pipe : pipes) {
            int px = pipe[0];
            int ph = pipe[1];
            Rectangle topPipe = new Rectangle(px, 0, PIPE_WIDTH, ph);
            Rectangle botPipe = new Rectangle(px, ph + GAP, PIPE_WIDTH, WINDOW_HEIGHT);
            if (birdBox.intersects(topPipe) || birdBox.intersects(botPipe)) {
                gameOver = true;
                return;
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (currentScreen.equals("welcome")) {
            drawWelcomeScreen(g2);
        } else {
            drawSky(g2);
            drawClouds(g2);
            drawPipes(g2);
            drawCoins(g2);
            drawGround(g2);
            drawBird(g2);
            drawScore(g2);
            drawDifficultyBadge(g2);
            if (!gameStarted) drawStartPrompt(g2);
            if (gameOver)     drawGameOverScreen(g2);
        }
    }

    void drawWelcomeScreen(Graphics2D g) {
        GradientPaint bg = new GradientPaint(0, 0, new Color(0x1A0000), 0, WINDOW_HEIGHT, new Color(0x3D0000));
        g.setPaint(bg);
        g.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

        g.setColor(new Color(255, 255, 255, 10));
        g.fillOval(-40, -40, 200, 200);
        g.fillOval(280, 400, 180, 180);

        g.setColor(new Color(255, 100, 100, 100));
        int[][] stars = {{30,40},{80,90},{150,30},{220,70},{310,20},{360,80},{50,150},{280,130},{340,160},{100,200}};
        for (int[] s : stars) g.fillOval(s[0], s[1], 3, 3);

        g.setColor(new Color(0, 0, 0, 100));
        g.fillRoundRect(20, 60, WINDOW_WIDTH - 40, 150, 25, 25);

        g.setFont(new Font("Arial Black", Font.BOLD, 18));
        g.setColor(new Color(0xFF4500));
        drawCenteredString(g, "WELCOME TO", WINDOW_WIDTH, 100);

        g.setFont(new Font("Arial Black", Font.BOLD, 36));
        GradientPaint titleGrad = new GradientPaint(0, 120, new Color(0xFF4500), 0, 160, new Color(0xFF0000));
        g.setPaint(titleGrad);
        drawCenteredString(g, "FLAPPY BIRD", WINDOW_WIDTH, 155);

        g.setFont(new Font("Arial", Font.ITALIC, 14));
        g.setColor(new Color(255, 150, 150, 180));
        drawCenteredString(g, "Select your difficulty to play", WINDOW_WIDTH, 195);

        drawMenuButton(g, btnEasy,   "EASY",   new Color(0x2ECC71), new Color(0x27AE60), hoverBtn.equals("easy"));
        drawMenuButton(g, btnNormal, "NORMAL", new Color(0xF39C12), new Color(0xD68910), hoverBtn.equals("normal"));
        drawMenuButton(g, btnHard,   "HARD",   new Color(0xE74C3C), new Color(0xC0392B), hoverBtn.equals("hard"));
        drawMenuButton(g, btnInfo,   "INFO",   new Color(0x3498DB), new Color(0x2980B9), hoverBtn.equals("info"));

        g.setColor(new Color(255, 50, 50, 30));
        g.fillRect(0, WINDOW_HEIGHT - 50, WINDOW_WIDTH, 50);
        g.setFont(new Font("Arial", Font.PLAIN, 11));
        g.setColor(new Color(255, 150, 150, 120));
        drawCenteredString(g, "CSE-2200 | Software Development Laboratory", WINDOW_WIDTH, WINDOW_HEIGHT - 20);
    }

    void drawMenuButton(Graphics2D g, Rectangle btn, String label, Color c1, Color c2, boolean hovered) {
        int x = btn.x, y = btn.y, w = btn.width, h = btn.height;
        g.setColor(new Color(0, 0, 0, 80));
        g.fillRoundRect(x + 4, y + 4, w, h, 15, 15);
        GradientPaint grad = new GradientPaint(x, y, hovered ? c1.brighter() : c1, x, y + h, hovered ? c2.brighter() : c2);
        g.setPaint(grad);
        g.fillRoundRect(x, y, w, h, 15, 15);
        g.setColor(new Color(255, 255, 255, 60));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(x, y, w, h, 15, 15);
        g.setStroke(new BasicStroke(1));
        g.setFont(new Font("Arial Black", Font.BOLD, 15));
        g.setColor(Color.WHITE);
        FontMetrics fm = g.getFontMetrics();
        int tx = x + (w - fm.stringWidth(label)) / 2;
        int ty = y + (h + fm.getAscent() - fm.getDescent()) / 2;
        g.drawString(label, tx, ty);
    }

    void openInfoWindow() {
        JFrame infoWindow = new JFrame("About This Game");
        infoWindow.setSize(420, 600);
        infoWindow.setResizable(false);
        infoWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        infoWindow.setLocationRelativeTo(null);
        infoWindow.add(new InfoPanel(infoWindow));
        infoWindow.pack();
        infoWindow.setVisible(true);
    }

    class InfoPanel extends JPanel {
        JFrame owner;

        InfoPanel(JFrame owner) {
            this.owner = owner;
            setPreferredSize(new Dimension(420, 580));
            setBackground(new Color(0x1A2A3A));
            setLayout(null);

            JButton closeBtn = new JButton("\u2190 CLOSE");
            closeBtn.setBounds(135, 510, 150, 42);
            closeBtn.setBackground(new Color(0x2980B9));
            closeBtn.setForeground(Color.WHITE);
            closeBtn.setFont(new Font("Arial Black", Font.BOLD, 13));
            closeBtn.setFocusPainted(false);
            closeBtn.setBorderPainted(false);
            closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            closeBtn.addActionListener(e -> owner.dispose());
            closeBtn.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { closeBtn.setBackground(new Color(0x3498DB)); }
                public void mouseExited(MouseEvent e)  { closeBtn.setBackground(new Color(0x2980B9)); }
            });
            add(closeBtn);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int W = getWidth();

            GradientPaint bg = new GradientPaint(0, 0, new Color(0x1A2A3A), 0, 580, new Color(0x0F1E2E));
            g2.setPaint(bg);
            g2.fillRect(0, 0, W, 580);

            GradientPaint goldBar = new GradientPaint(0, 0, new Color(0xFFD700), W, 0, new Color(0xFF6B00));
            g2.setPaint(goldBar);
            g2.fillRect(0, 0, W, 6);

            g2.setFont(new Font("Arial Black", Font.BOLD, 20));
            g2.setColor(new Color(0xFFD700));
            drawC(g2, "ABOUT THIS GAME", W, 48);

            g2.setFont(new Font("Arial", Font.ITALIC, 13));
            g2.setColor(new Color(255, 255, 255, 100));
            drawC(g2, "Flappy Bird  \u2014  Java Edition", W, 68);

            g2.setColor(new Color(255, 255, 255, 40));
            g2.fillRect(30, 78, W - 60, 1);

            g2.setFont(new Font("Arial", Font.BOLD, 10));
            g2.setColor(new Color(0xFFD700));
            g2.drawString("DEVELOPED BY", 30, 105);

            drawAvatar(g2, 30, 115, "MS", new Color(0x1B4F72), new Color(0x5DADE2));
            g2.setFont(new Font("Arial Black", Font.BOLD, 14));
            g2.setColor(Color.WHITE);
            g2.drawString("Md. Sanzidul Islam Reshad", 85, 133);
            g2.setFont(new Font("Arial", Font.PLAIN, 12));
            g2.setColor(new Color(150, 200, 255, 200));
            g2.drawString("ID: 20242149010", 85, 151);

            g2.setColor(new Color(255, 255, 255, 20));
            g2.fillRect(30, 168, W - 60, 1);

            drawAvatar(g2, 30, 178, "KF", new Color(0x1D6A47), new Color(0x58D68D));
            g2.setFont(new Font("Arial Black", Font.BOLD, 14));
            g2.setColor(Color.WHITE);
            g2.drawString("Fatema Khatun", 85, 196);
            g2.setFont(new Font("Arial", Font.PLAIN, 12));
            g2.setColor(new Color(150, 200, 255, 200));
            g2.drawString("ID: 20242126010", 85, 214);

            g2.setColor(new Color(255, 255, 255, 40));
            g2.fillRect(30, 232, W - 60, 1);

            g2.setFont(new Font("Arial", Font.BOLD, 10));
            g2.setColor(new Color(0xFFD700));
            g2.drawString("COURSE INFORMATION", 30, 258);

            g2.setColor(new Color(255, 255, 255, 12));
            g2.fillRoundRect(30, 268, W - 60, 80, 12, 12);
            g2.setColor(new Color(255, 255, 255, 25));
            g2.drawRoundRect(30, 268, W - 60, 80, 12, 12);

            g2.setFont(new Font("Arial", Font.PLAIN, 13));
            g2.setColor(new Color(255, 255, 255, 140));
            g2.drawString("Course name", 45, 293);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 13));
            g2.drawString("Software Development Laboratory", 45, 313);

            g2.setFont(new Font("Arial", Font.PLAIN, 13));
            g2.setColor(new Color(255, 255, 255, 140));
            g2.drawString("Course code", 45, 335);

            g2.setColor(new Color(0x1A5276));
            g2.fillRoundRect(140, 322, 80, 22, 20, 20);
            g2.setColor(new Color(0x5DADE2));
            g2.setFont(new Font("Arial Black", Font.BOLD, 11));
            g2.drawString("CSE-2200", 150, 337);

            g2.setColor(new Color(255, 255, 255, 40));
            g2.fillRect(30, 365, W - 60, 1);

            g2.setFont(new Font("Arial", Font.BOLD, 10));
            g2.setColor(new Color(0xFFD700));
            g2.drawString("COURSE TEACHER", 30, 390);

            drawAvatar(g2, 30, 400, "AN", new Color(0x7D4E0A), new Color(0xF5B041));
            g2.setFont(new Font("Arial Black", Font.BOLD, 14));
            g2.setColor(Color.WHITE);
            g2.drawString("Abu Naim Khan", 85, 418);
            g2.setFont(new Font("Arial", Font.PLAIN, 12));
            g2.setColor(new Color(255, 255, 255, 140));
            g2.drawString("Course instructor", 85, 436);

            g2.setPaint(goldBar);
            g2.fillRect(0, 574, W, 6);
        }

        void drawAvatar(Graphics2D g2, int x, int y, String initials, Color bgColor, Color textColor) {
            g2.setColor(bgColor);
            g2.fillOval(x, y, 44, 44);
            g2.setColor(textColor);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval(x, y, 44, 44);
            g2.setStroke(new BasicStroke(1));
            g2.setFont(new Font("Arial Black", Font.BOLD, 13));
            FontMetrics fm = g2.getFontMetrics();
            int tx = x + (44 - fm.stringWidth(initials)) / 2;
            int ty = y + (44 + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(initials, tx, ty);
        }

        void drawC(Graphics2D g2, String text, int width, int y) {
            FontMetrics fm = g2.getFontMetrics();
            int x = (width - fm.stringWidth(text)) / 2;
            g2.drawString(text, x, y);
        }
    }

    void drawSky(Graphics2D g) {
        GradientPaint skyGradient = new GradientPaint(
                0, 0, new Color(0x1A0000),
                0, WINDOW_HEIGHT - 60, new Color(0x3D0000));
        g.setPaint(skyGradient);
        g.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

        g.setColor(new Color(180, 0, 0, 60));
        g.fillOval(295, 15, 100, 100);

        g.setColor(new Color(200, 0, 0, 80));
        g.fillOval(305, 25, 80, 80);

        g.setColor(new Color(0xCC0000));
        g.fillOval(315, 30, 60, 60);

        g.setColor(new Color(255, 80, 80, 120));
        g.fillOval(320, 33, 25, 20);

        g.setColor(new Color(80, 0, 0, 150));
        g.fillOval(333, 50, 35, 35);
    }

    void drawClouds(Graphics2D g) {
        g.setColor(new Color(180, 50, 50, 120));
        for (int i = 0; i < cloudX.length; i++) {
            int cx = cloudX[i];
            int cy = cloudY[i];
            g.fillOval(cx,      cy + 10, 60, 30);
            g.fillOval(cx + 15, cy,      40, 30);
            g.fillOval(cx + 35, cy + 8,  50, 28);
        }
    }

    void drawPipes(Graphics2D g) {
        for (int[] pipe : pipes) {
            int px = pipe[0];
            int ph = pipe[1];

            GradientPaint pipeColor = new GradientPaint(
                    px, 0, new Color(0xFF6D00),
                    px + PIPE_WIDTH, 0, new Color(0xE65100));
            g.setPaint(pipeColor);

            g.fillRect(px, 0, PIPE_WIDTH, ph);
            g.fillRect(px - 5, ph - 20, PIPE_WIDTH + 10, 20);

            g.fillRect(px, ph + GAP, PIPE_WIDTH, WINDOW_HEIGHT);
            g.fillRect(px - 5, ph + GAP, PIPE_WIDTH + 10, 20);

            g.setColor(new Color(255, 255, 255, 60));
            g.fillRect(px + 5, 0, 8, ph);
            g.fillRect(px + 5, ph + GAP, 8, WINDOW_HEIGHT);
        }
    }

    void drawCoins(Graphics2D g) {
        for (int[] coin : coins) {
            int cx = coin[0];
            int cy = coin[1];

            g.setColor(new Color(255, 200, 0, 80));
            g.fillOval(cx - 5, cy - 5, COIN_SIZE + 10, COIN_SIZE + 10);

            GradientPaint coinGrad = new GradientPaint(
                    cx, cy, new Color(0xFFD700),
                    cx + COIN_SIZE, cy + COIN_SIZE, new Color(0xFF8C00));
            g.setPaint(coinGrad);
            g.fillOval(cx, cy, COIN_SIZE, COIN_SIZE);

            g.setColor(new Color(255, 255, 150, 180));
            g.fillOval(cx + 4, cy + 3, 7, 5);

            g.setColor(new Color(180, 120, 0));
            g.setStroke(new BasicStroke(1.5f));
            g.drawOval(cx, cy, COIN_SIZE, COIN_SIZE);
            g.setStroke(new BasicStroke(1));

            g.setFont(new Font("Arial Black", Font.BOLD, 9));
            g.setColor(new Color(120, 70, 0));
            g.drawString("$", cx + 6, cy + 14);
        }
    }

    void drawGround(Graphics2D g) {
        g.setColor(new Color(0x4A1000));
        g.fillRect(0, WINDOW_HEIGHT - 60, WINDOW_WIDTH, 60);
        g.setColor(new Color(0x3B0A00));
        g.fillRect(0, WINDOW_HEIGHT - 40, WINDOW_WIDTH, 40);
        g.setColor(new Color(0x2D0700));
        for (int x = -groundOffset; x < WINDOW_WIDTH; x += 20) {
            g.fillRect(x, WINDOW_HEIGHT - 40, 10, 40);
        }
    }

    void drawBird(Graphics2D g) {
        int bx = birdX;
        int by = birdY;

        g.setColor(new Color(0xFFFFFF));
        g.fillOval(bx, by, birdWidth, birdHeight);

        g.setColor(new Color(0xDDDDDD));
        int[] wingY = { by - 6, by, by + 6 };
        g.fillOval(bx + 5, wingY[wingFrame], 18, 10);

        g.setColor(Color.WHITE);
        g.fillOval(bx + 20, by + 4, 10, 10);
        g.setColor(Color.BLACK);
        g.fillOval(bx + 23, by + 6, 5, 5);
        g.setColor(Color.WHITE);
        g.fillOval(bx + 25, by + 6, 2, 2);

        g.setColor(new Color(0xFF6600));
        int[] beakX = { bx + 30, bx + 38, bx + 30 };
        int[] beakY = { by + 8,  by + 12, by + 16 };
        g.fillPolygon(beakX, beakY, 3);
    }

    void drawScore(Graphics2D g) {
        String text = String.valueOf(score);
        g.setFont(new Font("Arial Black", Font.BOLD, 36));
        g.setColor(new Color(0, 0, 0, 80));
        g.drawString(text, 192, 62);
        g.setColor(new Color(0xFF1744));
        g.drawString(text, 190, 60);

        g.setColor(new Color(0, 0, 0, 80));
        g.fillRoundRect(9, 9, 100, 26, 10, 10);
        g.setColor(new Color(255, 200, 0));
        g.fillRoundRect(7, 7, 100, 26, 10, 10);
        g.setFont(new Font("Arial Black", Font.BOLD, 13));
        g.setColor(new Color(100, 50, 0));
        g.drawString("$ x " + coinScore, 15, 25);
    }

    void drawDifficultyBadge(Graphics2D g) {
        Color badgeColor;
        switch (difficulty) {
            case "Easy": badgeColor = new Color(0x2ECC71); break;
            case "Hard": badgeColor = new Color(0xE74C3C); break;
            default:     badgeColor = new Color(0xF39C12); break;
        }
        g.setColor(new Color(0, 0, 0, 100));
        g.fillRoundRect(298, 12, 90, 24, 10, 10);
        g.setColor(badgeColor);
        g.fillRoundRect(296, 10, 90, 24, 10, 10);
        g.setFont(new Font("Arial Black", Font.BOLD, 11));
        g.setColor(Color.WHITE);
        g.drawString(difficulty.toUpperCase(), 310, 27);
    }

    void drawStartPrompt(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 140));
        g.fillRoundRect(60, WINDOW_HEIGHT / 2 - 50, WINDOW_WIDTH - 120, 90, 20, 20);
        g.setFont(new Font("Arial Black", Font.BOLD, 16));
        g.setColor(new Color(0xFF4500));
        drawCenteredString(g, "READY!", WINDOW_WIDTH, WINDOW_HEIGHT / 2 - 15);
        g.setFont(new Font("Arial", Font.PLAIN, 13));
        g.setColor(Color.WHITE);
        drawCenteredString(g, "Press SPACE or UP to flap", WINDOW_WIDTH, WINDOW_HEIGHT / 2 + 15);
    }

    void drawGameOverScreen(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRoundRect(35, WINDOW_HEIGHT / 2 - 120, WINDOW_WIDTH - 70, 220, 20, 20);
        GradientPaint bar = new GradientPaint(35, 0, new Color(0xFF4500), WINDOW_WIDTH - 35, 0, new Color(0xFF0000));
        g.setPaint(bar);
        g.fillRoundRect(35, WINDOW_HEIGHT / 2 - 120, WINDOW_WIDTH - 70, 8, 5, 5);
        g.setFont(new Font("Arial Black", Font.BOLD, 30));
        g.setColor(new Color(0xFF4500));
        drawCenteredString(g, "GAME OVER", WINDOW_WIDTH, WINDOW_HEIGHT / 2 - 65);
        g.setFont(new Font("Arial", Font.PLAIN, 16));
        g.setColor(Color.WHITE);
        drawCenteredString(g, "Score: " + score, WINDOW_WIDTH, WINDOW_HEIGHT / 2 - 25);
        g.setColor(new Color(255, 150, 150));
        drawCenteredString(g, "Best: " + highScore, WINDOW_WIDTH, WINDOW_HEIGHT / 2 + 5);
        g.setColor(new Color(255, 200, 0));
        drawCenteredString(g, "Coins: " + coinScore, WINDOW_WIDTH, WINDOW_HEIGHT / 2 + 25);
        g.setColor(new Color(255, 255, 255, 180));
        g.setFont(new Font("Arial", Font.PLAIN, 13));
        drawCenteredString(g, "Press R to restart", WINDOW_WIDTH, WINDOW_HEIGHT / 2 + 45);
        drawCenteredString(g, "Press M for main menu", WINDOW_WIDTH, WINDOW_HEIGHT / 2 + 65);
    }

    void drawCenteredString(Graphics2D g, String text, int width, int y) {
        FontMetrics fm = g.getFontMetrics();
        int x = (width - fm.stringWidth(text)) / 2;
        g.drawString(text, x, y);
    }

    void flap() {
        if (gameOver) return;
        if (!gameStarted) { gameStarted = true; return; }
        velocity = JUMP;
    }

    void restart() {
        birdY        = 250;
        velocity     = 0;
        pipes.clear();
        coins.clear();
        score        = 0;
        coinScore    = 0;
        gameOver     = false;
        gameStarted  = false;
        groundOffset = 0;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (currentScreen.equals("playing")) {
            if (key == KeyEvent.VK_SPACE || key == KeyEvent.VK_UP) flap();
            if (key == KeyEvent.VK_R && gameOver) restart();
            if (key == KeyEvent.VK_M && gameOver) {
                currentScreen = "welcome";
                restart();
            }
        }
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}

    @Override
    public void actionPerformed(ActionEvent e) {
        update();
        repaint();
    }

    public static void main(String[] args) {
        JFrame window = new JFrame("Flappy Bird");
        FlappyBird game = new FlappyBird();
        window.add(game);
        window.pack();
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);
        window.setLocationRelativeTo(null);
        window.setVisible(true);
    }
}