import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

public class Maze {

    private final int ROWS = 23;
    private final int COLS = 53;
    private char[][] grid;
    private Random random;

    public Maze() {
        grid = new char[ROWS][COLS];
        random = new Random();
        initializeGrid();
        generateRandomMaze();
    }

    private void initializeGrid() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                grid[r][c] = ' ';
            }
        }
    }

    public Maze(boolean loadFromFile) {
        grid = new char[ROWS][COLS];
        if (loadFromFile) {
            loadMazeFromFile("maze.txt");
        } else {
            generateRandomMaze();
        }
    }
    private void loadMazeFromFile(String filename) {

    }
    private void generateRandomMaze() {

        addBorders();

        // 2. AddWalls(4, 8): 4 * LongWall (length:8)
        addWalls(4, 8);

        // 3. AddWalls(6, 6): 6 * MediumWall (length:6)
        addWalls(6, 6);

        // 4. AddWalls(20, 4): 20 * ShortWall (length:4)
        addWalls(20, 4);

        // 5. AddWalls(5, 3): 5 * ShortWall (length:3)
        addWalls(5, 3);

    }

    private void addBorders() {
        for (int c = 0; c < COLS; c++) {
            grid[0][c] = '#';
            grid[ROWS - 1][c] = '#';
        }
        for (int r = 0; r < ROWS; r++) {
            grid[r][0] = '#';
            grid[r][COLS - 1] = '#';
        }
    }

    private void addWalls(int numberOfWalls, int wallLength) {
        int wallsPlaced = 0;
        int attempts = 0;
        int maxAttempts = 10000;

        while (wallsPlaced < numberOfWalls && attempts < maxAttempts) {
            attempts++;

            int r = (int) (Math.random() * ROWS);
            int c = (int) (Math.random() * COLS);
            int dir = (int) (Math.random() * 2);

            if (dir == 0 && c + wallLength > COLS - 1) continue;
            if (dir == 1 && r + wallLength > ROWS - 1) continue;

            boolean overlaps = false;
            for (int i = 0; i < wallLength; i++) {
                int dr = (dir == 1) ? i : 0;
                int dc = (dir == 0) ? i : 0;
                if (grid[r + dr][c + dc] == '#') {
                    overlaps = true;
                    break;
                }
            }
            if (overlaps) continue;

            for (int i = 0; i < wallLength; i++) {
                int dr = (dir == 1) ? i : 0;
                int dc = (dir == 0) ? i : 0;
                grid[r + dr][c + dc] = '#';
            }

            // 3. VALIDATE
            boolean isValid = true;
            if (!checkConnected()) isValid = false;
            else if (!checkArea(2, 2, 3)) isValid = false;
            else if (!checkArea(3, 3, 5)) isValid = false;
            else if (!checkArea(4, 4, 7)) isValid = false;
            else if (!checkArea(6, 6, 15)) isValid = false;

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

    private boolean checkArea(int width, int height, int maxWalls)
    {
        for (int r = 0; r <= ROWS - height; r++ )
        {
            for (int c = 0; c <= COLS - width; c++)
            {
                int currentWallCount = 0;

                for (int i = 0; i < height; i++) {
                    for (int j = 0; j < width; j++) {
                        if (grid[r + i][c + j] == '#') {
                            currentWallCount++;
                        }
                    }
                }
                if (currentWallCount > maxWalls) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean checkConnected() {
        int totalEmpty = 0;
        int startRow = -1;
        int startCol = -1;
        boolean[][] visited = new boolean[ROWS][COLS];
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (grid[r][c] == ' ') {
                    totalEmpty++;
                    if (startRow == -1) {
                        startRow = r;
                        startCol = c;
                    }
                }
            }
        }
        if (totalEmpty == 0) return true;

        int reachableCount = 0;
        Queue<int[]> queue = new LinkedList<>();

        queue.add(new int[]{startRow, startCol});
        visited[startRow][startCol] = true;

        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            reachableCount++;


            for (int[] dir : directions) {
                int newRow = current[0] + dir[0];
                int newCol = current[1] + dir[1];


                if (newRow >= 0 && newRow < ROWS && newCol >= 0 && newCol < COLS &&
                        !visited[newRow][newCol] && grid[newRow][newCol] == ' ') {

                    visited[newRow][newCol] = true;
                    queue.add(new int[]{newRow, newCol});
                }
            }
        }
        return reachableCount == totalEmpty;
    }

    public char[][] getGrid() {

        return grid;


    }

    public char getCoordinate(int r, int c) {
        if (r >= 0 && r < ROWS && c >= 0 && c < COLS) {
            return grid[r][c];
        }
        return '#';
    }
    public void printMaze() {
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                System.out.print(grid[i][j]);
            }
            System.out.println(); 
        }
    }


