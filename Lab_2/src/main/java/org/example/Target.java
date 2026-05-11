package org.example;

import java.awt.*;
import java.io.Serializable;

public class Target implements Serializable {
    private static final long serialVersionUID = 1L;

    int x, y;
    int size;
    int speed;
    int points;

    public Target(int x, int y, int size, int speed, int points) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.speed = speed;
        this.points = points;
    }

    public void move() {
        y += speed;
    }

    public void changeDirection() {
        speed = speed * (-1);
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, size, size);
    }

    public int getSize() {
        return size;
    }

    public void draw(Graphics g) {
        g.setColor(Color.RED);
        g.fillOval(x, y, size, size);


        // 2/3 от размера
        int inWhiteSize = size * 2 / 3;
        int whiteX = x + (size - inWhiteSize) / 2;
        int whiteY = y + (size - inWhiteSize) / 2;
        g.setColor(Color.WHITE);
        g.fillOval(whiteX, whiteY, inWhiteSize, inWhiteSize);

        // 1/3 от размера
        int inRedSize = size / 3;
        int redX = x + (size - inRedSize) / 2;
        int redY = y + (size - inRedSize) / 2;
        g.setColor(Color.RED);
        g.fillOval(redX, redY, inRedSize, inRedSize);
    }
}

