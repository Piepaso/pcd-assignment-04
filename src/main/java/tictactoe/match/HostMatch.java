package tictactoe.match;

import tictactoe.logics.Logics;
import tictactoe.service.BoardService;
import tictactoe.service.BoardServiceImpl;
import tictactoe.service.SerializableMove;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import java.util.Optional;
import java.util.Enumeration;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;

public class HostMatch extends AbstractMatch {

	private static final int PLAYER_NUM = 0;
	private final BoardServiceImpl boardService;

	public HostMatch(Logics logics, String name) {
		String hostIp = resolveHostIp();
		System.setProperty("java.rmi.server.hostname", hostIp);
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
		return resolveHostIp();
	}

	private static String resolveHostIp() {
		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface netIf = interfaces.nextElement();
				if (!netIf.isUp() || netIf.isLoopback() || netIf.isVirtual()) {
					continue;
				}
				Enumeration<InetAddress> addresses = netIf.getInetAddresses();
				while (addresses.hasMoreElements()) {
					InetAddress address = addresses.nextElement();
					if (address instanceof Inet4Address && address.isSiteLocalAddress()) {
						return address.getHostAddress();
					}
				}
			}
		} catch (Exception e) {
			// Fall back to localhost below.
		}
		return "127.0.0.1";
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
