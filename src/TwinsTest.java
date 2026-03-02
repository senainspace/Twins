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

        // 1 veya 2 dışında bir şey girilirse tekrar sor
        while (mode != 1 && mode != 2) {
            System.out.println("Invalid input! Please enter 1 or 2.");
            System.out.print("Select an option: ");
            mode = sc.nextInt();
        }

        Twins game = new Twins(mode);
        game.run();
    }
}