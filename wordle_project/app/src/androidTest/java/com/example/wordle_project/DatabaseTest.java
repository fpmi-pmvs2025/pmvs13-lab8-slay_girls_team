package com.example.wordle_project;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class DatabaseTest {
    private AppDatabase db;
    private PlayerDao playerDao;
    private GameResultDao gameResultDao;

    @Before
    public void createDb() {
        db = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                AppDatabase.class
        ).allowMainThreadQueries().build();
        playerDao = db.playerDao();
        gameResultDao = db.gameResultDao();
    }

    @After
    public void closeDb() throws IOException {
        db.close();
    }

    // Проверка вставки игрока и его извлечения из базы данных
    @Test
    public void testPlayerInsertAndRetrieve() {
        Player player = new Player(1, "Test Player");
        playerDao.insert(player);

        Player loaded = playerDao.findById(1);
        assertNotNull(loaded);
        assertEquals("Test Player", loaded.name);
    }

    // Проверка вставки результатов игр и подсчета статистики
    @Test
    public void testGameResultInsertAndStats() {
        Player player = new Player(10, "Girls");
        playerDao.insert(player);

        // Добавляем результаты игр
        gameResultDao.insert(new GameResult(10, true, 3, 0.7f, System.currentTimeMillis()));
        gameResultDao.insert(new GameResult(10, false, 6, 0f, System.currentTimeMillis()));
        gameResultDao.insert(new GameResult(10, true, 2, 0.8f, System.currentTimeMillis()));

        // Проверяем статистику
        int games = gameResultDao.countGames(10);
        assertEquals(3, games);

        int wins = gameResultDao.countWins(10);
        assertEquals(2, wins);

        float avgCoef = gameResultDao.averageCoefficient(10);
        assertEquals(0.5f, avgCoef, 0.01f);
    }

    // Проверка расчета статистики рейтинга для игроков
    @Test
    public void testRatingStatsCalculation() {
        // Добавляем двух игроков с результатами
        Player player1 = new Player(1, "Player 1");
        Player player2 = new Player(2, "Player 2");
        playerDao.insert(player1);
        playerDao.insert(player2);

        // Игрок 1: 2 победы из 3 игр
        gameResultDao.insert(new GameResult(1, true, 2, 0.8f, System.currentTimeMillis()));
        gameResultDao.insert(new GameResult(1, false, 6, 0f, System.currentTimeMillis()));
        gameResultDao.insert(new GameResult(1, true, 3, 0.7f, System.currentTimeMillis()));

        // Игрок 2: 1 победа из 2 игр
        gameResultDao.insert(new GameResult(2, true, 1, 1.0f, System.currentTimeMillis()));
        gameResultDao.insert(new GameResult(2, false, 6, 0f, System.currentTimeMillis()));

        List<RatingStats> stats = gameResultDao.getRatingStats();
        assertEquals(2, stats.size());

        assertEquals("Player 1", stats.get(0).name);
        assertEquals(0.5f, stats.get(0).avgCoef, 0.01f);
    }
}