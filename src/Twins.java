import enigma.console.Console;
import enigma.core.Enigma;
import enigma.event.TextMouseEvent;
import enigma.event.TextMouseListener;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Random;

// Oyunun ana game loop'unu yöneten class. Input, timing, rendering ve tüm objelerin koordinasyonu burada yapılır.
public class Twins {
    Console cn;
    Coard coard;
    Player player;
    ArrayList<Robot> robots;
    Random rand = new Random();

    int keypr, rkey;
    int robotTimer = 0;

    public Twins() throws Exception {
        cn = Enigma.getConsole("Twins");
        coard = new Coard();
        robots = new ArrayList<Robot>();

        int[] posA = randomEmpty();
        int[] posB = randomEmpty();
        player = new Player(posA[0], posA[1], posB[0], posB[1]);

        // 3 C-Robot ve 3 X-Robot ekle
        for (int i = 0; i < 3; i++) {
            int[] p = randomEmpty();
            robots.add(new Robot(p[0], p[1], 'C'));
        }
        for (int i = 0; i < 3; i++) {
            int[] p = randomEmpty();
            robots.add(new Robot(p[0], p[1], 'X'));
        }

        setupInput();
    }

    private void setupInput() {
        cn.getTextWindow().addTextMouseListener(new TextMouseListener() {
            public void mouseClicked(TextMouseEvent e) {
            }

            public void mousePressed(TextMouseEvent e) {
            }

            public void mouseReleased(TextMouseEvent e) {
            }
        });
        cn.getTextWindow().addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent e) {
            }

            public void keyPressed(KeyEvent e) {
                if (keypr == 0) {
                    keypr = 1;
                    rkey = e.getKeyCode();
                }
            }

            public void keyReleased(KeyEvent e) {
            }
        });
    }

    public void run() throws Exception {
        // Maze'i çiz
        for (int r = 0; r < Coard.ROWS; r++)
            for (int c = 0; c < Coard.COLS; c++)
                cn.getTextWindow().output(c, r, coard.grid[r][c]);

        // Robotları çiz
        for (Robot robot : robots)
            cn.getTextWindow().output(robot.x, robot.y, robot.type);

        // Player'ı çiz (A ve B aynı kareden başlar)
        drawPlayer();

        // Game loop
        while (true) {
            // --- PLAYER INPUT ---
            if (keypr == 1) {
                int dx = 0, dy = 0;

                if (rkey == KeyEvent.VK_LEFT) dx = -1;
                else if (rkey == KeyEvent.VK_RIGHT) dx = 1;
                else if (rkey == KeyEvent.VK_UP) dy = -1;
                else if (rkey == KeyEvent.VK_DOWN) dy = 1;

                if (dx != 0 || dy != 0) {
                    clearPlayer();
                    player.move(dx, dy, coard);
                    drawPlayer();
                } else if (rkey == KeyEvent.VK_M) {
                    player.mode *= -1;
                    drawPlayer();
                } else if (rkey == KeyEvent.VK_L) {
                    fireLaser();
                }

                keypr = 0;
            }

            // --- ROBOT HAREKETİ (her 4 time unit) ---
            robotTimer++;
            if (robotTimer >= 4) {
                for (Robot robot : robots) {
                    cn.getTextWindow().output(robot.x, robot.y, ' ');
                    robot.moveRandom(coard);
                    cn.getTextWindow().output(robot.x, robot.y, robot.type);
                }
                robotTimer = 0;
            }

            Thread.sleep(50);
        }
    }

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

    private int[] randomEmpty() {
        while (true) {
            int x = 1 + rand.nextInt(Coard.COLS - 2);
            int y = 1 + rand.nextInt(Coard.ROWS - 2);
            if (!coard.isWall(x, y)) return new int[]{x, y};
        }
    }

    private void fireLaser() throws Exception {
        Laser laser = new Laser(player.ax, player.ay, player.bx, player.by);
        // is a and b same row
        if (laser.y1 == laser.y2) {
            //lasers start & end coard
            int start = Math.min(laser.x1, laser.x2);
            int end = Math.max(laser.x1, laser.x2);

            for (int x = start + 1; x < end; x++) {
                if (coard.isWall(x, laser.y1))
                    break;
                cn.getTextWindow().output(x, laser.y1, '+');
                for (Robot robot : robots) {
                    if (robot.x == x && robot.y == laser.y1) {
                        robot.hp -= 100;
                    }
                }


            }
            Thread.sleep(150);

            //bring back
            for (int x = start + 1; x < end; x++) {
                if (coard.isWall(x, laser.y1))
                    break;
                cn.getTextWindow().output(x, laser.y1, coard.grid[laser.y1][x]);
            }
        }
        // is a and b same column
        else if (laser.x1 == laser.x2) {
            int start = Math.min(laser.y1, laser.y2);
            int end = Math.max(laser.y1, laser.y2);
            for (int y = start + 1; y < end; y++) {
                if (coard.isWall(laser.x1, y))
                    break;
                cn.getTextWindow().output(laser.x1, y, '+');
                for (Robot robot : robots) {
                    if (robot.x == laser.x1 && robot.y == y) {
                        robot.hp -= 100;
                    }
                }
            }
            Thread.sleep(150);

            // bring back
            for (int y = start + 1; y < end; y++) {
                if (coard.isWall(laser.x1, y))
                    break;
                cn.getTextWindow().output(laser.x1, y, coard.grid[y][laser.x1]);

            }

        }
    }
}
