package org.example;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.List;

public class GameFrame extends JFrame {
    private GameClient client;
    private ClientGamePanel gamePanel;
    private JButton readyBtn, pauseBtn, leaderBtn;
    private JLabel statusLabel;

    public GameFrame() {
        setTitle("Меткий стрелок - Multiplayer");
        setSize(900, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        //логина
        JPanel loginPanel = new JPanel();
        JTextField nameField = new JTextField(15);
        JButton connectBtn = new JButton("Подключиться");
        loginPanel.add(new JLabel("Имя:"));
        loginPanel.add(nameField);
        loginPanel.add(connectBtn);
        add(loginPanel, BorderLayout.NORTH);

        // панель
        gamePanel = new ClientGamePanel(null);
        add(gamePanel, BorderLayout.CENTER);
        gamePanel.requestFocusInWindow();

        //управление
        JPanel controlPanel = new JPanel();
        readyBtn = new JButton("Готов");
        pauseBtn = new JButton("Пауза");
        leaderBtn = new JButton("Лидеры");
        statusLabel = new JLabel("Не подключены");
        controlPanel.add(readyBtn);
        controlPanel.add(pauseBtn);
        controlPanel.add(leaderBtn);
        controlPanel.add(statusLabel);
        readyBtn.setEnabled(false);
        pauseBtn.setEnabled(false);
        leaderBtn.setEnabled(false);
        add(controlPanel, BorderLayout.SOUTH);

        //подключение
        connectBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) return;
            try {
                client = new GameClient("localhost", 8888, name, gamePanel);
                gamePanel = new ClientGamePanel(client);
                client.setLeaderboardCallback(this::showLeaderboard);
                add(gamePanel, BorderLayout.CENTER);
                client.startListener();
                readyBtn.setEnabled(true);
                pauseBtn.setEnabled(true);
                leaderBtn.setEnabled(true);
                connectBtn.setEnabled(false);
                nameField.setEnabled(false);
                statusLabel.setText("Подключены как " + name);
                gamePanel.requestFocusInWindow();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Ошибка подключения: " + ex.getMessage());
            }
        });

        readyBtn.addActionListener(e -> {
            if (client != null) client.sendReady();
            gamePanel.requestFocusInWindow();
        });
        pauseBtn.addActionListener(e -> {
            if (client != null) client.sendPause();
            gamePanel.requestFocusInWindow();
        });
        leaderBtn.addActionListener(e -> {
            if (client != null) client.requestLeaderboard();
            gamePanel.requestFocusInWindow();
        });
    }

    private void showLeaderboard(List<PlayerEntity> leaders) {
        LeaderboardDialog dialog = new LeaderboardDialog(this, leaders);
        dialog.setVisible(true);
        if (client != null) {
            client.sendLeaderboardDone();
        }
    }
}