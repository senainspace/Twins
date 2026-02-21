import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

// 23x53'lük game board'u tutan class. Maze generate etme ve duvar kontrolü buradadır.
public class Coard {
    public static final int ROWS = 23;
    public static final int COLS = 53;
    public char[][] grid;
    private Random random;

    public Coard() {
        grid = new char[ROWS][COLS];
        random = new Random();
        initializeGrid();
        generateRandomMaze();
    }

    private void initializeGrid() {
        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < COLS; c++)
                grid[r][c] = ' ';
    }

    private void generateRandomMaze() {
        addBorders();
        addWalls(4, 8);
        addWalls(6, 6);
        addWalls(20, 4);
        addWalls(5, 3);
    }

    private void addBorders() {
        for (int c = 0; c < COLS; c++) { grid[0][c] = '#'; grid[ROWS-1][c] = '#'; }
        for (int r = 0; r < ROWS; r++) { grid[r][0] = '#'; grid[r][COLS-1] = '#'; }
    }

    private void addWalls(int numberOfWalls, int wallLength) {
        int wallsPlaced = 0;
        int attempts = 0;
        int maxAttempts = 10000;

        while (wallsPlaced < numberOfWalls && attempts < maxAttempts) {
            attempts++;
            int r = (int)(Math.random() * ROWS);
            int c = (int)(Math.random() * COLS);
            int dir = (int)(Math.random() * 2);

            if (dir == 0 && c + wallLength > COLS - 1) continue;
            if (dir == 1 && r + wallLength > ROWS - 1) continue;

            boolean overlaps = false;
            for (int i = 0; i < wallLength; i++) {
                int dr = (dir == 1) ? i : 0;
                int dc = (dir == 0) ? i : 0;
                if (grid[r + dr][c + dc] == '#') { overlaps = true; break; }
            }
            if (overlaps) continue;

            for (int i = 0; i < wallLength; i++) {
                int dr = (dir == 1) ? i : 0;
                int dc = (dir == 0) ? i : 0;
                grid[r + dr][c + dc] = '#';
            }

            boolean isValid = checkConnected()
                    && checkArea(2, 2, 3)
                    && checkArea(3, 3, 5)
                    && checkArea(4, 4, 7)
                    && checkArea(6, 6, 15);

            if (isValid) {
                wallsPlaced++;
                attempts = 0;
            } else {
                for (int i = 0; i < wallLength; i++) {
                    int dr = (dir == 1) ? i : 0;
                    int dc = (dir == 0) ? i : 0;
                    grid[r + dr][c + dc] = ' ';
                }
            }
        }
    }

    private boolean checkArea(int width, int height, int maxWalls) {
        for (int r = 0; r <= ROWS - height; r++) {
            for (int c = 0; c <= COLS - width; c++) {
                int count = 0;
                for (int i = 0; i < height; i++)
                    for (int j = 0; j < width; j++)
                        if (grid[r+i][c+j] == '#') count++;
                if (count > maxWalls) return false;
            }
        }
        return true;
    }

    private boolean checkConnected() {
        int totalEmpty = 0;
        int startRow = -1, startCol = -1;
        boolean[][] visited = new boolean[ROWS][COLS];

        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < COLS; c++)
                if (grid[r][c] == ' ') {
                    totalEmpty++;
                    if (startRow == -1) { startRow = r; startCol = c; }
                }

        if (totalEmpty == 0) return true;

        Queue<int[]> queue = new LinkedList<int[]>();
        queue.add(new int[]{startRow, startCol});
        visited[startRow][startCol] = true;
        int reachable = 0;
        int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};

        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            reachable++;
            for (int[] d : dirs) {
                int nr = cur[0] + d[0], nc = cur[1] + d[1];
                if (nr >= 0 && nr < ROWS && nc >= 0 && nc < COLS
                        && !visited[nr][nc] && grid[nr][nc] == ' ') {
                    visited[nr][nc] = true;
                    queue.add(new int[]{nr, nc});
                }
            }
        }
        return reachable == totalEmpty;
    }

    public boolean isWall(int x, int y) {
        if (x < 0 || x >= COLS || y < 0 || y >= ROWS) return true;
        return grid[y][x] == '#';
    }

    public char getCoordinate(int r, int c) {
        if (r >= 0 && r < ROWS && c >= 0 && c < COLS) return grid[r][c];
        return '#';
    }
}