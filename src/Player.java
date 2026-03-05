// Programın entry point'i. Twins objesini oluşturup oyunu başlatır.
public class Player {
    public int ax, ay;  // A'nın pozisyonu
    public int bx, by;  // B'nin pozisyonu
    public int hp;
    public int score;
    public int laserCount;
    public int mode;    // 1: aynı yön, -1: ters yön

    public Player(int ax, int ay, int bx, int by) {
        this.ax = ax; this.ay = ay;
        this.bx = bx; this.by = by;
        hp = 1000;
        score = 0;
        laserCount = 0;
        mode = 1;
    }

    // robot dizisi de alınıyor çünkü player robotun üstüne geçemez
    public void move(int dx, int dy, Coard coard, Robot[] robots, int robotCount) {
        // A hareketi: duvar ve robot çakışması kontrolü
        int nax = ax + dx, nay = ay + dy;
        if (!coard.isWall(nax, nay) && !isRobotAt(nax, nay, robots, robotCount)) { ax = nax; ay = nay; }

        // B hareketi (mode 1: aynı yön, mode -1: ters yön): duvar ve robot çakışması kontrolü
        int nbx = bx + dx * mode, nby = by + dy * mode;
        if (!coard.isWall(nbx, nby) && !isRobotAt(nbx, nby, robots, robotCount)) { bx = nbx; by = nby; }
    }

    // Verilen koordinatta robot olup olmadığını kontrol eder
    private boolean isRobotAt(int x, int y, Robot[] robots, int robotCount) {
        for (int i = 0; i < robotCount; i++) {
            if (robots[i].x == x && robots[i].y == y) return true;
        }
        return false;
    }

    public boolean isAlive() { return hp > 0; }
}