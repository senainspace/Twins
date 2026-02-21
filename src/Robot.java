import java.util.Random;
// C ve X robot'larını temsil eder. C en yakın treasure'a gider, X random hareket eder.
public class Robot {
    public int x, y;
    public char type; // 'C' veya 'X'
    public int hp;
    private Random rand = new Random();

    public Robot(int x, int y, char type) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.hp = 1000;
    }

    public void moveRandom(Coard coard) {
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        int[] d = dirs[rand.nextInt(4)];
        int nx = x + d[0], ny = y + d[1];
        if (!coard.isWall(nx, ny)) { x = nx; y = ny; }
    }

    public boolean isAlive() { return hp > 0; }
}