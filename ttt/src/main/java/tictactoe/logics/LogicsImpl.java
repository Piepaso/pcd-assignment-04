package tictactoe.logics;

import java.util.ArrayList;
import java.util.List;

public class LogicsImpl implements Logics {

    private final List<Integer> board = new ArrayList<>(9);
    private static final int[][] WINNING_COMBINATIONS = {
            {0, 1, 2}, {3, 4, 5}, {6, 7, 8}, // rows
            {0, 3, 6}, {1, 4, 7}, {2, 5, 8}, // columns
            {0, 4, 8}, {2, 4, 6}             // diagonals
    };
    private int playerTurn;

    public LogicsImpl() {
        for (int i = 0; i < 9; i++) {
            board.add(-1);
        }
        playerTurn = 0;
    }

    @Override
    public List<Integer> getBoard() {
        return List.copyOf(board);
    }

    @Override
    public boolean mark(int pos, int player) throws IllegalStateException {
        if (player != playerTurn) {
            throw new IllegalStateException("Not your turn!");
        }
        if (board.get(pos) != -1) {
            throw new IllegalStateException("Position already occupied!");
        }
        board.set(pos, player);
        boolean win = checkWin(player);
        playerTurn = (player + 1) % 2;
        return win;
    }

    private boolean checkWin(int player) {
        for (int[] tris : WINNING_COMBINATIONS) {
            int tot = 0;
            for (int pos : tris) {
                tot += board.get(pos) == player ? 1 : 0;
            }
            if (tot == 3) {
                return true;
            }
        }
        return false;
    }
}
