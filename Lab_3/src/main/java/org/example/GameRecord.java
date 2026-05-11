package org.example;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "game_records")
public class GameRecord implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "game_name")
    private String gameName;

    @Column(name = "winner_username")
    private String winnerUsername;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "played_at")
    private Date playedAt;

    public GameRecord() {}

    public GameRecord(String gameName, String winnerUsername) {
        this.gameName = gameName;
        this.winnerUsername = winnerUsername;
        this.playedAt = new Date();
    }

    // геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getGameName() { return gameName; }
    public void setGameName(String gameName) { this.gameName = gameName; }
    public String getWinnerUsername() { return winnerUsername; }
    public void setWinnerUsername(String winnerUsername) { this.winnerUsername = winnerUsername; }
    public Date getPlayedAt() { return playedAt; }
    public void setPlayedAt(Date playedAt) { this.playedAt = playedAt; }
}