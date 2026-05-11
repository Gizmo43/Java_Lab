package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class GamePanel extends JPanel implements Runnable {
    // поток
    private Thread gameThread;
    private final Object lock = new Object();
    // флаги
    private boolean paused = false;

    private boolean upPressed = false;
    private boolean downPressed = false;
    private boolean spacePressed = false;


    // объекты
    private Target nearTarget;
    private Target farTarget;
    private List<Arrow> arrows;  // массив стрел
    private Player player;

    // счетчики
    private int score = 0;
    private int shots = 0;


    public GamePanel() {
        setBackground(Color.WHITE);
        setFocusable(true);
        addKeyListener(new KeyAdapter());
        requestFocusInWindow();
    }

    // управление игрой (старт, стоп, пауза)

    public void startGame() {

        if (gameThread != null && gameThread.isAlive()) {
            stopGame();                      // перезапуск
        }

        score = 0;
        shots = 0;
        player = new Player(50, 250, 40, 8);
        nearTarget = new Target(500, 250, 80, 2, 1);
        farTarget = new Target(650, 250, 40, 4, 2);

        arrows = Collections.synchronizedList(new ArrayList<>()); // для работы с потоками

        paused = false;

        requestFocusInWindow();


        gameThread = new Thread(this);
        gameThread.start();
        requestFocusInWindow();
    }

    public void stopGame() {
        if (gameThread != null) {
            gameThread.interrupt();          // прерываем поток
            try {
                gameThread.join();           // дожидаемся завершения
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            gameThread = null;
        }
    }
    public void pauseGame() {
        synchronized (lock) {
            paused = !paused;
            if (!paused) {
                lock.notifyAll();
            }
        }
    }


    //выстрел
    public void shoot() {
        synchronized (arrows) {
            arrows.add(new Arrow(
                    player.getX() + player.getSize(),
                    player.getY() + player.getSize() / 2
            ));
        }
        shots++;
    }


    @Override
    protected void paintComponent(Graphics g) { // отрисовка, вызывается repaint()
        super.paintComponent(g);


        if (player != null) player.draw(g);
        if (nearTarget != null) nearTarget.draw(g);
        if (farTarget != null) farTarget.draw(g);

        if (arrows != null) {
            synchronized (arrows) {
                for (Arrow a : arrows) {
                    a.draw(g);
                }
            }
        }

        g.setColor(Color.BLACK);
        g.drawString("Очки: " + score, 20, 20);
        g.drawString("Выстрелы: " + shots, 20, 40);
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {

            synchronized (lock) {
                while (paused) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }


            nearTarget.move();
            farTarget.move();

            // движение игрока
            if (upPressed) {
                player.moveUp();
            }
            if (downPressed) {
                player.moveDown();
            }
            // ограничение
            if (player.getY() < 50) {
                player.setY(50);
            }
            if (player.getY() > getHeight() - player.getSize()) {
                player.setY(getHeight() - player.getSize());
            }

            // выстрел
            if (spacePressed) {
                shoot();
                spacePressed = false; // один за раз
            }

            // проверка состояния стрел
            synchronized (arrows) {
                Iterator<Arrow> it = arrows.iterator(); // вместо обычного for т.к. стрелы удаляются
                while (it.hasNext()) {
                    Arrow a = it.next();
                    a.move();

                    if (checkHit(a)) {
                        it.remove();          // попадание
                        continue;
                    }

                    if (a.x > getWidth()) {
                        it.remove();          // промах
                    }
                }
            }

            // рикошет целей
            if (nearTarget.y < 0 || nearTarget.y > getHeight() - nearTarget.getSize()) nearTarget.changeDirection();
            if (farTarget.y < 0 || farTarget.y > getHeight() - farTarget.getSize()) farTarget.changeDirection();



            // отрисовка
            repaint();

            try {
                Thread.sleep(20); // игровой тик
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }


    // проверка попадания
    private boolean checkHit(Arrow arrow) {
        if (arrow.getBounds().intersects(nearTarget.getBounds())) {
            score += nearTarget.points;
            return true;
        } else if (arrow.getBounds().intersects(farTarget.getBounds())) {
            score += farTarget.points;
            return true;
        }
        return false;
    }


    // обработка клавиш
    private class KeyAdapter implements KeyListener {
        @Override
        public void keyPressed(KeyEvent e) {
            if (Thread.currentThread().isInterrupted()) return;

            switch (e.getKeyCode()) {
                case KeyEvent.VK_UP:
                    upPressed = true;
                    break;
                case KeyEvent.VK_DOWN:
                    downPressed = true;
                    break;
                case KeyEvent.VK_SPACE:
                    spacePressed = true;
                    break;
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            if (Thread.currentThread().isInterrupted()) return;

            switch (e.getKeyCode()) {
                case KeyEvent.VK_UP:
                    upPressed = false;
                    break;
                case KeyEvent.VK_DOWN:
                    downPressed = false;
                    break;
                case KeyEvent.VK_SPACE:
                    spacePressed = false;
                    break;
            }
        }

        @Override
        public void keyTyped(KeyEvent e) {} // нужно т.к. класс абстрактный
    }



}

