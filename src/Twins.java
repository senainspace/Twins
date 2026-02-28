import enigma.console.Console;
import enigma.core.Enigma;
import enigma.event.TextMouseEvent;
import enigma.event.TextMouseListener;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Random;

// Main class that manages the game loop: input, timing, rendering, and coordination of all objects.
public class Twins {
    Console cn;
    Coard coard;
    Player player;
    ArrayList<Robot> robots;
    ArrayList<Treasure> treasures;

    // Total score of the computer (robots)
    int computerScore = 0;

    Random rand = new Random();

    int keypr, rkey;
    int robotTimer = 0;
    int inputTimer = 0; // Timer for game input system (new element every 20 units)

    // HUD start X position (maze is 0..52)
    private static final int HUD_X = 55;

    // Start time for real-time seconds display
    private long startTimeMs;

    // Input element weights: 1→2, 2→2, 3→2, @→3, C→1, X→1  (total = 11)
    // @ is placed but currently has no effect (laser not yet implemented)
    private static final char[] INPUT_ELEMENTS   = {'1', '2', '3', '@', 'C', 'X'};
    private static final int[]  INPUT_WEIGHTS    = { 2,   2,   2,   3,   1,   1 };
    private static final int    WEIGHT_TOTAL     = 11;

    public Twins(int mode) {
        cn = Enigma.getConsole("Twins");
        coard = new Coard(mode);
        robots = new ArrayList<Robot>();
        treasures = new ArrayList<Treasure>();

        // Start time (used for Time : seconds)
        startTimeMs = System.currentTimeMillis();

        // Player A/B placement
        int[] InitialPos = randomFreeCell();
        player = new Player(InitialPos[0], InitialPos[1], InitialPos[0], InitialPos[1]);

        // Add 3 C-Robots and 3 X-Robots
        for (int i = 0; i < 3; i++) {
            int[] p = randomFreeCell();
            robots.add(new Robot(p[0], p[1], 'C'));
        }
        for (int i = 0; i < 3; i++) {
            int[] p = randomFreeCell();
            robots.add(new Robot(p[0], p[1], 'X'));
        }

        // Place the first 10 elements of the game input system at the beginning
        for (int i = 0; i < 10; i++) {
            spawnInputElement();
        }

        setupInput();
    }

    /**
     * Picks a random element according to the input system probabilities and
     * places it at a random free cell in the maze.
     *
     * Probabilities: 1→2/11, 2→2/11, 3→2/11, @→3/11, C→1/11, X→1/11
     * @ is placed on the grid but has no gameplay effect until laser is implemented.
     * C and X spawn new robots.
     */
    private void spawnInputElement() {
        char element = pickInputElement();

        if (element == 'C' || element == 'X') {
            // Spawn a new robot
            int[] p = randomFreeCell();
            Robot newRobot = new Robot(p[0], p[1], element);
            robots.add(newRobot);
            cn.getTextWindow().output(p[0], p[1], element);
        } else {
            // Place a treasure or laser pack on the grid
            int[] p = randomFreeCell();
            if (element == '1' || element == '2' || element == '3') {
                Treasure t = new Treasure(p[0], p[1], element);
                treasures.add(t);
                coard.grid[p[1]][p[0]] = element;
            } else {
                // '@' – laser pack: just mark on grid for now
                coard.grid[p[1]][p[0]] = element;
            }
            cn.getTextWindow().output(p[0], p[1], element);
        }
    }

    /** Weighted random pick from INPUT_ELEMENTS using INPUT_WEIGHTS. */
    private char pickInputElement() {
        int roll = rand.nextInt(WEIGHT_TOTAL); // 0..10
        int cumulative = 0;
        for (int i = 0; i < INPUT_ELEMENTS.length; i++) {
            cumulative += INPUT_WEIGHTS[i];
            if (roll < cumulative) return INPUT_ELEMENTS[i];
        }
        return INPUT_ELEMENTS[0]; // fallback
    }

    private void setupInput() {
        cn.getTextWindow().addTextMouseListener(new TextMouseListener() {
            public void mouseClicked(TextMouseEvent e) {}
            public void mousePressed(TextMouseEvent e) {}
            public void mouseReleased(TextMouseEvent e) {}
        });
        cn.getTextWindow().addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent e) {}
            public void keyPressed(KeyEvent e) {
                if (keypr == 0) { keypr = 1; rkey = e.getKeyCode(); }
            }
            public void keyReleased(KeyEvent e) {}
        });
    }

    public void run() {
        // Draw maze
        for (int r = 0; r < Coard.ROWS; r++)
            for (int c = 0; c < Coard.COLS; c++)
                cn.getTextWindow().output(c, r, coard.grid[r][c]);

        // Draw treasures (grid already has them, but ensure)
        for (Treasure t : treasures) {
            cn.getTextWindow().output(t.x, t.y, t.symbol);
        }

        // Draw robots
        for (Robot robot : robots)
            cn.getTextWindow().output(robot.x, robot.y, robot.type);

        // Draw player
        drawPlayer();

        updateHUD();

        // Game loop
        while (true) {
            // --- PLAYER INPUT ---
            if (keypr == 1) {
                int dx = 0, dy = 0;

                if      (rkey == KeyEvent.VK_LEFT)  dx = -1;
                else if (rkey == KeyEvent.VK_RIGHT) dx =  1;
                else if (rkey == KeyEvent.VK_UP)    dy = -1;
                else if (rkey == KeyEvent.VK_DOWN)  dy =  1;

                if (dx != 0 || dy != 0) {
                    clearPlayer();
                    player.move(dx, dy, coard);
                    // Treasure collection (A or B)
                    handlePlayerTreasureCollection();
                    drawPlayer();
                    updateHUD();
                } else if (rkey == KeyEvent.VK_M) {
                    player.mode *= -1;
                    drawPlayer();
                }

                keypr = 0;
            }

            // --- ROBOT MOVEMENT (every 4 time units) ---
            robotTimer++;
            if (robotTimer >= 4) {
                for (Robot robot : robots) {
                    if (robot.x == player.ax && robot.y == player.ay) {
                        drawPlayer();
                    } else if (robot.x == player.bx && robot.y == player.by) {
                        drawPlayer();
                    } else {
                        cn.getTextWindow().output(robot.x, robot.y, coard.grid[robot.y][robot.x]);
                    }

                    robot.step(coard);

                    // If robot steps on treasure, score + remove treasure
                    handleRobotTreasureCollection(robot);

                    // Draw robot (if not on top of player A)
                    if (!(robot.x == player.ax && robot.y == player.ay)) {
                        cn.getTextWindow().output(robot.x, robot.y, robot.type);
                    }
                }
                robotTimer = 0;
                checkPlayerHarming();
                updateHUD();
            }

            // --- GAME INPUT SYSTEM (every 20 time units) ---
            inputTimer++;
            if (inputTimer >= 20) {
                spawnInputElement();
                inputTimer = 0;
                updateHUD();
            }

            sleepMs(50);
        }
    }

    // --- TREASURE COLLECTION / SCORING ---

    private void handlePlayerTreasureCollection() {
        collectTreasureAt(player.ax, player.ay, false);
        if (!(player.ax == player.bx && player.ay == player.by)) {
            collectTreasureAt(player.bx, player.by, false);
        }
    }

    private void handleRobotTreasureCollection(Robot robot) {
        collectTreasureAt(robot.x, robot.y, true);
    }

    // byComputer=false -> Player score, true -> Computer score (3x)
    private void collectTreasureAt(int x, int y, boolean byComputer) {
        char cell = coard.getCoordinate(y, x);
        if (cell != '1' && cell != '2' && cell != '3') return;

        Treasure found = null;
        for (Treasure t : treasures) {
            if (t.x == x && t.y == y) { found = t; break; }
        }

        int pPoints;
        int cPoints;
        if (found != null) {
            pPoints = found.playerPoints;
            cPoints = found.computerPoints;
        } else {
            Treasure tmp = new Treasure(x, y, cell);
            pPoints = tmp.playerPoints;
            cPoints = tmp.computerPoints;
        }

        if (byComputer) computerScore += cPoints;
        else player.score += pPoints;

        coard.grid[y][x] = ' ';
        cn.getTextWindow().output(x, y, ' ');
        if (found != null) treasures.remove(found);
    }

    private void placeTreasureRandom(char symbol) {
        int[] p = randomFreeCell();
        Treasure t = new Treasure(p[0], p[1], symbol);
        treasures.add(t);
        coard.grid[p[1]][p[0]] = symbol;
    }

    // --- DRAW HELPERS ---

    private void drawPlayer() {
        enigma.console.TextAttributes attr;

        if (player.mode == 1) {
            attr = new enigma.console.TextAttributes(java.awt.Color.GREEN, java.awt.Color.BLACK);
        } else {
            attr = new enigma.console.TextAttributes(java.awt.Color.MAGENTA, java.awt.Color.BLACK);
        }

        cn.getTextWindow().output(player.bx, player.by, 'B', attr);
        cn.getTextWindow().output(player.ax, player.ay, 'A', attr);
    }

    private void clearPlayer() {
        cn.getTextWindow().output(player.ax, player.ay, coard.grid[player.ay][player.ax]);
        cn.getTextWindow().output(player.bx, player.by, coard.grid[player.by][player.bx]);
    }

    private void updateHUD() {
        int seconds = (int) ((System.currentTimeMillis() - startTimeMs) / 1000);

        writeText(HUD_X, 0, "Time : " + seconds + "   ");

        writeText(HUD_X, 2, "P.Score : " + player.score + "   ");
        writeText(HUD_X, 3, "P.Life  : " + player.hp + "   ");
        writeText(HUD_X, 4, "P.Laser : " + player.laserCount + "   ");

        int cCount = 0, xCount = 0;
        for (Robot r : robots) {
            if (r.isAlive()) {
                if (r.type == 'C') cCount++;
                else if (r.type == 'X') xCount++;
            }
        }

        writeText(HUD_X, 6, "C.Score : " + computerScore + "   ");
        writeText(HUD_X, 7, "C-Robots: " + cCount + "   ");
        writeText(HUD_X, 8, "X-Robots: " + xCount + "   ");
    }

    private void writeText(int x, int y, String s) {
        for (int i = 0; i < s.length(); i++) {
            cn.getTextWindow().output(x + i, y, s.charAt(i));
        }
    }

    // --- POSITION HELPERS ---

    private boolean isOccupied(int x, int y) {
        if (player != null) {
            if ((player.ax == x && player.ay == y) || (player.bx == x && player.by == y)) return true;
        }
        for (Robot r : robots) {
            if (r.x == x && r.y == y) return true;
        }
        for (Treasure t : treasures) {
            if (t.x == x && t.y == y) return true;
        }
        return false;
    }

    private int[] randomFreeCell() {
        while (true) {
            int x = 1 + rand.nextInt(Coard.COLS - 2);
            int y = 1 + rand.nextInt(Coard.ROWS - 2);
            if (coard.isWall(x, y)) continue;
            if (isOccupied(x, y)) continue;
            return new int[]{x, y};
        }
    }

    private void sleepMs(long ms) {
        long end = System.nanoTime() + ms * 1_000_000L;
        while (System.nanoTime() < end) { }
    }

    private void checkPlayerHarming() {
        int[][] neighbors = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};

        for (Robot robot : robots) {
            for (int[] dir : neighbors) {
                int checkX = player.ax + dir[0];
                int checkY = player.ay + dir[1];
                if (robot.x == checkX && robot.y == checkY) {
                    player.hp -= 50;

                    if (player.hp <= 0) {
                        player.hp = 0;
                        updateHUD();
                        handleGameOver();
                    }
                    return;
                }
            }
        }
    }

    private void handleGameOver() {
        for (int r = 0; r < 23; r++) {
            for (int c = 0; c < 80; c++) {
                cn.getTextWindow().output(c, r, ' ');
            }
        }
        String title  = "==========================";
        String msg    = "        GAME OVER         ";
        String score  = "  Final Player Score:   " + player.score;
        String cScore = "  Final Comp. Score:   " + computerScore;
        String exit   = "Press any key to exit...";

        int startY = 8;
        int startX = 15;
        writeText(startX, startY,     title);
        writeText(startX, startY + 1, msg);
        writeText(startX, startY + 2, title);
        writeText(startX, startY + 4, score);
        writeText(startX, startY + 5, cScore);
        writeText(startX, startY + 8, exit);

        keypr = 0;
        while (keypr == 0) {
            sleepMs(100);
        }
        System.exit(0);
    }
}