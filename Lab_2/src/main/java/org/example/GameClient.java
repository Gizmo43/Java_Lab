package org.example;

import java.io.*;
import java.net.Socket;
import java.util.Map;

public class GameClient {
    private Socket socket;

    //потоки для сообщений
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private String username;
    private int playerId;
    private ClientGamePanel gamePanel;
    private Thread listenerThread;

    public GameClient(String host, int port, String username, ClientGamePanel gamePanel) throws IOException {
        this.username = username;
        this.gamePanel = gamePanel;
        //создание сокета
        socket = new Socket(host, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        //логин
        try {
            out.writeObject(new LoginMessage(username));

            Object response = in.readObject();
            if ("REJECT_NAME".equals(response) || "REJECT_MAX".equals(response)) {
                throw new IOException("Login rejected: " + response);
            }
            //получаем id
            playerId = (int) in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException("Invalid server response", e); //особые исключения для ввода/вывода и сетевых соединений
        }
    }

    //поток отвечает за прослушивание событий
    public void startListener() {
        listenerThread = new Thread(() -> {
            try {
                while (true) {
                    Object header = in.readObject();
                    if ("STATE".equals(header)) {
                        GameStateMessage state = (GameStateMessage) in.readObject();
                        Map<Integer, Integer> scores = (Map<Integer, Integer>) in.readObject();
                        gamePanel.updateState(state, scores);
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Disconnected from server.");
            }
        });
        listenerThread.start();
    }

    //отправка сообщений
    public void sendInput(PlayerInputMessage.Type type) {
        try {
            out.writeObject(new PlayerInputMessage(type, username));
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendReady() { sendInput(PlayerInputMessage.Type.READY); }
    public void sendPause() { sendInput(PlayerInputMessage.Type.PAUSE); }
    public int getPlayerId() { return playerId; }
    public String getUsername() { return username; }
}