package org.example;

import java.awt.*;

public class Player {

    private int x;
    private int y;
    private int size = 40;
    private int speed = 8;

    public Player(int x, int y, int size, int speed) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.speed = speed;
    }

    public void moveUp() {
        y -= speed;
    }

    public void moveDown() {
        y += speed;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void draw(Graphics g) {
        g.setColor(Color.BLACK);
        // треугольник, направленный вправо
        int[] xPoints = {x, x, x + size};          // левая-верхняя, левая-нижняя, правая
        int[] yPoints = {y, y + size, y + size / 2};
        g.fillPolygon(xPoints, yPoints, 3);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getSize() {
        return size;
    }
}
