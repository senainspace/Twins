import java.util.Scanner;

public class TwinsTest {
    // Programın entry point'i. Twins objesini oluşturup oyunu başlatır.
    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        System.out.println("#### TWINS GAME ####");
        System.out.println("1. Load Maze from maze.txt");
        System.out.println("2. Generate Random Maze");
        System.out.print("Select an option: ");
        int mode = sc.nextInt();
        Twins game = new Twins(mode);
        game.run();
    }
}