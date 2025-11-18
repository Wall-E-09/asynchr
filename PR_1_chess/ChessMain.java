import java.util.Scanner;
import java.util.concurrent.Semaphore;
import java.util.Random;

class Board {
    public final Semaphore whiteTurn = new Semaphore(0);
    public final Semaphore blackTurn = new Semaphore(0);
    public final Semaphore moveDone = new Semaphore(0);
}

class Player implements Runnable {
    private String color;
    private Board board;
    private Semaphore mySemaphore;
    private Random random;

    public Player(String color, Board board, Semaphore mySemaphore) {
        this.color = color;
        this.board = board;
        this.mySemaphore = mySemaphore;
        this.random = new Random();
    }

    @Override
    public void run() {
        try {
            while (true) {
                mySemaphore.acquire();

                System.out.println("   - " + color + " is thinking");
                Thread.sleep(500 + random.nextInt(500));
                System.out.println("   - " + color + " made a move");

                board.moveDone.release();
            }
        } catch (InterruptedException e) {
        }
    }
}

public class ChessMain {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Board board = new Board();

        Thread p1 = new Thread(new Player("White", board, board.whiteTurn));
        Thread p2 = new Thread(new Player("Black", board, board.blackTurn));

        p1.setDaemon(true);
        p2.setDaemon(true);

        p1.start();
        p2.start();

        boolean isWhiteTurn = true;
        int movesCount = 0;
        int maxMoves = 10;

        System.out.println("CHESS GAME");
        System.out.println("Controls:");
        System.out.println(" '1' = Move White");
        System.out.println(" '2' = Move Black");
        System.out.println(" '0' = SURRENDER (Give up)");

        try {
            while (movesCount < maxMoves) {
                String currentTurnName = isWhiteTurn ? "White (1)" : "Black (2)";
                System.out.println("\nWait for move: " + currentTurnName);
                System.out.print("Action : ");

                String input = scanner.nextLine();

                if (input.equals("0")) {
                    String loser = isWhiteTurn ? "White" : "Black";
                    String winner = isWhiteTurn ? "Black" : "White";

                    System.out.println("\n" + loser + " resigned");
                    System.out.println(winner + " wins the game");

                    break;
                }

                if (input.equals("1")) {
                    if (isWhiteTurn) {
                        board.whiteTurn.release();
                        board.moveDone.acquire();
                        isWhiteTurn = false;
                        movesCount++;
                    } else {
                        System.out.println("ERROR: black's turn");
                    }
                } else if (input.equals("2")) {
                    if (!isWhiteTurn) {
                        board.blackTurn.release();
                        board.moveDone.acquire();
                        isWhiteTurn = true;
                        movesCount++;
                    } else {
                        System.out.println("ERROR: white's turn");
                    }
                } else {
                    System.out.println("Invalid input. Use 1, 2 or 0.");
                }
            }

            if (movesCount >= maxMoves) {
                System.out.println("\n Draw. Game Over ");
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        scanner.close();
    }
}
