package tictactoe.logics;

import java.util.List;

public interface Logics {
    List<Integer> getBoard();
    boolean mark(int pos, int player) throws IllegalStateException;
}
