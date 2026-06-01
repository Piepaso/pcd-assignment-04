package tictactoe.view;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GUI {
    private JFrame frame;
    private JPanel mainPanel;
    private CardLayout cardLayout;

    // Schermata 1: Menu
    private JButton hostButton;
    private JButton joinButton;

    // Schermata 2: Lobby/Codice
    private JLabel codeLabel;
    private JTextField codeField;
    private JButton connectButton;

    // Schermata 3: Gioco
    private final List<JButton> boardButtons = new ArrayList<>(9);
    private JLabel messageLabel;

    // Callback per comunicare con il Main/Match
    private Runnable onHostSelected;
    private Consumer<String> onJoinSelected;
    private Consumer<Integer> onMoveSelected;

    public GUI() {
        SwingUtilities.invokeLater(this::initGUI);
    }

    private void initGUI() {
        frame = new JFrame("TicTacToe Distribuito");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 400);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // Costruiamo i tre pannelli (le tre "fasi")
        mainPanel.add(createMenuPanel(), "MENU");
        mainPanel.add(createLobbyPanel(), "LOBBY");
        mainPanel.add(createGamePanel(), "GAME");

        frame.add(mainPanel);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // --- CREAZIONE DEI PANNELLI ---

    private JPanel createMenuPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        hostButton = new JButton("Ospita Partita (Host)");
        joinButton = new JButton("Unisciti a Partita (Join)");

        hostButton.addActionListener(e -> {
            if (onHostSelected != null) onHostSelected.run();
        });

        joinButton.addActionListener(e -> {
            // Mostra i campi per inserire il codice nel pannello Lobby
            codeLabel.setText("Inserisci il codice dell'Host:");
            codeField.setVisible(true);
            connectButton.setVisible(true);
            cardLayout.show(mainPanel, "LOBBY");
        });

        panel.add(hostButton);
        panel.add(joinButton);
        return panel;
    }

    private JPanel createLobbyPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = GridBagConstraints.RELATIVE; gbc.insets = new Insets(10,10,10,10);

        codeLabel = new JLabel("In attesa del codice...");
        codeField = new JTextField(15);
        connectButton = new JButton("Connettiti");

        // Di default nascosti, usati solo dal Guest
        codeField.setVisible(false);
        connectButton.setVisible(false);

        connectButton.addActionListener(e -> {
            if (onJoinSelected != null) onJoinSelected.accept(codeField.getText());
        });

        panel.add(codeLabel, gbc);
        panel.add(codeField, gbc);
        panel.add(connectButton, gbc);
        return panel;
    }

    private JPanel createGamePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel gridPanel = new JPanel(new GridLayout(3, 3, 5, 5));

        for (int i = 0; i < 9; i++) {
            final int index = i;
            JButton btn = new JButton(" ");
            btn.setFont(new Font("Arial", Font.BOLD, 40));
            btn.addActionListener(e -> {
                if (onMoveSelected != null) onMoveSelected.accept(index);
            });
            boardButtons.add(btn);
            gridPanel.add(btn);
        }

        messageLabel = new JLabel("In attesa dell'avversario...", SwingConstants.CENTER);
        messageLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        messageLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panel.add(gridPanel, BorderLayout.CENTER);
        panel.add(messageLabel, BorderLayout.SOUTH);
        return panel;
    }

    // --- METODI DI CONTROLLO (Chiamati dal Main/Match) ---

    public void showHostCode(String code) {
        SwingUtilities.invokeLater(() -> {
            codeLabel.setText("<html><center>Il tuo codice di gioco è:<br><b style='font-size:16px;'>" + code + "</b><br><br>In attesa che l'altro giocatore si unisca...</center></html>");
            codeField.setVisible(false);
            connectButton.setVisible(false);
            cardLayout.show(mainPanel, "LOBBY");
        });
    }

    public void startGame() {
        // Ecco il "risveglio"! RMI chiama questo metodo e noi passiamo alla schermata di gioco
        SwingUtilities.invokeLater(() -> {
            cardLayout.show(mainPanel, "GAME");
            updateStatus("Partita Iniziata! Il gioco ha inizio.");
        });
    }

    public void updateBoard(List<Integer> board) {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < 9; i++) {
                if (board.get(i) == 0) boardButtons.get(i).setText("X");
                else if (board.get(i) == 1) boardButtons.get(i).setText("O");
                else boardButtons.get(i).setText(" ");
            }
        });
    }

    public void updateStatus(String message) {
        SwingUtilities.invokeLater(() -> messageLabel.setText(message));
    }

    // --- SETTER PER I LISTENER ---

    public void setOnHostSelected(Runnable callback) { this.onHostSelected = callback; }
    public void setOnJoinSelected(Consumer<String> callback) { this.onJoinSelected = callback; }
    public void setOnMoveSelected(Consumer<Integer> callback) { this.onMoveSelected = callback; }
}