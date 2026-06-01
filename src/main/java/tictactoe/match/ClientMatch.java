package tictactoe.match;

import tictactoe.logics.Logics;
import tictactoe.service.BoardListener;
import tictactoe.service.BoardService;
import tictactoe.service.SerializableMove;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class ClientMatch extends AbstractMatch {

	private BoardService service;
	private static final int PLAYER_NUM = 1;
	private final String code;

	public ClientMatch(Logics logics, String code) {
		super(logics);
		this.code = code;
	}

	@Override
	public void start() {
		super.start();
		try {
			Registry registry = LocateRegistry.getRegistry();
			service = (BoardService) registry.lookup(code);
			var listenerStub = (BoardListener) UnicastRemoteObject.exportObject(listener, 0);
			service.addBoardListener(listenerStub);
			notifyGameStarted.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void move(Integer pos) {
		try {
			service.makeMove(new SerializableMove(pos, PLAYER_NUM));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
