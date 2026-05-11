package org.example;

import java.awt.*;
import java.io.Serializable;

public class Arrow implements Serializable {
    private static final long serialVersionUID = 1L;

    int x, y;
    int speed = 10;
    private int ownerId;

    public Arrow(int x, int y, int ownerId) {
        this.x = x;
        this.y = y;
        this.ownerId = ownerId;
    }

    public void move() {
        x += speed;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, 20, 4);
    }

    public void draw(Graphics g) {
        g.setColor(Color.BLUE);
        g.fillRect(x, y, 20, 4);
    }
}

