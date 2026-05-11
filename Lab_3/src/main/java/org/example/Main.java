package org.example;

import javax.swing.*;
import java.net.URL;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                GameFrame frame = new GameFrame();
                String path = "pic/icon.png";
                URL imgURL = Thread.currentThread().getContextClassLoader().getResource(path);
                if (imgURL != null) {
                    frame.setIconImage(new ImageIcon(imgURL).getImage());
                }
                frame.setVisible(true);
            }
        });
    }
}