package org.example;

import javax.swing.*;
import java.awt.*;

public class GameFrame extends JFrame {

    public GameFrame() {
        setTitle("Меткий стрелок");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        GamePanel panel = new GamePanel();
        add(panel, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel();

        JButton startBtn = new JButton("Старт");
        JButton stopBtn = new JButton("Стоп");
        JButton pauseBtn = new JButton("Пауза");

        controlPanel.add(startBtn);
        controlPanel.add(stopBtn);
        controlPanel.add(pauseBtn);
        pauseBtn.setFocusable(false);

        add(controlPanel, BorderLayout.SOUTH);

        startBtn.addActionListener(e -> panel.startGame());
        stopBtn.addActionListener(e -> panel.stopGame());
        pauseBtn.addActionListener(e -> panel.pauseGame());
    }
}

