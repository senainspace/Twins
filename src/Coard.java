// Coard.java
import java.util.Random;

public class Coard {
    // 23x53'lük game board'u tutan class. Maze generate etme ve duvar kontrolü buradadır.
    public static final int ROWS = 23;
    public static final int COLS = 53;
    public char[][] grid;

    public Coard() {
        grid = new char[ROWS][COLS];
        generate();
    }

    private void generate() {
        // Her şeyi önce boşlukla doldur
        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < COLS; c++)
                grid[r][c] = ' ';

        // Dış duvarlar
        for (int c = 0; c < COLS; c++) { grid[0][c] = '#'; grid[ROWS-1][c] = '#'; }
        for (int r = 0; r < ROWS; r++) { grid[r][0] = '#'; grid[r][COLS-1] = '#'; }

        // Basit iç duvarlar (şimdilik random, hafta 4'te düzgün yapılacak)
        Random rand = new Random();
        for (int i = 0; i < 60; i++) {
            int r = 1 + rand.nextInt(ROWS - 2);
            int c = 1 + rand.nextInt(COLS - 2);
            grid[r][c] = '#';
        }
    }

    public boolean isWall(int x, int y) {
        if (x < 0 || x >= COLS || y < 0 || y >= ROWS) return true;
        return grid[y][x] == '#';
    }
}