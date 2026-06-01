package tictactoe.match;

import tictactoe.logics.Logics;
import tictactoe.service.BoardService;
import tictactoe.service.BoardServiceImpl;
import tictactoe.service.SerializableMove;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Optional;
import java.util.Random;

public class HostMatch extends AbstractMatch {

	private static final int PLAYER_NUM = 0;
	private String code = "undefined";
	private BoardServiceImpl boardService;

	public HostMatch(Logics logics) {
		super(logics);
	}

	public String getJoinCode() {
		return code;
	}

	public void init() {
		this.boardService = new BoardServiceImpl(logics, notifyGameStarted);
		startRmi().ifPresent(registry -> {
			try {
				BoardService serviceStub = (BoardService) UnicastRemoteObject.exportObject(boardService, 0);

				Random random = new Random();
				String code = random.nextInt(1000, 10000) + "";

				registry.rebind(code, serviceStub);

				boardService.addBoardListener(listener);
				this.code = code;
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	@Override
	public void move(Integer pos) {
		try {
			boardService.makeMove(new SerializableMove(pos, PLAYER_NUM));
		} catch (Exception e) {
			System.err.println("Errore durante l'esecuzione della mossa:");
			e.printStackTrace();
		}
	}

	private static Optional<Registry> startRmi() {
		try {
			int porta = 1099; // default
			System.out.println("Avvio del registro RMI sulla porta " + porta + "...");
			Registry registry = LocateRegistry.createRegistry(porta);
			System.out.println("Registro RMI avviato con successo!");

			System.out.println("Server pronto.");
			return Optional.of(registry);
		} catch (Exception e) {
			System.err.println("Errore durante l'avvio del Server RMI:");
			e.printStackTrace();
			return Optional.empty();
		}
	}
}
