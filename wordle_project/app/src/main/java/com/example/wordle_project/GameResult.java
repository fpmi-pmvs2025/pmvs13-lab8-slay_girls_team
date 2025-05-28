package com.example.wordle_project;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName="results",
        foreignKeys = @ForeignKey(
                entity = Player.class,
                parentColumns = "id",
                childColumns  = "playerId",
                onDelete      = ForeignKey.CASCADE
        ),
        indices = @Index("playerId")
)
public class GameResult {
    @PrimaryKey(autoGenerate = true)
    public int uid;             // теперь будет уникальным для каждой вставки

    public int playerId;
    public boolean win;
    public int attempts;
    public float coefficient;
    public long timestamp;

    public GameResult(int playerId, boolean win, int attempts, float coefficient, long timestamp) {
        this.playerId   = playerId;
        this.win        = win;
        this.attempts   = attempts;
        this.coefficient= coefficient;
        this.timestamp  = timestamp;
    }
}