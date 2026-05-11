package org.example;

import java.awt.*;
import java.io.Serializable;

public class Player implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String username;
    private Color color;

    private int x;
    private int y;
    private int size;
    private int speed;

    public Player(int id, String username, Color color, int x, int y, int size, int speed) {
        this.id = id;
        this.username = username;
        this.color = color;
        this.x = x;
        this.y = y;
        this.size = size;
        this.speed = speed;
    }

    public void moveUp() { y -= speed; }
    public void moveDown() { y += speed; }

    public void setY(int y) { this.y = y; }
    public void setX(int x) { this.x = x; }

    public int getId() { return id; }
    public String getUsername() { return username; }   // <-- добавлен
    public int getX() { return x; }
    public int getY() { return y; }
    public int getSize() { return size; }

    public void draw(Graphics g) {
        g.setColor(color);
        int[] xPoints = {x, x, x + size};
        int[] yPoints = {y, y + size, y + size / 2};
        g.fillPolygon(xPoints, yPoints, 3);
        g.setColor(Color.BLACK);
        g.drawString(username, x, y - 5);
    }
}