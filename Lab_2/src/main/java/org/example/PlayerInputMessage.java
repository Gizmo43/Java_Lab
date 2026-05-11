package org.example;

import java.io.Serializable;

public class PlayerInputMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type { READY, PAUSE, SHOOT, MOVE_UP, MOVE_DOWN, STOP_MOVE }
    public Type type;
    public String username;

    public PlayerInputMessage(Type type, String username) {
        this.type = type;
        this.username = username;
    }
}