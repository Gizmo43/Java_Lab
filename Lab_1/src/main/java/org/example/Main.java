package org.example;

import javax.swing.*;
import java.net.URL;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameFrame frame = new GameFrame();

            String path = "pic/icon.png";
            URL imgURL = Thread.currentThread().getContextClassLoader()
                    .getResource(path);
            ImageIcon icon = new ImageIcon(imgURL);
            frame.setIconImage(icon.getImage());

            frame.setVisible(true);
        });
    }
}

