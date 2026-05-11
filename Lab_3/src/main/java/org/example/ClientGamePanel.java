package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.*;
import java.util.List;

public class ClientGamePanel extends JPanel {
    private GameStateMessage state;
    private Map<Integer, Integer> scores = new HashMap<>();
    private GameClient client;
    private String previousStatus = null;

    private boolean upPressed = false;
    private boolean downPressed = false;
    private boolean spacePressed = false;

    public ClientGamePanel(GameClient client) {
        this.client = client;
        setBackground(Color.WHITE);
        setFocusable(true);
        addKeyListener(new KeyHandler());
    }

    public void updateState(GameStateMessage state, Map<Integer, Integer> scores) {
        this.state = state;
        this.scores = scores;
        if ("GAME_OVER".equals(state.status) && !"GAME_OVER".equals(previousStatus)
                && state.winnerName != null) {
            JOptionPane.showMessageDialog(this,
                    "Победитель: " + state.winnerName,
                    "Игра окончена",
                    JOptionPane.INFORMATION_MESSAGE);
        }
        previousStatus = state.status;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (state == null) {
            g.drawString("Waiting for game...", 50, 50);
            return;
        }

        //мишени
        if (state.nearTarget != null) state.nearTarget.draw(g);
        if (state.farTarget != null) state.farTarget.draw(g);

        //игроки
        for (Player p : state.players) {
            p.draw(g);
        }

        //стрелы
        for (Arrow a : state.arrows) {
            a.draw(g);
        }

        //информация о статусе
        g.setColor(Color.BLACK);
        g.drawString("Status: " + state.status, 20, 20);
        if ("GAME_OVER".equals(state.status) && state.winnerName != null) {
            g.drawString("Winner: " + state.winnerName, 20, 40);
        }

        //счёт и выстрелы
        int y = 60;
        for (Player p : state.players) {
            int s = scores.getOrDefault(p.getId(), 0);
            g.drawString(p.getUsername() + ": " + s + " очков, выстрелов: " + p.getShots(), 20, y);
            y += 20;
        }
    }

    //никакой логики, просто отправляет сообщения
    private class KeyHandler implements KeyListener {
        @Override
        public void keyPressed(KeyEvent e) {
            if (client == null) return;
            switch (e.getKeyCode()) {
                case KeyEvent.VK_UP:
                    if (!upPressed) client.sendInput(PlayerInputMessage.Type.MOVE_UP);
                    upPressed = true;
                    break;
                case KeyEvent.VK_DOWN:
                    if (!downPressed) client.sendInput(PlayerInputMessage.Type.MOVE_DOWN);
                    downPressed = true;
                    break;
                case KeyEvent.VK_SPACE:
                    if (!spacePressed) client.sendInput(PlayerInputMessage.Type.SHOOT);
                    spacePressed = true;
                    break;
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_UP:
                    upPressed = false;
                    client.sendInput(PlayerInputMessage.Type.STOP_MOVE);
                    break;
                case KeyEvent.VK_DOWN:
                    downPressed = false;
                    client.sendInput(PlayerInputMessage.Type.STOP_MOVE);
                    break;
                case KeyEvent.VK_SPACE:
                    spacePressed = false;
                    break;
            }
        }

        @Override
        public void keyTyped(KeyEvent e) {}
    }
}