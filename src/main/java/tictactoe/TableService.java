package tictactoe;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface TableService extends Remote {
    void mark(Pair<Integer, Integer> pos) throws RemoteException;
    void addTableObserver() throws RemoteException;
}
