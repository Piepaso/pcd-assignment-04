package tictactoe.match;

import tictactoe.service.BoardListener;
import tictactoe.service.BoardService;
import tictactoe.service.SerializableMove;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class ClientMatch extends AbstractMatch {

	private BoardService service;
	private static final int PLAYER_NUM = 1;

	public void connect(String ip, String lobbyName) {
		try {
			Registry registry = LocateRegistry.getRegistry(ip);
			service = (BoardService) registry.lookup(lobbyName);
			var listenerStub = (BoardListener) UnicastRemoteObject.exportObject(listener, 0);
			service.addBoardListener(listenerStub);
			notifyGameStarted.run();
		} catch (Exception e) {
			System.out.println("Failed to connect to server: " + e.getMessage());
		}
	}

	@Override
	public void move(Integer pos) {
		try {
			service.makeMove(new SerializableMove(pos, PLAYER_NUM));
		} catch (RemoteException e) {
			statusObservers.forEach(ob -> ob.accept("Network problems... retry"));
		}
	}
}
