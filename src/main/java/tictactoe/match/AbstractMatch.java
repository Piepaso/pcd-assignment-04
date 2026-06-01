package tictactoe.match;

import tictactoe.logics.Logics;
import tictactoe.service.SerializableBoard;
import tictactoe.service.BoardListener;

import java.rmi.RemoteException;
import java.util.List;
import java.util.function.Consumer;

public abstract class AbstractMatch implements Match {
	protected Runnable notifyGameStarted;
	protected final Logics logics;
	private Consumer<List<Integer>> onBoardUpdate;
	private Consumer<String> onMessage;

	protected BoardListener listener;


	public AbstractMatch(Logics logics) {
		this.logics = logics;
	}

	@Override
	public void start() {
		this.listener = new BoardListener() {
			@Override
			public void boardUpdated(SerializableBoard board) throws RemoteException {
				onBoardUpdate.accept(board.cells());
			}

			@Override
			public void notifyWinner(int winner) throws RemoteException {
				onMessage.accept("Player " + winner + " wins!");
			}

			@Override
			public void notifyMessage(String message) throws RemoteException {
				onMessage.accept(message);
			}
		};
	}

	@Override
	public void addGameStartedObserver(Runnable f) {
		this.notifyGameStarted = f;
	}

	@Override
	public void addUpdateBoardObserver(Consumer<List<Integer>> board) {
		this.onBoardUpdate = board;
	}

	@Override
	public void addStatusObserver(Consumer<String> status) {
		this.onMessage = status;
	}
}
