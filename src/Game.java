import enigma.core.Enigma;
import enigma.event.TextMouseEvent;
import enigma.event.TextMouseListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class Game {
    public enigma.console.Console cn = Enigma.getConsole("Mouse and Keyboard");
    public TextMouseListener tmlis;
    public KeyListener klis;

    public int mousepr;
    public int mousex, mousey;
    public int keypr;
    public int rkey;



    Game maze; // Add reference to the maze

    // Constructor only sets up the game data
    Game(Game m) throws Exception {
        this.maze = m; // Store the maze

        tmlis=new TextMouseListener() {
            public void mouseClicked(TextMouseEvent arg0) {}
            public void mousePressed(TextMouseEvent arg0) {
                if(mousepr==0) {
                    mousepr=1;
                    mousex=arg0.getX();
                    mousey=arg0.getY();
                }
            }
            public void mouseReleased(TextMouseEvent arg0) {}
        };
        cn.getTextWindow().addTextMouseListener(tmlis);

        klis=new KeyListener() {
            public void keyTyped(KeyEvent e) {}
            public void keyPressed(KeyEvent e) {
                if(keypr==0) {
                    keypr=1;
                    rkey=e.getKeyCode();
                }
            }
            public void keyReleased(KeyEvent e) {}
        };
        cn.getTextWindow().addKeyListener(klis);
    }

    // New method to run the game loop
    public void run() throws Exception {
        int px=5,py=5;


        cn.getTextWindow().output(px,py,'A');

        while(true) {
            if(mousepr==1) {
                cn.getTextWindow().output(mousex,mousey,'#');
                px=mousex; py=mousey;
                mousepr=0;
            }
            if(keypr==1) {
                // Clear old player position
                cn.getTextWindow().output(px, py, ' ');

                int targetX = px;
                int targetY = py;

                if (rkey == KeyEvent.VK_LEFT) targetX--;
                if (rkey == KeyEvent.VK_RIGHT) targetX++;
                if (rkey == KeyEvent.VK_UP) targetY--;
                if (rkey == KeyEvent.VK_DOWN) targetY++;

                cn.getTextWindow().output(px, py, 'A');
                keypr=0;
            }
            Thread.sleep(20);
        }
    }
}

