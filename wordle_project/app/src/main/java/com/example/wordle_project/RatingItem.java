package com.example.wordle_project;

/**
 * Модель элемента рейтинга для RecyclerView
 */
public class RatingItem {
    private final String name;
    private final int wins;
    private final int games;
    private final float avgCoef;

    /**
     * @param name     имя игрока
     * @param wins     количество побед
     * @param games    общее количество игр
     * @param avgCoef  средний коэффициент
     */
    public RatingItem(String name, int wins, int games, float avgCoef) {
        this.name = name;
        this.wins = wins;
        this.games = games;
        this.avgCoef = avgCoef;
    }

    public String getName() {
        return name;
    }

    public int getWins() {
        return wins;
    }

    public int getGames() {
        return games;
    }

    public float getAvgCoef() {
        return avgCoef;
    }
}