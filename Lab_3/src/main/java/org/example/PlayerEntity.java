package org.example;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "players")
public class PlayerEntity implements Serializable {
    @Id
    @Column(name = "username", length = 50)
    private String username;

    @Column(name = "wins")
    private int wins;

    public PlayerEntity() {}

    public PlayerEntity(String username, int wins) {
        this.username = username;
        this.wins = wins;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public int getWins() { return wins; }
    public void setWins(int wins) { this.wins = wins; }
}