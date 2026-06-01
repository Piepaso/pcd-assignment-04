package tictactoe.service;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface BoardListener extends Remote {
	void boardUpdated(SerializableBoard board) throws RemoteException;
	void notifyWinner(int winner) throws RemoteException;
	void notifyMessage(String message) throws RemoteException;
}
