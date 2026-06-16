package tictactoe.service;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface BoardService extends Remote {
    void addBoardListener(BoardListener listener) throws RemoteException;
    void makeMove(SerializableMove move) throws RemoteException;
}
