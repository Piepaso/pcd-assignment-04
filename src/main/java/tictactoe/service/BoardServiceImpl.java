package tictactoe.service;

import tictactoe.logics.*;

import java.rmi.RemoteException;
import java.util.List;

public class BoardServiceImpl implements BoardService {
	private final Logics logics;
	private final List<BoardListener> listeners = new java.util.ArrayList<>();
	private final Runnable notifyGameStarted;

	public BoardServiceImpl(Logics logics, Runnable r) {
		this.logics = logics;
		this.notifyGameStarted = r;
	}

	@Override
	public void addBoardListener(BoardListener listener) throws RemoteException {
		listeners.add(listener);
		if (listeners.size() == 2) {
			notifyGameStarted.run();
			for (BoardListener l : listeners) {
				l.boardUpdated(new SerializableBoard(logics.getBoard()));
			}
		}
	}

	@Override
	public void makeMove(SerializableMove move) throws RemoteException {
		try {
			if (logics.mark(move.position(), move.player())) {
				 for (BoardListener listener : listeners) {
					 listener.notifyWinner(move.player());
				 }
			}
			for (BoardListener listener : listeners) {
				listener.boardUpdated(new SerializableBoard(logics.getBoard()));
			}

		} catch (IllegalStateException e) {
			listeners.get(move.player()).notifyMessage(e.getMessage());
		}
	}
}
