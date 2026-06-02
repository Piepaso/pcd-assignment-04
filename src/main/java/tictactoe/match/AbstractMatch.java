package tictactoe.match;

import tictactoe.service.SerializableBoard;
import tictactoe.service.BoardListener;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class AbstractMatch implements Match {
	protected final Runnable notifyGameStarted;
	private final List<Runnable> gameStartedObservers = new ArrayList<>();
	private final List<Consumer<List<Integer>>> boardObservers = new ArrayList<>();
	protected final List<Consumer<String>> statusObservers = new ArrayList<>();
	private boolean gameStarted;

	protected final BoardListener listener;

	public AbstractMatch() {
		this.notifyGameStarted = () -> {
			gameStarted = true;
			for (Runnable observer : gameStartedObservers) {
				observer.run();
			}
		};

		this.listener = new BoardListener() {
			@Override
			public void boardUpdated(SerializableBoard board) throws RemoteException {
				for (Consumer<List<Integer>> observer : boardObservers) {
					observer.accept(board.cells());
				}
			}

			@Override
			public void notifyWinner(int winner) throws RemoteException {
				for (Consumer<String> observer : statusObservers) {
					observer.accept("Player " + winner + " wins!");
				}
			}

			@Override
			public void notifyMessage(String message) throws RemoteException {
				for (Consumer<String> observer : statusObservers) {
					observer.accept(message);
				}
			}
		};
	}

	@Override
	public void addGameStartedObserver(Runnable f) {
		if (f != null) {
			this.gameStartedObservers.add(f);
			if (gameStarted) {
				f.run();
			}
		}
	}

	@Override
	public void addUpdateBoardObserver(Consumer<List<Integer>> board) {
		if (board != null) {
			this.boardObservers.add(board);
		}
	}

	@Override
	public void addStatusObserver(Consumer<String> status) {
		if (status != null) {
			this.statusObservers.add(status);
		}
	}
}
