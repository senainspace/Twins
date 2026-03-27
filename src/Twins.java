import enigma.console.Console;
import enigma.core.Enigma;
import enigma.event.TextMouseEvent;
import enigma.event.TextMouseListener;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Random;
import java.util.Scanner;

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
    boolean paused = false;
    boolean welcomeDone = false;
    Laser[] lasers = new Laser[200];
    int laserCount = 0;
    int laserHeadX = -1, laserHeadY = -1;
    int laserEndX = -1, laserEndY = -1;
    boolean laserFire = false;
    private int[] laserPathX;
    private int[] laserPathY;
    private int laserPathIndex;

    // HUD sağ tarafta başlayacağı X konumu (maze 0..52 arası)
    private static final int HUD_X = 55;

    // Realtime saniye göstergesi için başlangıç zamanı
    private long startTimeMs;

    // Input elemanlarının ağırlıkları: 1→2, 2→2, 3→2, @→3, C→1, X→1 (toplam = 11)
    // @ yerleştirdik ama laser paketi olarak sadece görsel amaçlı kullanacağız.
    private static final char[] INPUT_ELEMENTS = {'1', '2', '3', '@', 'C', 'X'};
    private static final int[]  INPUT_WEIGHTS  = { 2,   2,   2,   3,   1,   1 };
    private static final int    WEIGHT_TOTAL   = 11;

    public Twins() {
        // Başlangıç menüsü
        Scanner sc = new Scanner(System.in);
        System.out.println("#### TWINS GAME ####");
        System.out.println("1. Load Maze from maze.txt");
        System.out.println("2. Generate Random Maze");
        System.out.print("Select an option: ");
        int mode = sc.nextInt();
        sc.nextLine(); // nextInt buffer'da bıraktığı newline'ı temizle

        // 1 veya 2 dışında bir şey girilirse tekrar sor
        while (mode != 1 && mode != 2) {
            System.out.println("Invalid input! Please enter 1 or 2.");
            System.out.print("Select an option: ");
            mode = sc.nextInt();
            sc.nextLine();
        }

        cn = Enigma.getConsole("Twins", 80, 24, 24);
        coard = new Coard(mode);

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
                enigma.console.TextAttributes attr = new enigma.console.TextAttributes(Color.ORANGE, java.awt.Color.BLACK);
                cn.getTextWindow().setCursorPosition(p[0], p[1]);
                cn.getTextWindow().output(element, attr);
            } else {
                // '@' – laser paketi: şimdilik sadece grid'e işaretle
                coard.grid[p[1]][p[0]] = element;
                cn.getTextWindow().setCursorPosition(p[0], p[1]);
                cn.getTextWindow().output(element); //aaaaaaaaaaa
            }
        }
    } //aaa sdfd

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
                if (e.getKeyCode() == KeyEvent.VK_ENTER && !welcomeDone) {
                    // Karşılama ekranı: ENTER'ı keypr'a yönlendir
                    if (keypr == 0) { keypr = 1; rkey = e.getKeyCode(); }
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_ENTER && !paused) {
                    // Oyun içinde ENTER: pause aç
                    paused = true;
                    return;
                }
                // Paused menüde veya diğer tuşlar: keypr'a düşür (UP/DOWN/ENTER seçim için)
                if (keypr == 0) { keypr = 1; rkey = e.getKeyCode(); }
            }
            public void keyReleased(KeyEvent e) {}
        });
    }

    public void run() throws InterruptedException {
        // Karşılama ekranı
        showWelcomeScreen();

        // Karşılama ekranı kapandıktan sonra süreyi başlat
        startTimeMs = System.currentTimeMillis();

        // Maze'i çiz
        for (int r = 0; r < Coard.ROWS; r++)
            for (int c = 0; c < Coard.COLS; c++) {
                cn.getTextWindow().setCursorPosition(c, r); cn.getTextWindow().output(coard.grid[r][c]);
            }

        // Treasure'ları çiz
        for (int i = 0; i < treasureCount; i++) {
            enigma.console.TextAttributes attr = new enigma.console.TextAttributes(Color.ORANGE, java.awt.Color.BLACK);
            cn.getTextWindow().setCursorPosition(treasures[i].x, treasures[i].y);
            cn.getTextWindow().output(treasures[i].symbol, attr);
        }

        // Robotları çiz
        for (int i = 0; i < robotCount; i++) {
            drawRobot(robots[i]);
        }

        // Player'ı çiz
        drawPlayer();

        updateHUD();

        // Oyun döngüsü — her tick 50ms, yani 1 saniye = 20 tick
        // Robotlar her 4 tickte bir hareket eder (200ms)
        // Yeni eleman her 20 tickte bir spawn olur (1 saniye)
        boolean wasPaused = false;
        while (true) {
            if (paused) {
                if (!wasPaused) {
                    boolean devamEt = showPausedScreen();
                    wasPaused = true;
                    if (!devamEt) {
                        System.exit(0);
                    }
                    // Devam et seçildiyse paused'u kapat
                    paused = false;
                }
                Thread.sleep(50);
                continue;
            }
            if (wasPaused) {
                clearPausedScreen();
                wasPaused = false;
            }

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
                    drawTreasures();
                    drawPlayer();
                } else if (rkey == KeyEvent.VK_M) {
                    player.mode *= -1;
                    drawPlayer();
                } else if (rkey == KeyEvent.VK_SPACE) {
                    if (player.laserCount > 0 && !laserFire) {
                        startLaser();
                    }
                }

                keypr = 0;
            }

            // --- LASER GÜNCELLEMESİ ---
            updateLaser();

            // --- ROBOT HAREKETİ (her 4 tickte bir = 200ms) ---
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
                    robot.step(coard, robots, robotCount, player.ax, player.ay, player.bx, player.by, treasures, treasureCount);

                    // Robot treasure'a bastıysa puan ekle ve treasure'ı kaldır
                    handleRobotTreasureCollection(robot);

                    // Robotu player A'nın üstünde değilse çiz
                    if (!(robot.x == player.ax && robot.y == player.ay)) {
                        drawRobot(robot);
                    }
                }
                robotTimer = 0;
                checkPlayerHarming();
            }

            // --- OYUN INPUT SİSTEMİ (her 20 tickte bir = 1 saniye) ---
            inputTimer++;
            if (inputTimer >= 20) {
                spawnInputElement();
                inputTimer = 0;
            }

            // HUD her tick sonunda bir kez güncellenir
            updateHUD();

            Thread.sleep(50);
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
        if (cell == '@' && !byComputer) {
            player.laserCount++;
            coard.grid[y][x] = ' ';
            cn.getTextWindow().setCursorPosition(x, y);
            cn.getTextWindow().output(' ');
            return;
        }
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

    private void drawTreasures() {
        enigma.console.TextAttributes orangeAttr = new enigma.console.TextAttributes(java.awt.Color.ORANGE, java.awt.Color.BLACK);
        for (int r = 0; r < Coard.ROWS; r++) {
            for (int c = 0; c < Coard.COLS; c++) {
                char cell = coard.grid[r][c];
                if (cell == '1' || cell == '2' || cell == '3') {
                    cn.getTextWindow().setCursorPosition(c, r);
                    cn.getTextWindow().output(cell, orangeAttr);
                }
            }
        }
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

    private void writeColored(int x, int y, String s, java.awt.Color fg) {
        enigma.console.TextAttributes attr = new enigma.console.TextAttributes(fg, java.awt.Color.BLACK);
        for (int i = 0; i < s.length(); i++) {
            cn.getTextWindow().setCursorPosition(x + i, y);
            cn.getTextWindow().output(s.charAt(i), attr);
        }
    }

    // Oyun başlamadan önce karşılama ekranını gösterir, Enter'a basınca devam eder
    private void showWelcomeScreen() {
        for (int r = 0; r < 24; r++)
            for (int c = 0; c < 80; c++) {
                cn.getTextWindow().setCursorPosition(c, r); cn.getTextWindow().output(' ');
            }

        // TWINS - 5x5 piksel grid, x=25 ile tam ortada
        writeColored(25, 3, "ooooo o   o  ooo  o   o  oooo", java.awt.Color.CYAN);
        writeColored(25, 4, "  o   o   o   o   oo  o o    ", java.awt.Color.CYAN);
        writeColored(25, 5, "  o   o o o   o   o o o  ooo ", java.awt.Color.CYAN);
        writeColored(25, 6, "  o   o o o   o   o  oo     o", java.awt.Color.CYAN);
        writeColored(25, 7, "  o    o o   ooo  o   o oooo ", java.awt.Color.CYAN);

        writeColored(26, 10, "~ Twin Heroes of the Maze ~", java.awt.Color.YELLOW);

        writeColored(22, 12, "CONTROLS", java.awt.Color.GREEN);
        writeText   (22, 13, "  Arrow Keys  : Move");
        writeText   (22, 14, "  M           : Toggle twin mode (same / opposite)");
        writeText   (22, 15, "  SPACE       : Fire laser");
        writeText   (22, 16, "  ENTER       : Pause / Resume");

        writeColored(25, 19, "Press ENTER to start...", java.awt.Color.WHITE);

        keypr = 0;
        while (true) {
            if (keypr == 1 && rkey == KeyEvent.VK_ENTER) break;
            else { keypr = 0;}
            try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        keypr = 0;
        welcomeDone = true;

        // Karşılama ekranını temizle
        for (int r = 0; r < 24; r++)
            for (int c = 0; c < 80; c++) {
                cn.getTextWindow().setCursorPosition(c, r); cn.getTextWindow().output(' ');
            }
    }

    // Ekranın ortasına PAUSED menüsü yazar, seçim döngüsü burada
    // Döner: true = devam et, false = çık
    private boolean showPausedScreen() {
        int selected = 0; // 0 = Devam Et, 1 = Cik

        while (true) {
            writeColored(28, 9,  "+---------------------+", java.awt.Color.YELLOW);
            writeColored(28, 10, "|       PAUSED        |", java.awt.Color.YELLOW);
            writeColored(28, 11, "+---------------------+", java.awt.Color.YELLOW);

            if (selected == 0) {
                writeColored(28, 12, "|  > Devam Et         |", java.awt.Color.GREEN);
                writeColored(28, 13, "|    Cik              |", java.awt.Color.YELLOW);
            } else {
                writeColored(28, 12, "|    Devam Et         |", java.awt.Color.YELLOW);
                writeColored(28, 13, "|  > Cik              |", java.awt.Color.RED);
            }

            writeColored(28, 14, "+---------------------+", java.awt.Color.YELLOW);

            // tuş bekle
            keypr = 0;
            while (keypr == 0) {
                try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }

            if (rkey == java.awt.event.KeyEvent.VK_UP || rkey == java.awt.event.KeyEvent.VK_DOWN) {
                selected = (selected == 0) ? 1 : 0;
            } else if (rkey == java.awt.event.KeyEvent.VK_ENTER) {
                keypr = 0;
                return selected == 0; // true = devam et
            }
            keypr = 0;
        }
    }

    // Pause menüsü kapandıktan sonra tüm ekranı yeniden çizer
    private void clearPausedScreen() {
        // Maze'i komple yeniden çiz
        for (int r = 0; r < Coard.ROWS; r++)
            for (int c = 0; c < Coard.COLS; c++) {
                cn.getTextWindow().setCursorPosition(c, r);
                cn.getTextWindow().output(coard.grid[r][c]);
            }
        // Treasure'ları çiz
        enigma.console.TextAttributes orangeAttr = new enigma.console.TextAttributes(java.awt.Color.ORANGE, java.awt.Color.BLACK);
        for (int i = 0; i < treasureCount; i++) {
            cn.getTextWindow().setCursorPosition(treasures[i].x, treasures[i].y);
            cn.getTextWindow().output(treasures[i].symbol, orangeAttr);
        }
        // Robotları çiz
        for (int i = 0; i < robotCount; i++) drawRobot(robots[i]);
        // Player'ı çiz
        drawPlayer();
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
        for (int r = 0; r < 24; r++)
            for (int c = 0; c < 80; c++) {
                cn.getTextWindow().setCursorPosition(c, r); cn.getTextWindow().output(' ');
            }

        int startX = 15, startY = 6;

        writeColored(startX, startY,     "==========================", java.awt.Color.RED);
        writeColored(startX, startY + 1, "        GAME  OVER        ", java.awt.Color.RED);
        writeColored(startX, startY + 2, "==========================", java.awt.Color.RED);

        writeColored(startX, startY + 4, "  Final Player Score : ", java.awt.Color.GREEN);
        writeColored(startX + 23, startY + 4, String.valueOf(player.score), java.awt.Color.WHITE);

        writeColored(startX, startY + 5, "  Final Comp.  Score : ", java.awt.Color.CYAN);
        writeColored(startX + 23, startY + 5, String.valueOf(computerScore), java.awt.Color.WHITE);

        // Kazananı göster
        String winner;
        java.awt.Color winColor;
        if (player.score > computerScore) {
            winner = "  >> Player wins! <<";
            winColor = java.awt.Color.GREEN;
        } else if (computerScore > player.score) {
            winner = "  >> Computer wins! <<";
            winColor = java.awt.Color.RED;
        } else {
            winner = "  >> It's a tie! <<";
            winColor = java.awt.Color.YELLOW;
        }
        writeColored(startX, startY + 7, winner, winColor);

        writeColored(startX, startY + 10, "Press any key to exit...", java.awt.Color.GRAY);

        // Önceki tuş basımının temizlenmesi için bekle, sonra yeni input bekle
        try { Thread.sleep(300); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        keypr = 0;
        while (keypr == 0) {
            try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
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

    private void startLaser() {
        if (player.ax == player.bx && player.ay == player.by) {
            return;
        }

        player.laserCount--;

        laserFire = true;
        laserEndX = player.bx;
        laserEndY = player.by;
        laserHeadX = player.ax;
        laserHeadY = player.ay;
        computeLaserPath();
    }

    private void computeLaserPath() {
        int dx = Math.abs(laserEndX - laserHeadX);
        int dy = Math.abs(laserEndY - laserHeadY);
        int totalSteps = dx + dy;
        laserPathX = new int[totalSteps];
        laserPathY = new int[totalSteps];
        laserPathIndex  = 0;

        int cx  = laserHeadX;
        int cy  = laserHeadY;
        int sx  = Integer.signum(laserEndX - laserHeadX);
        int sy  = Integer.signum(laserEndY - laserHeadY);
        int err = dx - dy;

        for (int i = 0; i < totalSteps; i++) {
            if (err >= 0 && cx != laserEndX) {
                cx += sx;
                err -= dy;
            } else if (cy != laserEndY) {
                cy += sy;
                err += dx;
            } else {
                cx += sx;
                err -= dy;
            }
            laserPathX[i] = cx;
            laserPathY[i] = cy;
        }
    }

    private void updateLaser() {
        if (laserFire) {
            if (laserHeadX == laserEndX && laserHeadY == laserEndY) {
                laserFire = false;
            } else {
                int nextX = laserPathX[laserPathIndex];
                int nextY = laserPathY[laserPathIndex];
                laserPathIndex++;

                addLaser(new Laser(nextX, nextY));

                if (coard.grid[nextY][nextX] == ' ' && !(nextX == player.bx && nextY == player.by)) {
                    cn.getTextWindow().setCursorPosition(nextX, nextY);
                    enigma.console.TextAttributes laserAttr = new enigma.console.TextAttributes(java.awt.Color.CYAN, java.awt.Color.BLACK);
                    cn.getTextWindow().output('+', laserAttr);
                }

                laserHeadX = nextX;
                laserHeadY = nextY;
            }
        }

        for (int i = laserCount - 1; i >= 0; i--) {
            lasers[i].lifetime--;
            if (lasers[i].lifetime <= 0) {
                int lx = lasers[i].x;
                int ly = lasers[i].y;

                if (coard.grid[ly][lx] == ' ' && !(lx == player.bx && ly == player.by)) {
                    cn.getTextWindow().setCursorPosition(lx, ly);
                    cn.getTextWindow().output(' ');
                }

                lasers[i] = lasers[laserCount - 1];
                lasers[laserCount - 1] = null;
                laserCount--;
            }
        }

        applyLaserDamage();
    }

    private void applyLaserDamage() {
        for (int i = 0; i < laserCount; i++) {
            for (int j = robotCount - 1; j >= 0; j--) {
                Robot robot = robots[j];
                if (robot == null) continue;

                if (Math.abs(robot.x - lasers[i].x) + Math.abs(robot.y - lasers[i].y) == 1) {
                    robot.hp -= 50;

                    if (robot.hp <= 0) {
                        cn.getTextWindow().setCursorPosition(robot.x, robot.y);
                        cn.getTextWindow().output(coard.grid[robot.y][robot.x]);

                        player.score += 100;

                        robots[j] = robots[robotCount - 1];
                        robots[robotCount - 1] = null;
                        robotCount--;
                    }
                }
            }
        }
    }

    private void addLaser(Laser lb) {
        if (laserCount == lasers.length) {
            Laser[] newArr = new Laser[lasers.length * 2];
            System.arraycopy(lasers, 0, newArr, 0, laserCount);
            lasers = newArr;
        }
        lasers[laserCount++] = lb;
    }
}