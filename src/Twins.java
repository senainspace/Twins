import enigma.console.Console;
import enigma.core.Enigma;
import enigma.event.TextMouseEvent;
import enigma.event.TextMouseListener;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Random;

public class Twins {
    // Oyunun ana game loop'unu yöneten class. Input, timing, rendering ve tüm objelerin koordinasyonu burada yapılır.
    Console cn;
    Coard coard;
    ArrayList<Robot> robots;
    Random rand = new Random();

    int robotTimer = 0;

    public Twins() throws Exception {
        cn = Enigma.getConsole("Twins");
        coard = new Coard();
        robots = new ArrayList<Robot>();

        // 3 C-Robot ve 3 X-Robot ekle
        for (int i = 0; i < 3; i++) {
            int[] pos = randomEmpty();
            robots.add(new Robot(pos[0], pos[1], 'C'));
        }
        for (int i = 0; i < 3; i++) {
            int[] pos = randomEmpty();
            robots.add(new Robot(pos[0], pos[1], 'X'));
        }
    }

    public void run() throws Exception {
        // Mazeyi çiz
        for (int r = 0; r < Coard.ROWS; r++)
            for (int c = 0; c < Coard.COLS; c++)
                cn.getTextWindow().output(c, r, coard.grid[r][c]);

        // Robotları çiz
        for (Robot robot : robots)
            cn.getTextWindow().output(robot.x, robot.y, robot.type);

        // Game loop
        while (true) {
            robotTimer++;
            if (robotTimer >= 4) {
                for (Robot robot : robots) {
                    // Eski pozisyonu temizle
                    cn.getTextWindow().output(robot.x, robot.y, ' ');
                    // Hareket et
                    robot.moveRandom(coard);
                    // Yeni pozisyonu çiz
                    cn.getTextWindow().output(robot.x, robot.y, robot.type);
                }
                robotTimer = 0;
            }

            Thread.sleep(50);
        }
    }

    private int[] randomEmpty() {
        while (true) {
            int x = 1 + rand.nextInt(Coard.COLS - 2);
            int y = 1 + rand.nextInt(Coard.ROWS - 2);
            if (!coard.isWall(x, y)) return new int[]{x, y};
        }
    }
}