package org.example;

import java.io.Serializable;
import java.util.List;

public class GameStateMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    public List<Player> players;
    public List<Arrow> arrows;
    public Target nearTarget;
    public Target farTarget;
    public String status; // "LOBBY", "RUNNING", "PAUSED", "GAME_OVER"
    public String winnerName;

    public GameStateMessage(List<Player> players, List<Arrow> arrows,
                            Target nearTarget, Target farTarget,
                            String status, String winnerName) {
        this.players = players;
        this.arrows = arrows;
        this.nearTarget = nearTarget;
        this.farTarget = farTarget;
        this.status = status;
        this.winnerName = winnerName;
    }
}