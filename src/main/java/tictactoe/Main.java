package tictactoe;

import tictactoe.logics.Logics;
import tictactoe.logics.LogicsImpl;
import tictactoe.match.ClientMatch;
import tictactoe.match.HostMatch;
import tictactoe.match.Match;
import tictactoe.view.GUI;

public class Main {
    public static void main(String[] args) {
        Logics logics = new LogicsImpl();
        GUI gui = new GUI();
        ActiveController controller = new ActiveController();
        controller.start();

        gui.setOnHostSelected(name -> controller.submit(() -> {
            HostMatch hostMatch = new HostMatch(logics, name);
            configureMatch(hostMatch, gui, controller);
            gui.showHostIP(hostMatch.getHostIp());
        }));

        gui.setOnJoinSelected((ip, name) -> controller.submit(() -> {
            ClientMatch clientMatch = new ClientMatch();
            configureMatch(clientMatch, gui, controller);
            clientMatch.connect(ip, name);
        }));
    }

    private static void configureMatch(Match match, GUI gui, ActiveController controller) {
        match.addGameStartedObserver(gui::startGame);
        match.addUpdateBoardObserver(gui::updateBoard);
        match.addStatusObserver(gui::updateStatus);
        gui.setOnMoveSelected(pos -> controller.submit(() -> match.move(pos)));
    }
}
