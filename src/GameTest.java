public class GameTest {
     public static void main(String[] args) throws Exception {
        Maze myMaze = new Maze();

        Game game = new Game(myMaze);

        game.run();
    }
}
