import java.util.Random;

public class Robot {
    public int x, y;
    public char type; // 'C' veya 'X'
    public int hp;

    private final Random rand = new Random();

    // X robotu için mevcut hareket yönü
    private int dirX = 0;
    private int dirY = 0;
    private boolean hasDir = false;

    // C ve X robotlar 1000 can ile başlar.
    public Robot(int x, int y, char type) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.hp = 1000;

        // X robotu için başlangıç yönünü belirle
        if (type == 'X') {
            pickNewDirection();
        }
    }

    // Twins oyun döngüsünden çağrılan tek hareket metodu: robot tipine göre hareket eder
    public void step(Coard coard) {
        if (!isAlive()) return;

        if (type == 'X') {
            moveXWith25PercentTurn(coard);
        } else {
            // C robotunun akıllı hareketi henüz implemente edilmedi
            // oyunun çalışmaya devam etmesi için şimdilik rastgele hareket kullanılıyruz
            moveRandom(coard);
        }
    }

    // Her adımda tamamen rastgele bir yön seçerek hareket eder
    private void moveRandom(Coard coard) {
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        int[] d = dirs[rand.nextInt(4)];
        int nx = x + d[0], ny = y + d[1];
        if (!coard.isWall(nx, ny)) { x = nx; y = ny; }
    }

    // X robotu 4 yönde rastgele hareket eder,
    // her adımda %25 ihtimalle yön değiştirir.
    private void moveXWith25PercentTurn(Coard coard) {
        // Henüz yön belirlenmemişse yeni bir yön seç
        if (!hasDir) pickNewDirection();

        // %25 ihtimalle yön değiştir
        if (rand.nextInt(4) == 0) {
            pickNewDirection();
        }

        // Mevcut yönde ilerlemeyi dene; duvara çarparsa yeni yön seç
        for (int attempt = 0; attempt < 4; attempt++) {
            int nx = x + dirX;
            int ny = y + dirY;

            if (!coard.isWall(nx, ny)) {
                x = nx;
                y = ny;
                return;
            }

            // Duvara çarptıysa yön değiştir ve tekrar dene
            pickNewDirection();
        }
        // 4 denemenin hepsi başarısız olursa olduğu yerde kalır.
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