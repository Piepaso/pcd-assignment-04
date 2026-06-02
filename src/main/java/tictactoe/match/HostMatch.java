package tictactoe.match;

import tictactoe.logics.Logics;
import tictactoe.service.BoardService;
import tictactoe.service.BoardServiceImpl;
import tictactoe.service.SerializableMove;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Optional;

public class HostMatch extends AbstractMatch {

	private static final int PLAYER_NUM = 0;
	private final BoardServiceImpl boardService;

	public HostMatch(Logics logics, String name) {
		this.boardService = new BoardServiceImpl(logics, notifyGameStarted);
		startRmi().ifPresent(registry -> {
			try {
				BoardService serviceStub = (BoardService) UnicastRemoteObject.exportObject(boardService, 0);
				registry.rebind(name, serviceStub);
				boardService.addBoardListener(listener);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	public String getHostIp() {
		try {
			return java.net.InetAddress.getLocalHost().getHostAddress();
		} catch (Exception e) {
			return "localhost";
		}
	}

	@Override
	public void move(Integer pos) {
		try {
			boardService.makeMove(new SerializableMove(pos, PLAYER_NUM));
		} catch (Exception e) {
			System.err.println("Failed to make move: ");
			e.printStackTrace();
		}
	}

	private static Optional<Registry> startRmi() {
		try {
			int port = 1099;
			System.out.println("Starting RMI registry on port " + port + "...");
			Registry registry = LocateRegistry.createRegistry(port);
			System.out.println("RMI registry started successfully.");
			return Optional.of(registry);
		} catch (Exception e) {
			System.err.println("Failed to start RMI registry: ");
			e.printStackTrace();
			return Optional.empty();
		}
	}
}
