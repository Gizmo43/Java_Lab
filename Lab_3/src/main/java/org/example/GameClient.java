package org.example;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;

public class GameClient {
    private Socket socket;

    //потоки для сообщений
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private String username;
    private int playerId;
    private ClientGamePanel gamePanel;
    private Thread listenerThread;

    private Consumer<List<PlayerEntity>> leaderboardCallback;

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

    public void setLeaderboardCallback(Consumer<List<PlayerEntity>> callback) {
        this.leaderboardCallback = callback;
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
                    } else if ("LEADERBOARD".equals(header)) {
                        List<PlayerEntity> leaders = (List<PlayerEntity>) in.readObject();
                        if (leaderboardCallback != null) {
                            SwingUtilities.invokeLater(() -> leaderboardCallback.accept(leaders));
                        }
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

    public void requestLeaderboard() {
        sendInput(PlayerInputMessage.Type.LEADERBOARD_REQUEST);
    }

    public void sendLeaderboardDone() {
        sendInput(PlayerInputMessage.Type.LEADERBOARD_DONE);
    }

    public void sendReady() { sendInput(PlayerInputMessage.Type.READY); }
    public void sendPause() { sendInput(PlayerInputMessage.Type.PAUSE); }
    public int getPlayerId() { return playerId; }
    public String getUsername() { return username; }
}