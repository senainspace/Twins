import java.util.Random;

public class Robot {
    public int x, y;
    public char type; // 'C' veya 'X'
    public int hp;

    private final Random rand = new Random();

    // X robotu için hareket yönü
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
    // player konumları da parametre olarak alınıyor böylece robotlar player karesine giremez
    public void step(Coard coard, Robot[] robots, int robotCount, int pax, int pay, int pbx, int pby) {
        if (!isAlive()) return;

        if (type == 'X') {
            moveXWith25PercentTurn(coard, robots, robotCount, pax, pay, pbx, pby);
        } else {
            // C robotunun akıllı hareketi henüz implemente edilmedi
            // oyunun çalışmaya devam etmesi için şimdilik rastgele hareket kullanılıyruz
            moveRandom(coard, robots, robotCount, pax, pay, pbx, pby);
        }
    }

    // Hedef kare başka bir robota veya player'a ait mi diye kontrol eder
    private boolean isOccupied(int nx, int ny, Robot[] robots, int robotCount, int pax, int pay, int pbx, int pby) {
        // Player A ve B kontrolü
        if (nx == pax && ny == pay) return true;
        if (nx == pbx && ny == pby) return true;
        // Diğer robotların konumu kontrolü
        for (int i = 0; i < robotCount; i++) {
            if (robots[i] == this) continue;
            if (robots[i].x == nx && robots[i].y == ny) return true;
        }
        return false;
    }

    // Her adımda tamamen rastgele bir yön seçerek hareket eder
    private void moveRandom(Coard coard, Robot[] robots, int robotCount, int pax, int pay, int pbx, int pby) {
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        // 4 yönü rastgele dene, ilk geçerli yöne git
        int start = rand.nextInt(4);
        for (int i = 0; i < 4; i++) {
            int[] d = dirs[(start + i) % 4];
            int nx = x + d[0], ny = y + d[1];
            if (!coard.isWall(nx, ny) && !isOccupied(nx, ny, robots, robotCount, pax, pay, pbx, pby)) {
                x = nx; y = ny;
                return;
            }
        }
    }

    // X robotu 4 yönde rastgele hareket eder,
    // her adımda %25 ihtimalle yön değiştirir.
    private void moveXWith25PercentTurn(Coard coard, Robot[] robots, int robotCount, int pax, int pay, int pbx, int pby) {
        // Henüz yön belirlenmemişse yeni bir yön seç
        if (!hasDir) pickNewDirection();

        // %25 ihtimalle yön değiştirir
        if (rand.nextInt(4) == 0) {
            pickNewDirection();
        }

        // Mevcut yönde ilerlemeyi dene duvara çarparsa veya kare doluysa yeni yön seç
        for (int attempt = 0; attempt < 4; attempt++) {
            int nx = x + dirX;
            int ny = y + dirY;

            if (!coard.isWall(nx, ny) && !isOccupied(nx, ny, robots, robotCount, pax, pay, pbx, pby)) {
                x = nx;
                y = ny;
                return;
            }

            // Duvara çarptıysa veya kare doluysa yön değiştir ve tekrar dene
            pickNewDirection();
        }
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