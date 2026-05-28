package tictactoe;
import java.rmi.Remote;

public interface Match extends Remote {
    void setHost(boolean host);
    void addPlayerListener(PlayerListener playerListener);
}
