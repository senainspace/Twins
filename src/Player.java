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

    public void move(int dx, int dy, Coard coard) {
        // A hareketi
        int nax = ax + dx, nay = ay + dy;
        if (!coard.isWall(nax, nay)) { ax = nax; ay = nay; }

        // B hareketi (mode 1: aynı yön, mode -1: ters yön)
        int nbx = bx + dx * mode, nby = by + dy * mode;
        if (!coard.isWall(nbx, nby)) { bx = nbx; by = nby; }
    }

    public boolean isAlive() { return hp > 0; }
}