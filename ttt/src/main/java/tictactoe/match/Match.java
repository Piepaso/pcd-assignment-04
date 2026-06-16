package tictactoe.match;

import java.util.List;
import java.util.function.Consumer;

public interface Match {
    void addGameStartedObserver(Runnable f);

    void addUpdateBoardObserver(Consumer<List<Integer>> board);

    void addStatusObserver(Consumer<String> status);

    void move(Integer pos);
}
