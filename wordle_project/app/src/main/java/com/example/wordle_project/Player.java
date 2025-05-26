package com.example.wordle_project;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "players")
public class Player {
    @PrimaryKey
    public int id;
    @NonNull public String name;
    public int gamesCount = 0;
    public float totalCoef = 0f;

    public Player(int id, @NonNull String name) {
        this.id = id;
        this.name = name;
    }

    // Пустой конструктор для Room
    public Player() {}
}
