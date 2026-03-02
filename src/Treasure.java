public class Treasure {
    // Maze'deki collect edilebilir objeleri (1, 2, 3, @) temsil eder. Player ve computer için puan değerleri farklıdır.
    public int x, y;
    public char symbol;          // '1', '2', '3'
    public int playerPoints;     // player için
    public int computerPoints;   // computer için (3x)

    public Treasure(int x, int y, char symbol) {
        this.x = x;
        this.y = y;
        this.symbol = symbol;

        if (symbol == '1') {
            playerPoints = 3;
            computerPoints = 9;
        } else if (symbol == '2') {
            playerPoints = 10;
            computerPoints = 30;
        } else if (symbol == '3') {
            playerPoints = 30;
            computerPoints = 90;
        } else {
            playerPoints = 0;
            computerPoints = 0;
        }
    }
}