import enigma.console.Console;
import enigma.core.Enigma;
import enigma.event.TextMouseEvent;
import enigma.event.TextMouseListener;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Random;

// Oyunun ana döngüsünü yöneten class. Input, zamanlama, çizim ve tüm objelerin koordinasyonu burada yapılır.
// Değiştirildi: gettextwindow kullanırken koordinatı direkt output'a vermek yerine cursor pozisyonu ayarlayarak yazdık.
// Değiştirildi: robotlar ve player aynı kareye gelemez, isOccupied ve isRobotAt metodlarıyla hareket sırasında kontrol edildi.
public class Twins {
    Console cn;
    Coard coard;
    Player player;

    Robot[]    robots    = new Robot[50];
    int        robotCount = 0;

    Treasure[] treasures    = new Treasure[100];
    int        treasureCount = 0;

    // Bilgisayarın (robotların) toplam skoru
    int computerScore = 0;

    Random rand = new Random();

    int keypr, rkey;
    int robotTimer = 0;
    int inputTimer = 0; // Oyun input sistemi için sayaç (her 20 birimde yeni eleman)

    // HUD sağ tarafta başlayacağı X konumu (maze 0..52 arası)
    private static final int HUD_X = 55;

    // Realtime saniye göstergesi için başlangıç zamanı
    private long startTimeMs;

    // Input elemanlarının ağırlıkları: 1→2, 2→2, 3→2, @→3, C→1, X→1 (toplam = 11)
    // @ yerleştirdik ama laser paketi olarak sadece görsel amaçlı kullanacağız.
    private static final char[] INPUT_ELEMENTS = {'1', '2', '3', '@', 'C', 'X'};
    private static final int[]  INPUT_WEIGHTS  = { 2,   2,   2,   3,   1,   1 };
    private static final int    WEIGHT_TOTAL   = 11;

    public Twins(int mode) {
        cn = Enigma.getConsole("Twins"); //consoleu default boyut yaptık çünkü değiştirince büyüdüğünde maze bozuluyo
        coard = new Coard(mode);

        // Başlangıç zamanı (Time : saniye gösterimi için)
        startTimeMs = System.currentTimeMillis();

        // Player A/B başlangıç konumu
        int[] InitialPos = randomFreeCell();
        player = new Player(InitialPos[0], InitialPos[1], InitialPos[0], InitialPos[1]);

        // Oyun başında input sisteminin ilk 10 elemanını yerleştir
        for (int i = 0; i < 10; i++) {
            spawnInputElement();
        }

        setupInput();
    }

    // Olasılıklara göre rastgele bir eleman seçip maze'e yerleştirir.
    // Olasılıklar: 1→2/11, 2→2/11, 3→2/11, @→3/11, C→1/11, X→1/11
    // C ve X yeni robot spawn eder; diğerleri grid'e yerleştirilir.
    private void spawnInputElement() {
        char element = pickInputElement();

        if (element == 'C' || element == 'X') {
            // Yeni robot ekle
            int[] p = randomFreeCell();
            addRobot(new Robot(p[0], p[1], element));
            cn.getTextWindow().setCursorPosition(p[0], p[1]); cn.getTextWindow().output(element);
        } else {
            // Treasure veya laser paketi grid'e yerleştir
            int[] p = randomFreeCell();
            if (element == '1' || element == '2' || element == '3') {
                addTreasure(new Treasure(p[0], p[1], element));
                coard.grid[p[1]][p[0]] = element;
            } else {
                // '@' – laser paketi: şimdilik sadece grid'e işaretle
                coard.grid[p[1]][p[0]] = element;
            }
            cn.getTextWindow().setCursorPosition(p[0], p[1]); cn.getTextWindow().output(element);
        }
    }

    // INPUT_WEIGHTS ağırlıklarına göre INPUT_ELEMENTS dizisinden rastgele eleman seçer.
    private char pickInputElement() {
        int roll = rand.nextInt(WEIGHT_TOTAL); // 0..10
        int cumulative = 0;
        for (int i = 0; i < INPUT_ELEMENTS.length; i++) {
            cumulative += INPUT_WEIGHTS[i];
            if (roll < cumulative) return INPUT_ELEMENTS[i];
        }
        return INPUT_ELEMENTS[0];
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
            for (int c = 0; c < Coard.COLS; c++) {
                cn.getTextWindow().setCursorPosition(c, r); cn.getTextWindow().output(coard.grid[r][c]);
            }

        // Treasure'ları çiz
        for (int i = 0; i < treasureCount; i++) {
            cn.getTextWindow().setCursorPosition(treasures[i].x, treasures[i].y); cn.getTextWindow().output(treasures[i].symbol);
        }

        // Robotları çiz
        for (int i = 0; i < robotCount; i++) {
            drawRobot(robots[i]);
        }

        // Player'ı çiz
        drawPlayer();

        updateHUD();

        // Oyun döngüsü
        while (true) {
            // --- OYUNCU GİRİŞİ ---
            if (keypr == 1) {
                int dx = 0, dy = 0;

                if      (rkey == KeyEvent.VK_LEFT)  dx = -1;
                else if (rkey == KeyEvent.VK_RIGHT) dx =  1;
                else if (rkey == KeyEvent.VK_UP)    dy = -1;
                else if (rkey == KeyEvent.VK_DOWN)  dy =  1;

                if (dx != 0 || dy != 0) {
                    clearPlayer();
                    // robots arrayi de geçiriliyor; player robotun üstüne geçemesin diye
                    player.move(dx, dy, coard, robots, robotCount);
                    // Treasure toplama
                    handlePlayerTreasureCollection();
                    drawPlayer();
                    updateHUD();
                } else if (rkey == KeyEvent.VK_M) {
                    player.mode *= -1;
                    drawPlayer();
                }

                keypr = 0;
            }

            // --- ROBOT HAREKETİ (her 4 time unit'te bir) ---
            robotTimer++;
            if (robotTimer >= 4) {
                for (int i = 0; i < robotCount; i++) {
                    Robot robot = robots[i];
                    if (robot.x == player.ax && robot.y == player.ay) {
                        drawPlayer();
                    } else if (robot.x == player.bx && robot.y == player.by) {
                        drawPlayer();
                    } else {
                        cn.getTextWindow().setCursorPosition(robot.x, robot.y); cn.getTextWindow().output(coard.grid[robot.y][robot.x]);
                    }

                    // tüm robotlar ve player konumları verilir aynı kareye iki nesne giremez
                    robot.step(coard, robots, robotCount, player.ax, player.ay, player.bx, player.by);

                    // Robot treasure'a bastıysa puan ekle ve treasure'ı kaldır
                    handleRobotTreasureCollection(robot);

                    // Robotu player A'nın üstünde değilse çiz
                    if (!(robot.x == player.ax && robot.y == player.ay)) {
                        drawRobot(robot);
                    }
                }
                robotTimer = 0;
                checkPlayerHarming();
                updateHUD();
            }

            // --- OYUN INPUT SİSTEMİ (her 20 time unit'te bir) ---
            inputTimer++;
            if (inputTimer >= 20) {
                spawnInputElement();
                inputTimer = 0;
                updateHUD();
            }

            sleepMs(50);
        }
    }

    // --- TREASURE TOPLAMA / PUANLAMA ---

    private void handlePlayerTreasureCollection() {
        collectTreasureAt(player.ax, player.ay, false);
        if (!(player.ax == player.bx && player.ay == player.by)) {
            collectTreasureAt(player.bx, player.by, false);
        }
    }

    private void handleRobotTreasureCollection(Robot robot) {
        collectTreasureAt(robot.x, robot.y, true);
    }

    // byComputer=false -> Player puanı, true -> Bilgisayar puanı (3x)
    private void collectTreasureAt(int x, int y, boolean byComputer) {
        char cell = coard.getCoordinate(y, x);
        if (cell != '1' && cell != '2' && cell != '3') return;

        // Treasure arrayinde ilgili konumu ara
        int foundIdx = -1;
        for (int i = 0; i < treasureCount; i++) {
            if (treasures[i].x == x && treasures[i].y == y) { foundIdx = i; break; }
        }

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

        coard.grid[y][x] = ' ';
        cn.getTextWindow().setCursorPosition(x, y); cn.getTextWindow().output(' ');
        if (foundIdx != -1) {
            // Silinen elemanın yerine dizinin son elemanını taşı, sayacı azalt
            treasures[foundIdx] = treasures[treasureCount - 1];
            treasures[treasureCount - 1] = null;
            treasureCount--;
        }
    }

    private void placeTreasureRandom(char symbol) {
        int[] p = randomFreeCell();
        Treasure t = new Treasure(p[0], p[1], symbol);
        addTreasure(t);
        coard.grid[p[1]][p[0]] = symbol;
    }

    // --- ÇİZİM YARDIMCI METODLARı ---

    private void drawPlayer() {
        enigma.console.TextAttributes attr;

        if (player.mode == 1) {
            attr = new enigma.console.TextAttributes(java.awt.Color.GREEN, java.awt.Color.BLACK);
        } else {
            attr = new enigma.console.TextAttributes(java.awt.Color.MAGENTA, java.awt.Color.BLACK);
        }

        cn.getTextWindow().setCursorPosition(player.bx, player.by); cn.getTextWindow().output('B', attr);
        cn.getTextWindow().setCursorPosition(player.ax, player.ay); cn.getTextWindow().output('A', attr);
    }

    private void drawRobot(Robot robot) {
        enigma.console.TextAttributes attr = new enigma.console.TextAttributes(java.awt.Color.RED, java.awt.Color.BLACK);
        cn.getTextWindow().setCursorPosition(robot.x, robot.y);
        cn.getTextWindow().output(robot.type, attr);
    }

    private void clearPlayer() {
        cn.getTextWindow().setCursorPosition(player.ax, player.ay); cn.getTextWindow().output(coard.grid[player.ay][player.ax]);
        cn.getTextWindow().setCursorPosition(player.bx, player.by); cn.getTextWindow().output(coard.grid[player.by][player.bx]);
    }

    private void updateHUD() {
        int seconds = (int) ((System.currentTimeMillis() - startTimeMs) / 1000);

        writeText(HUD_X, 0, "Time : " + seconds + "   ");

        writeText(HUD_X, 2, "P.Score : " + player.score + "   ");
        writeText(HUD_X, 3, "P.Life  : " + player.hp + "   ");
        writeText(HUD_X, 4, "P.Laser : " + player.laserCount + "   ");

        // Aktif robot sayılarını say
        int cCount = 0, xCount = 0;
        for (int i = 0; i < robotCount; i++) {
            if (robots[i].isAlive()) {
                if (robots[i].type == 'C') cCount++;
                else if (robots[i].type == 'X') xCount++;
            }
        }

        writeText(HUD_X, 6, "C.Score : " + computerScore + "   ");
        writeText(HUD_X, 7, "C-Robots: " + cCount + "   ");
        writeText(HUD_X, 8, "X-Robots: " + xCount + "   ");
    }

    private void writeText(int x, int y, String s) {
        for (int i = 0; i < s.length(); i++) {
            cn.getTextWindow().setCursorPosition(x + i, y); cn.getTextWindow().output(s.charAt(i));
        }
    }

    // --- KONUM YARDIMCI METODLARı ---

    private boolean isOccupied(int x, int y) {
        // Player kontrolü
        if (player != null) {
            if ((player.ax == x && player.ay == y) || (player.bx == x && player.by == y)) return true;
        }
        // Robot kontrolü
        for (int i = 0; i < robotCount; i++) {
            if (robots[i].x == x && robots[i].y == y) return true;
        }
        // Treasure kontrolü
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
            // grid'de zaten bir şey varsa o kareyi atla
            if (coard.grid[y][x] != ' ') continue;
            return new int[]{x, y};
        }
    }

    private void sleepMs(long ms) {
        long end = System.nanoTime() + ms * 1_000_000L;
        while (System.nanoTime() < end) { }
    }

    private void checkPlayerHarming() {
        int[][] neighbors = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};

        for (int i = 0; i < robotCount; i++) {
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

    private void handleGameOver() {
        for (int r = 0; r < 23; r++) {
            for (int c = 0; c < 80; c++) {
                cn.getTextWindow().setCursorPosition(c, r); cn.getTextWindow().output(' ');
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

    // Robotu diziye ekler. Dizi doluysa kapasiteyi 2 katına çıkarır.
    private void addRobot(Robot r) {
        if (robotCount == robots.length) {
            Robot[] newArr = new Robot[robots.length * 2];
            System.arraycopy(robots, 0, newArr, 0, robotCount);
            robots = newArr;
        }
        robots[robotCount++] = r;
    }

    // Treasure'ı diziye ekler. Dizi doluysa kapasiteyi 2 katına çıkarır.
    private void addTreasure(Treasure t) {
        if (treasureCount == treasures.length) {
            Treasure[] newArr = new Treasure[treasures.length * 2];
            System.arraycopy(treasures, 0, newArr, 0, treasureCount);
            treasures = newArr;
        }
        treasures[treasureCount++] = t;
    }
}