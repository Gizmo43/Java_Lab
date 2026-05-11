package org.example;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.awt.Color;

public class GameServer {
    private static final int MAX_PLAYERS = 4;
    private static final int WIN_SCORE = 6;

    private ServerSocket serverSocket;
    private List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private List<Player> players = new CopyOnWriteArrayList<>();

    private Target nearTarget, farTarget;
    private List<Arrow> arrows = new CopyOnWriteArrayList<>();
    private volatile boolean gameRunning = false;
    private volatile boolean paused = false;
    private Set<String> pauseRequesters = Collections.synchronizedSet(new HashSet<>());

    private int nextId = 0;

    public GameServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //запуск зервера
    public void start() {
        System.out.println("Server started on port " + serverSocket.getLocalPort());
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                if (clients.size() >= MAX_PLAYERS) {
                    new ObjectOutputStream(clientSocket.getOutputStream()).writeObject("REJECT_MAX");
                    clientSocket.close();
                    continue;
                }
                //поток на каждого клиента
                ClientHandler handler = new ClientHandler(clientSocket);
                clients.add(handler);
                new Thread(handler).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized boolean addPlayer(String username, ClientHandler handler) {
        if (players.stream().anyMatch(p -> p.getUsername().equals(username)))
            return false;
        Color[] colors = {Color.BLUE, Color.GREEN, Color.ORANGE, Color.MAGENTA};
        Color color = colors[players.size() % colors.length];
        Player player = new Player(nextId++, username, color, 50, 250, 40, 8);
        players.add(player);
        handler.player = player;
        return true;
    }

    private synchronized void removePlayer(ClientHandler handler) {
        clients.remove(handler);
        if (handler.player != null) {
            players.remove(handler.player);
            pauseRequesters.remove(handler.player.getUsername());
        }
        broadcastState("LOBBY", null);
    }

    private void checkAllReady() {
        if (clients.stream().allMatch(c -> c.ready) && !gameRunning) {
            startGame();
        }
    }

    private synchronized void startGame() {
        //сброс очков и выстрелов
        ClientHandler.scores.clear();
        for (Player p : players) {
            p.setY(250);
        }
        arrows.clear();
        nearTarget = new Target(500, 250, 80, 2, 1);
        farTarget = new Target(650, 250, 40, 4, 2);
        gameRunning = true;
        paused = false;
        pauseRequesters.clear();
        broadcastState("RUNNING", null);

        new Thread(this::gameLoop).start();
    }

    private void stopGame(String winnerName) {
        gameRunning = false;
        paused = false;
        pauseRequesters.clear();
        clients.forEach(c -> c.ready = false);
        broadcastState("GAME_OVER", winnerName);
    }

    private synchronized void handlePause(String username) {
        if (!gameRunning) return;
        pauseRequesters.add(username);
        paused = true;
        broadcastState("PAUSED", null);

        clients.stream().filter(c -> c.player != null && c.player.getUsername().equals(username))
                .findFirst().ifPresent(c -> c.ready = false);
    }

    private synchronized void handleReady(String username) {
        if (!gameRunning) {
            clients.stream().filter(c -> c.player != null && c.player.getUsername().equals(username))
                    .findFirst().ifPresent(c -> c.ready = true);
            checkAllReady();
        } else {
            //только тот, кто поставил паузу, может снять
            if (paused && pauseRequesters.contains(username)) {
                pauseRequesters.remove(username);
                if (pauseRequesters.isEmpty()) {
                    paused = false;
                    broadcastState("RUNNING", null);
                    // После снятия паузы все считаются готовыми, ready не важен
                } else {
                    broadcastState("PAUSED", null);
                }
            }
        }
    }

    private void handleShoot(String username) {
        if (!gameRunning || paused) return;
        ClientHandler shooter = clients.stream()
                .filter(c -> c.player != null && c.player.getUsername().equals(username))
                .findFirst().orElse(null);
        if (shooter == null || shooter.player == null) return;
        synchronized (arrows) {
            Player p = shooter.player;
            arrows.add(new Arrow(p.getX() + p.getSize(), p.getY() + p.getSize() / 2, p.getId()));
        }
    }

    private void gameLoop() {
        while (gameRunning) {
            if (paused) {
                try { Thread.sleep(50); } catch (InterruptedException e) { break; }
                continue;
            }

            synchronized (this) {
                if (nearTarget != null) nearTarget.move();
                if (farTarget != null) farTarget.move();

                //стрелы
                for (Arrow a : arrows) {
                    a.move();
                    if (checkHit(a) || a.x > 800) {
                        arrows.remove(a);
                    }
                }

                //рикошет целей
                if (nearTarget != null && (nearTarget.y < 0 || nearTarget.y > 600 - nearTarget.getSize()))
                    nearTarget.changeDirection();
                if (farTarget != null && (farTarget.y < 0 || farTarget.y > 600 - farTarget.getSize()))
                    farTarget.changeDirection();

                //проверка победителя
                for (Player p : players) {
                    if (getPlayerScore(p.getId()) >= WIN_SCORE) {
                        stopGame(p.getUsername());
                        break;
                    }
                }
            }

            broadcastState("RUNNING", null);

            try { Thread.sleep(20); } catch (InterruptedException e) { break; }
        }
    }

    private boolean checkHit(Arrow arrow) {
        if (nearTarget != null && arrow.getBounds().intersects(nearTarget.getBounds())) {
            addScore(arrow.getOwnerId(), nearTarget.points);
            return true;
        } else if (farTarget != null && arrow.getBounds().intersects(farTarget.getBounds())) {
            addScore(arrow.getOwnerId(), farTarget.points);
            return true;
        }
        return false;
    }

    private int getPlayerScore(int playerId) {
        // храним в отдельной Map
        return ClientHandler.scores.getOrDefault(playerId, 0);
    }

    private void addScore(int playerId, int points) {
        ClientHandler.scores.merge(playerId, points, Integer::sum);
    }

    private void broadcastState(String status, String winner) {
        GameStateMessage msg = new GameStateMessage(
                new ArrayList<>(players),
                new ArrayList<>(arrows),
                nearTarget, farTarget,
                status, winner
        );
        for (ClientHandler client : clients) {
            client.sendState(msg);
        }
    }

    //обработчик клиента
    class ClientHandler implements Runnable {
        private Socket socket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        public Player player;
        public volatile boolean ready = false;
        public static Map<Integer, Integer> scores = new ConcurrentHashMap<>();

        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
            } catch (IOException e) { e.printStackTrace(); }
        }

        @Override
        public void run() {
            try {
                //логина
                Object loginObj = in.readObject();
                if (loginObj instanceof LoginMessage) {
                    String username = ((LoginMessage) loginObj).username;
                    if (!addPlayer(username, this)) {
                        out.writeObject("REJECT_NAME");
                        socket.close();
                        return;
                    }
                    out.writeObject("OK");
                    out.writeObject(player.getId());
                }

                //отправка состояния
                broadcastState("LOBBY", null);

                //прием сообщений
                while (true) {
                    Object cmd = in.readObject();
                    if (cmd instanceof PlayerInputMessage) {
                        PlayerInputMessage msg = (PlayerInputMessage) cmd;
                        switch (msg.type) {
                            case READY: handleReady(msg.username); break;
                            case PAUSE: handlePause(msg.username); break;
                            case SHOOT: handleShoot(msg.username); break;
                            case MOVE_UP:
                                if (player != null && gameRunning && !paused) player.moveUp();
                                break;
                            case MOVE_DOWN:
                                if (player != null && gameRunning && !paused) player.moveDown();
                                break;
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Client disconnected: " + (player != null ? player.getUsername() : "unknown"));
            } finally {
                removePlayer(this);
                try { socket.close(); } catch (IOException ignored) {}
            }
        }

        public void sendState(GameStateMessage msg) {
            try {
                out.reset(); // чтобы не кэшировались объекты
                out.writeObject("STATE");
                out.writeObject(msg);
                out.writeObject(new HashMap<>(scores)); // playerId -> score
            } catch (IOException e) {
                removePlayer(this);
            }
        }
    }

    public static void main(String[] args) {
        new GameServer(8888).start();
    }
}