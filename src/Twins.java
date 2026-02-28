import enigma.console.Console;
import enigma.core.Enigma;
import enigma.event.TextMouseEvent;
import enigma.event.TextMouseListener;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Random;

// Oyunun ana game loop'unu yöneten class. Input, timing, rendering ve tüm objelerin koordinasyonu burada yapılır.
public class Twins {
    Console cn;
    Coard coard;
    Player player;

    // ArrayList yerine sabit boyutlu diziler kullanıyoruz
    Robot[] robots = new Robot[10];
    int robotCount = 0;

    Treasure[] treasures = new Treasure[50];
    int treasureCount = 0;

    // Bilgisayarın (robotların) toplam skoru
    int computerScore = 0;

    Random rand = new Random();

    int keypr, rkey;
    int robotTimer = 0;

    // HUD sağ tarafta yazacağımız başlangıç X'i (maze 0..52)
    private static final int HUD_X = 55;

    public Twins(int mode) {
        cn = Enigma.getConsole("Twins");
        coard = new Coard(mode);

        // Player A/B yerleşimi
        int[] InitialPos = randomFreeCell();

        player = new Player(InitialPos[0], InitialPos[1], InitialPos[0], InitialPos[1]);

        // 3 C-Robot ve 3 X-Robot ekle
        for (int i = 0; i < 3; i++) {
            int[] p = randomFreeCell();
            robots[robotCount++] = new Robot(p[0], p[1], 'C');
        }
        for (int i = 0; i < 3; i++) {
            int[] p = randomFreeCell();
            robots[robotCount++] = new Robot(p[0], p[1], 'X');
        }

        // Başlangıç için birkaç treasure koy (test edebilmek için).
        // (Input system'i ayrı bir task; burada sadece treasure toplama/puanlamayı çalışır hale getiriyoruz.)
        for (int i = 0; i < 2; i++) placeTreasureRandom('1');
        for (int i = 0; i < 2; i++) placeTreasureRandom('2');
        for (int i = 0; i < 2; i++) placeTreasureRandom('3');

        setupInput();
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
        // Maze'i çiz
        for (int r = 0; r < Coard.ROWS; r++)
            for (int c = 0; c < Coard.COLS; c++)
                cn.getTextWindow().output(c, r, coard.grid[r][c]);

        // Treasure'ları çiz (grid zaten içeriyor ama garanti olsun)
        for (int i = 0; i < treasureCount; i++) {
            cn.getTextWindow().output(treasures[i].x, treasures[i].y, treasures[i].symbol);
        }

        // Robotları çiz
        for (int i = 0; i < robotCount; i++)
            cn.getTextWindow().output(robots[i].x, robots[i].y, robots[i].type);

        // Player'ı çiz
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
                    // Treasure toplama (A veya B)
                    handlePlayerTreasureCollection();
                    drawPlayer();
                    updateHUD();
                } else if (rkey == KeyEvent.VK_M) {
                    player.mode *= -1;
                    drawPlayer();
                }

                keypr = 0;
            }

            // --- ROBOT HAREKETİ (her 4 time unit) ---
            robotTimer++;
            if (robotTimer >= 4) {
                for (int i = 0; i < robotCount; i++) {
                    Robot robot = robots[i];
                    if (robot.x == player.ax && robot.y == player.ay) {
                        drawPlayer(); // Redraw player if they were under the robot
                    } else if (robot.x == player.bx && robot.y == player.by) {
                        drawPlayer();
                    } else {
                        // Otherwise, restore the maze element (space, wall, or treasure)
                        cn.getTextWindow().output(robot.x, robot.y, coard.grid[robot.y][robot.x]);
                    }

                    robot.moveRandom(coard);

                    // Robot treasure'a geldiyse puan al + treasure sil
                    handleRobotTreasureCollection(robot);

                    // Robotu sadece player'ın altında değilse çiz
                    if (!(robot.x == player.ax && robot.y == player.ay)) {
                        cn.getTextWindow().output(robot.x, robot.y, robot.type);
                    }
                }
                robotTimer = 0;
                checkPlayerHarming();
                updateHUD();
            }

            sleepMs(50);
        }
    }

    // --- TREASURE TOPLAMA / PUANLAMA ---

    private void handlePlayerTreasureCollection() {
        // A ve B aynı karedeyse tek kere kontrol edelim
        collectTreasureAt(player.ax, player.ay, false);
        if (!(player.ax == player.bx && player.ay == player.by)) {
            collectTreasureAt(player.bx, player.by, false);
        }
    }

    private void handleRobotTreasureCollection(Robot robot) {
        collectTreasureAt(robot.x, robot.y, true);
    }

    // byComputer=false -> Player puanı, true -> Computer puanı (3x)
    private void collectTreasureAt(int x, int y, boolean byComputer) {
        // grid'de treasure sembolü var mı?
        char cell = coard.getCoordinate(y, x);
        if (cell != '1' && cell != '2' && cell != '3') return;

        // Treasure objesini bul (dizi içinde arama yapıyoruz)
        int foundIdx = -1;
        for (int i = 0; i < treasureCount; i++) {
            if (treasures[i].x == x && treasures[i].y == y) { foundIdx = i; break; }
        }

        // Eğer listede yoksa da sembolden puan hesaplayıp devam edelim
        int pPoints;
        int cPoints;
        if (foundIdx != -1) {
            pPoints = treasures[foundIdx].playerPoints;
            cPoints = treasures[foundIdx].computerPoints;
        } else {
            Treasure tmp = new Treasure(x, y, cell);
            pPoints = tmp.playerPoints;
            cPoints = tmp.computerPoints;
        }

        if (byComputer) computerScore += cPoints;
        else player.score += pPoints;

        // Treasure'ı sil (grid'i boşalt, diziden çıkar, ekrandan temizle)
        coard.grid[y][x] = ' ';
        cn.getTextWindow().output(x, y, ' ');
        if (foundIdx != -1) {
            // Silinen elemanın yerine son elemanı taşı, sayacı azalt
            treasures[foundIdx] = treasures[treasureCount - 1];
            treasures[treasureCount - 1] = null;
            treasureCount--;
        }
    }

    private void placeTreasureRandom(char symbol) {
        int[] p = randomFreeCell();
        Treasure t = new Treasure(p[0], p[1], symbol);
        treasures[treasureCount++] = t;
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

        // A/B aynı karedeyse sadece A gösterilecek şekilde isteniyorsa:
        // Şimdilik ikisini de basıyoruz; üst üste gelirse A son yazıldığı için A görünür.
        cn.getTextWindow().output(player.bx, player.by, 'B', attr);
        cn.getTextWindow().output(player.ax, player.ay, 'A', attr);
    }

    private void clearPlayer() {
        cn.getTextWindow().output(player.ax, player.ay, coard.grid[player.ay][player.ax]);
        cn.getTextWindow().output(player.bx, player.by, coard.grid[player.by][player.bx]);
    }

    private void updateHUD() {
        // Sağda basit bir HUD: Player ve Computer skorları
        writeText(HUD_X, 2, "P.Score : " + player.score + "   ");
        writeText(HUD_X, 3, "P.Life  : " + player.hp + "   ");
        writeText(HUD_X, 4, "C.Score : " + computerScore + "   ");

    }

    private void writeText(int x, int y, String s) {
        for (int i = 0; i < s.length(); i++) {
            cn.getTextWindow().output(x + i, y, s.charAt(i));
        }
    }

    // --- POSITION HELPERS ---

    private boolean isOccupied(int x, int y) {
        // Player
        if (player != null) {
            if ((player.ax == x && player.ay == y) || (player.bx == x && player.by == y)) return true;
        }
        // Robots
        for (int i = 0; i < robotCount; i++) {
            if (robots[i].x == x && robots[i].y == y) return true;
        }
        // Treasures
        for (int i = 0; i < treasureCount; i++) {
            if (treasures[i].x == x && treasures[i].y == y) return true;
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

    // Exception kullanmadan basit gecikme (busy-wait). Oyun loop'unda sleep kullanmak için Thread.sleep() yerine bunu kullanıyoruz.
    private void sleepMs(long ms) {
        long end = System.nanoTime() + ms * 1_000_000L;
        while (System.nanoTime() < end) {
            // spin
        }
    }
    private void checkPlayerHarming()
    {
        int[][] neighbors = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};

        for (int i = 0; i < robotCount; i++)
        {
            Robot robot = robots[i];
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
    private void handleGameOver()
    {
        for (int r = 0; r < 23; r++) {
            for (int c = 0; c < 80; c++) {
                cn.getTextWindow().output(c, r, ' ');
            }
        }
        String title = "==========================";
        String msg   = "        GAME OVER         ";
        String score = "  Final Player Score:   " + player.score;
        String cScore = "  Final Comp. Score:   " + computerScore;
        String exit  = "Press any key to exit...";

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