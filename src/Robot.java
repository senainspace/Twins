import java.util.Random;

public class Robot {
    public int x, y;
    public char type; // 'C' or 'X'
    public int hp;

    private final Random rand = new Random();

    // Current direction (inertia) for X robot
    private int dirX = 0;
    private int dirY = 0;
    private boolean hasDir = false;

    // C and X Robots start with 1000 life points.
    public Robot(int x, int y, char type) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.hp = 1000;

        // Select initial direction for X robot
        if (type == 'X') {
            pickNewDirection();
        }
    }

    // Single entry point from the Twins game loop: move according to robot type
    public void step(Coard coard) {
        if (!isAlive()) return;

        if (type == 'X') {
            moveXWith25PercentTurn(coard);
        } else {
            // If C robot's "smart" movement is not implemented yet,
            // keep random movement so the game can continue running.
            moveRandom(coard);
        }
    }

    // Simple random movement (completely new direction at each step)
    private void moveRandom(Coard coard) {
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        int[] d = dirs[rand.nextInt(4)];
        int nx = x + d[0], ny = y + d[1];
        if (!coard.isWall(nx, ny)) { x = nx; y = ny; }
    }

    // X Robots move randomly in 4 directions
    // with a 25% probability of direction change at each step.
    private void moveXWith25PercentTurn(Coard coard) {
        // Safety check: if no direction is set yet, choose one
        if (!hasDir) pickNewDirection();

        // 25% probability to change direction
        if (rand.nextInt(4) == 0) { // 0,1,2,3 -> 1/4 = 25%
            pickNewDirection();
        }

        // Try moving in the current direction;
        // if blocked by a wall, try a new direction
        for (int attempt = 0; attempt < 4; attempt++) {
            int nx = x + dirX;
            int ny = y + dirY;

            if (!coard.isWall(nx, ny)) {
                x = nx;
                y = ny;
                return;
            }

            // If it hits a wall, change direction and try again
            pickNewDirection();
        }
        // If all 4 attempts fail (very rare), it stays in place.
    }

    private void pickNewDirection() {
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        int[] d = dirs[rand.nextInt(4)];
        dirX = d[0];
        dirY = d[1];
        hasDir = true;
    }

    public boolean isAlive() { return hp > 0; }
}
