package tictactoe;

import com.sun.source.util.TaskListener;

import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;
import java.util.Set;

public class HostLogics implements Logics {
    private final TableService table;

    public HostLogics(Registry registry) {
        table = new TableService() {

            @Override
            public void mark(Pair<Integer, Integer> pos) throws RemoteException {

            }

            @Override
            public void addTableObserver() throws RemoteException {

            }
        };
        try {
            TableService myRemoteObjProxy = (TableService) UnicastRemoteObject.exportObject(table, 0);
            registry.rebind("table", myRemoteObjProxy);
        } catch (RemoteException e) {}

    }

    @Override
    public boolean hasX(int x, int y) {
        return marked.contains(new Pair<>(x, y));
    }

    @Override
    public boolean hasO(int x, int y) {
        return false;
    }

    @Override
    public boolean mark(int x, int y) {
        marked.add(new Pair<>(x, y));
        return false;
    }
}
