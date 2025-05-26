package com.example.wordle_project;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface GameResultDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(GameResult result);

    /** Общее количество игр для игрока */
    @Query("SELECT COUNT(*) FROM results WHERE playerId = :playerId")
    int countGames(int playerId);

    /** Количество побед для игрока */
    @Query("SELECT SUM(CASE WHEN win = 1 THEN 1 ELSE 0 END) FROM results WHERE playerId = :playerId")
    int countWins(int playerId);

    /** Средний коэффициент успеха для игрока */
    @Query("SELECT AVG(coefficient) FROM results WHERE playerId = :playerId")
    float averageCoefficient(int playerId);

    /** Сводная статистика по всем игрокам: имя, игр, побед, средний коэффициент */
    @Query(
            "SELECT p.name AS name, " +
                    "COUNT(r.uid) AS games, " +
                    "SUM(CASE WHEN r.win = 1 THEN 1 ELSE 0 END) AS wins, " +
                    "AVG(r.coefficient) AS avgCoef " +
                    "FROM results r " +
                    "JOIN players p ON p.id = r.playerId " +
                    "GROUP BY r.playerId " +
                    "ORDER BY avgCoef DESC"
    )
    List<RatingStats> getRatingStats();
    @Query("SELECT SUM(coefficient) FROM results WHERE playerId = :playerId")
    float sumCoefficient(int playerId);

    @Query("SELECT * FROM results WHERE playerId = :playerId")
    List<GameResult> getPlayerResults(int playerId);
}