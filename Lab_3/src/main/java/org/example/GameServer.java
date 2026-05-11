package org.example;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.awt.Color;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class GameServer {
    private static final int MAX_PLAYERS = 4;
    private static final int WIN_SCORE = 6;

    private ServerSocket serverSocket;

    private List<ClientHandler> clients = new Vector<>();
    private List<Player> players = new Vector<>();
    private List<Arrow> arrows = new Vector<>();

    private Target nearTarget, farTarget;
    private volatile boolean gameRunning = false;
    private volatile boolean paused = false;
    private volatile boolean leaderboardPaused = false;
    private Set<String> leaderboardRequesters = Collections.synchronizedSet(new HashSet<>());

    private int nextId = 0;

    private org.hibernate.SessionFactory sessionFactory;

    public GameServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
            sessionFactory = HibernateUtil.getSessionFactory();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //запуск сервера
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

        try (Session session = sessionFactory.openSession()) {
            PlayerEntity existing = session.get(PlayerEntity.class, username);
            if (existing == null) {
                Transaction tx = session.beginTransaction();
                PlayerEntity newEntity = new PlayerEntity(username, 0);
                session.persist(newEntity);
                tx.commit();
            }
        }

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
            leaderboardRequesters.remove(handler.player.getUsername());
        }
        broadcastState("LOBBY", null);
    }

    private void checkAllReady() {
        boolean allReady = true;
        for (ClientHandler c : clients) {
            if (!c.ready) {
                allReady = false;
                break;
            }
        }
        if (allReady && !gameRunning) {
            startGame();
        }
    }

    private synchronized void startGame() {
        //сброс очков и выстрелов
        ClientHandler.scores.clear();
        for (Player p : players) {
            p.setY(250);
            p.setShots(0);
        }
        arrows.clear();
        nearTarget = new Target(500, 250, 80, 2, 1);
        farTarget = new Target(650, 250, 40, 4, 2);
        gameRunning = true;
        paused = false;
        leaderboardPaused = false;
        leaderboardRequesters.clear();
        broadcastState("RUNNING", null);

        new Thread(new Runnable() {
            @Override
            public void run() {
                gameLoop();
            }
        }).start();
    }

    private void stopGame(String winnerName) {
        gameRunning = false;
        paused = false;
        leaderboardPaused = false;
        leaderboardRequesters.clear();
        clients.forEach(c -> c.ready = false);
        broadcastState("GAME_OVER", winnerName);

        updateWinnerStats(winnerName);
        saveGameRecord(winnerName);
    }

    // приостанавливает игру по запросу любого игрока.
    private synchronized void handlePause(String username) {
        if (!gameRunning || paused) return;
        paused = true;
        clients.forEach(c -> c.ready = false);
        broadcastState("PAUSED", null);
    }

    //для старта или снятия паузы (все должны быть готовы).
    private synchronized void handleReady(String username) {
        if (!gameRunning) {
            for (ClientHandler c : clients) {
                if (c.player != null && c.player.getUsername().equals(username)) {
                    c.ready = true;
                    break;
                }
            }
            checkAllReady();
        } else if (paused) {
            for (ClientHandler c : clients) {
                if (c.player != null && c.player.getUsername().equals(username)) {
                    c.ready = true;
                    break;
                }
            }
            boolean allReady = true;
            for (ClientHandler c : clients) {
                if (!c.ready) {
                    allReady = false;
                    break;
                }
            }
            if (allReady) {
                paused = false;
                if (!leaderboardPaused) {
                    broadcastState("RUNNING", null);
                }
            }
        }
    }

    private synchronized void handleLeaderboardRequest(String username) {
        if (gameRunning && !paused && !leaderboardPaused) {
            leaderboardPaused = true;
            broadcastState("PAUSED", null);
        }
        leaderboardRequesters.add(username);

        ClientHandler requester = null;
        for (ClientHandler c : clients) {
            if (c.player != null && c.player.getUsername().equals(username)) {
                requester = c;
                break;
            }
        }
        if (requester != null) {
            List<PlayerEntity> leaders = getLeaderboardData();
            requester.sendLeaderboard(leaders);
        }
    }

    private synchronized void handleLeaderboardDone(String username) {
        leaderboardRequesters.remove(username);
        if (leaderboardRequesters.isEmpty() && leaderboardPaused) {
            leaderboardPaused = false;
            if (!paused && gameRunning) {
                broadcastState("RUNNING", null);
            }
        }
    }

    private void handleShoot(String username) {
        if (!gameRunning || paused || leaderboardPaused) return;
        ClientHandler shooter = null;
        for (ClientHandler c : clients) {
            if (c.player != null && c.player.getUsername().equals(username)) {
                shooter = c;
                break;
            }
        }
        if (shooter == null || shooter.player == null) return;
        synchronized (arrows) {
            Player p = shooter.player;
            arrows.add(new Arrow(p.getX() + p.getSize(), p.getY() + p.getSize() / 2, p.getId()));
            p.addShot();
        }
    }

    private void gameLoop() {
        while (gameRunning) {
            if (paused || leaderboardPaused) {
                try { Thread.sleep(50); } catch (InterruptedException e) { break; }
                continue;
            }

            synchronized (this) {
                if (nearTarget != null) nearTarget.move();
                if (farTarget != null) farTarget.move();

                synchronized (arrows) {
                    Iterator<Arrow> it = arrows.iterator();
                    while (it.hasNext()) {
                        Arrow a = it.next();
                        a.move();
                        if (checkHit(a) || a.x > 800) {
                            it.remove();
                        }
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

    // храним в отдельной Map
    private int getPlayerScore(int playerId) {
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

    private void updateWinnerStats(String winnerUsername) {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            PlayerEntity winner = session.get(PlayerEntity.class, winnerUsername);
            if (winner != null) {
                winner.setWins(winner.getWins() + 1);
                session.merge(winner);
            }
            tx.commit();
        }
    }

    private void saveGameRecord(String winnerUsername) {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            GameRecord record = new GameRecord("Game_" + System.currentTimeMillis(), winnerUsername);
            session.persist(record);
            tx.commit();
        }
    }

    private List<PlayerEntity> getLeaderboardData() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("FROM PlayerEntity ORDER BY wins DESC", PlayerEntity.class)
                    .list();
        }
    }

    //обработчик клиента
    class ClientHandler implements Runnable {
        private Socket socket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        public Player player;
        public volatile boolean ready = false;
        public static Map<Integer, Integer> scores = new Hashtable<>();

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
                //логин
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
                            case LEADERBOARD_REQUEST: handleLeaderboardRequest(msg.username); break;
                            case LEADERBOARD_DONE: handleLeaderboardDone(msg.username); break;
                            case MOVE_UP:
                                if (player != null && gameRunning && !paused && !leaderboardPaused) player.moveUp();
                                break;
                            case MOVE_DOWN:
                                if (player != null && gameRunning && !paused && !leaderboardPaused) player.moveDown();
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
                out.writeObject(new HashMap<>(scores)); // playerId - score
            } catch (IOException e) {
                removePlayer(this);
            }
        }

        public void sendLeaderboard(List<PlayerEntity> leaders) {
            try {
                out.reset();
                out.writeObject("LEADERBOARD");
                out.writeObject(leaders);
            } catch (IOException e) {
                removePlayer(this);
            }
        }
    }

    public static void main(String[] args) {
        new GameServer(8888).start();
    }
}