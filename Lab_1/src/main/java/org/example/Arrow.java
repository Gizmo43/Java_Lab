package org.example;

import java.awt.*;

public class Arrow {

    int x, y;
    int speed = 10;

    public Arrow(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void move() {
        x += speed;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, 20, 4);
    }

    public void draw(Graphics g) {
        g.setColor(Color.BLUE);
        g.fillRect(x, y, 20, 4);
    }
}

