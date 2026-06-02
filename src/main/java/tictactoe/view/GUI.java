package tictactoe.view;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer; // Importato BiConsumer
import java.util.function.Consumer;

public class GUI {
    private JFrame frame;
    private JPanel mainPanel;
    private CardLayout cardLayout;

    // Menu
    private JButton hostButton;
    private JButton joinButton;

    // Lobby
    private JLabel label1;       // Primo input (Nome Lobby per Host / IP per Client)
    private JTextField field1;
    private JLabel label2;       // Secondo input (Solo per il Client: Nome Lobby)
    private JTextField field2;
    private JButton actionButton;

    // Game
    private final List<JButton> boardButtons = new ArrayList<>(9);
    private JLabel messageLabel;

    // Callbacks
    private Consumer<String> onHostSelected;
    private BiConsumer<String, String> onJoinSelected; // Modificato in BiConsumer<IP, LobbyName>
    private Consumer<Integer> onMoveSelected;

    public GUI() {
        SwingUtilities.invokeLater(this::initGUI);
    }

    private void initGUI() {
        frame = new JFrame("Distributed TicTacToe");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 450); // Aumentato leggermente l'altezza per far spazio ai due campi

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        mainPanel.add(createMenuPanel(), "MENU");
        mainPanel.add(createLobbyPanel(), "LOBBY");
        mainPanel.add(createGamePanel(), "GAME");

        frame.add(mainPanel);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JPanel createMenuPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        hostButton = new JButton("Ospita Partita (Host)");
        joinButton = new JButton("Unisciti a Partita (Join)");

        // Configura la lobby per l'HOST
        hostButton.addActionListener(e -> {
            label1.setText("Inserisci il nome della Lobby:");
            field1.setText("");
            field1.setVisible(true);

            // L'host non ha bisogno del secondo campo
            label2.setVisible(false);
            field2.setVisible(false);

            actionButton.setText("Crea Lobby");
            actionButton.setVisible(true);

            for (var al : actionButton.getActionListeners()) actionButton.removeActionListener(al);
            actionButton.addActionListener(ev -> {
                if (onHostSelected != null) onHostSelected.accept(field1.getText());
            });

            cardLayout.show(mainPanel, "LOBBY");
        });

        // Configura la lobby per il CLIENT
        joinButton.addActionListener(e -> {
            label1.setText("Inserisci l'IP dell'Host:");
            field1.setText("");
            field1.setVisible(true);

            label2.setText("Inserisci il nome della Lobby:");
            field2.setText("");
            label2.setVisible(true);
            field2.setVisible(true);

            actionButton.setText("Connettiti");
            actionButton.setVisible(true);

            for (var al : actionButton.getActionListeners()) actionButton.removeActionListener(al);
            actionButton.addActionListener(ev -> {
                if (onJoinSelected != null) {
                    // field1 IP, field2 lobby name
                    onJoinSelected.accept(field1.getText(), field2.getText());
                }
            });

            cardLayout.show(mainPanel, "LOBBY");
        });

        panel.add(hostButton);
        panel.add(joinButton);
        return panel;
    }

    private JPanel createLobbyPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = GridBagConstraints.RELATIVE; gbc.insets = new Insets(8,10,8,10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        label1 = new JLabel("In attesa...", SwingConstants.CENTER);
        field1 = new JTextField(15);
        label2 = new JLabel("In attesa...", SwingConstants.CENTER);
        field2 = new JTextField(15);
        actionButton = new JButton("Invia");

        field1.setVisible(false);
        label2.setVisible(false);
        field2.setVisible(false);
        actionButton.setVisible(false);

        panel.add(label1, gbc);
        panel.add(field1, gbc);
        panel.add(label2, gbc);
        panel.add(field2, gbc);
        panel.add(actionButton, gbc);
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

    public void showHostIP(String ip) {
        SwingUtilities.invokeLater(() -> {
            label1.setText("<html><center>Your IP:<br><b style='font-size:16px;'>" + ip + "</b><br><br>Waiting for opponent to join...</center></html>");
            field1.setVisible(false);
            label2.setVisible(false);
            field2.setVisible(false);
            actionButton.setVisible(false);
            cardLayout.show(mainPanel, "LOBBY");
        });
    }

    public void startGame() {
        SwingUtilities.invokeLater(() -> {
            cardLayout.show(mainPanel, "GAME");
            updateStatus("Opponent joined! Your turn.");
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

    public void setOnHostSelected(Consumer<String> callback) { this.onHostSelected = callback; }
    public void setOnJoinSelected(BiConsumer<String, String> callback) { this.onJoinSelected = callback; }
    public void setOnMoveSelected(Consumer<Integer> callback) { this.onMoveSelected = callback; }
}