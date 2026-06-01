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

        gui.setOnHostSelected(() -> {
            HostMatch hostMatch = new HostMatch(logics);
            configureMatch(hostMatch, gui);
            hostMatch.init();
            gui.showHostCode(hostMatch.getJoinCode());

        });

        gui.setOnJoinSelected(code -> {
            ClientMatch clientMatch = new ClientMatch(logics, code);
            configureMatch(clientMatch, gui);
        });
    }

    private static void configureMatch(Match match, GUI gui) {
        match.addGameStartedObserver(gui::startGame);
        match.addUpdateBoardObserver(gui::updateBoard);
        match.addStatusObserver(gui::updateStatus);
        gui.setOnMoveSelected(match::move);
        match.start();
    }
}
