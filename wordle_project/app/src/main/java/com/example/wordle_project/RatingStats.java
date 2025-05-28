package com.example.wordle_project;

public class RatingStats {
    public final String name;
    public final int games;
    public final int wins;
    public final float avgCoef;

    public RatingStats(String name, int games, int wins, float avgCoef) {
        this.name = name;
        this.games = games;
        this.wins = wins;
        this.avgCoef = avgCoef;
    }
}
