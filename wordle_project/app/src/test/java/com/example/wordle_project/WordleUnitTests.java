package com.example.wordle_project;

import static com.example.wordle_project.GameActivity.COLOR_CORRECT;
import static com.example.wordle_project.GameActivity.COLOR_PRESENT;
import static com.example.wordle_project.GameActivity.COLOR_WRONG;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class WordleUnitTests {

    // Database Tests
    private AppDatabase db;
    private PlayerDao playerDao;
    private GameResultDao gameResultDao;

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration()
                .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                .build();
        playerDao = db.playerDao();
        gameResultDao = db.gameResultDao();
    }

    @After
    public void cleanup() {
        if (db != null && db.isOpen()) {
            db.close();
        }
    }

    // Database Entity Tests
    @Test
    public void testPlayerCreation() {
        Player player = new Player(1, "Test Player");
        assertEquals(1, player.id);
        assertEquals("Test Player", player.name);
        assertEquals(0, player.gamesCount);
        assertEquals(0f, player.totalCoef, 0.001f);
    }

    @Test
    public void testGameResultCreation() {
        long timestamp = System.currentTimeMillis();
        GameResult result = new GameResult(1, true, 3, 0.8f, timestamp);

        assertEquals(1, result.playerId);
        assertTrue(result.win);
        assertEquals(3, result.attempts);
        assertEquals(0.8f, result.coefficient, 0.001f);
        assertEquals(timestamp, result.timestamp);
    }

    // Database DAO Tests
    @Test
    public void testPlayerDaoInsertAndRetrieve() {
        Player player = new Player(1, "Test Player");
        playerDao.insert(player);

        Player retrieved = playerDao.findById(1);
        assertNotNull(retrieved);
        assertEquals("Test Player", retrieved.name);
    }

    @Test
    public void testPlayerDaoUpdate() {
        Player player = new Player(1, "Test Player");
        playerDao.insert(player);

        player.name = "Updated Name";
        playerDao.update(player);

        Player updated = playerDao.findById(1);
        assertEquals("Updated Name", updated.name);
    }

    @Test
    public void testPlayerDaoGetAll() {
        playerDao.insert(new Player(1, "Player 1"));
        playerDao.insert(new Player(2, "Player 2"));

        List<Player> players = playerDao.getAll();
        assertEquals(2, players.size());
    }

    @Test
    public void testGameResultDaoStats() {
        playerDao.insert(new Player(1, "Test Player"));

        GameResult result1 = new GameResult(1, true, 2, 0.9f, System.currentTimeMillis());
        GameResult result2 = new GameResult(1, false, 0, 0f, System.currentTimeMillis());
        gameResultDao.insert(result1);
        gameResultDao.insert(result2);

        assertEquals(2, gameResultDao.countGames(1));
        assertEquals(1, gameResultDao.countWins(1));
        assertEquals(0.45f, gameResultDao.averageCoefficient(1), 0.001f);
    }

    @Test
    public void testGameResultDaoSumCoefficient() {
        playerDao.insert(new Player(1, "Test Player"));
        gameResultDao.insert(new GameResult(1, true, 2, 0.9f, System.currentTimeMillis()));
        gameResultDao.insert(new GameResult(1, true, 3, 0.6f, System.currentTimeMillis()));

        assertEquals(1.5f, gameResultDao.sumCoefficient(1), 0.001f);
    }

    @Test
    public void testGameResultDaoGetPlayerResults() {
        playerDao.insert(new Player(1, "Test Player"));
        gameResultDao.insert(new GameResult(1, true, 2, 0.9f, System.currentTimeMillis()));

        List<GameResult> results = gameResultDao.getPlayerResults(1);
        assertEquals(1, results.size());
        assertEquals(0.9f, results.get(0).coefficient, 0.001f);
    }

    @Test
    public void testGameResultDaoGetRatingStats() {
        playerDao.insert(new Player(1, "Player 1"));
        playerDao.insert(new Player(2, "Player 2"));

        gameResultDao.insert(new GameResult(1, true, 2, 0.9f, System.currentTimeMillis()));
        gameResultDao.insert(new GameResult(2, true, 3, 0.6f, System.currentTimeMillis()));
        gameResultDao.insert(new GameResult(2, false, 0, 0f, System.currentTimeMillis()));

        List<RatingStats> stats = gameResultDao.getRatingStats();
        assertEquals(2, stats.size());
        assertEquals("Player 1", stats.get(0).name);
        assertEquals(1, stats.get(0).games);
        assertEquals(1, stats.get(0).wins);
    }

    // Model Tests
    @Test
    public void testRatingItemCreation() {
        RatingItem item = new RatingItem("Player 1", 5, 10, 0.75f);
        assertEquals("Player 1", item.getName());
        assertEquals(5, item.getWins());
        assertEquals(10, item.getGames());
        assertEquals(0.75f, item.getAvgCoef(), 0.001f);
    }

    @Test
    public void testRatingStatsCreation() {
        RatingStats stats = new RatingStats("Player 1", 10, 5, 0.75f);
        assertEquals("Player 1", stats.name);
        assertEquals(10, stats.games);
        assertEquals(5, stats.wins);
        assertEquals(0.75f, stats.avgCoef, 0.001f);
    }

    // Game Logic Tests
    @Test
    public void testGameActivityWordCheck() {
        GameActivity gameActivity = new GameActivity();
        gameActivity.targetWord = "APPLE";

        assertTrue(gameActivity.checkWord("APPLE"));
        assertFalse(gameActivity.checkWord("APRIL"));
        assertFalse(gameActivity.checkWord("APP"));
        assertFalse(gameActivity.checkWord("APPLES"));
    }

    // Adapter Tests
    @Test
    public void testRatingAdapterItemCount() {
        RatingAdapter adapter = new RatingAdapter();
        List<RatingItem> items = Arrays.asList(
                new RatingItem("Player 1", 5, 10, 0.75f),
                new RatingItem("Player 2", 3, 8, 0.5f)
        );
        adapter.setItems(items);
        assertEquals(2, adapter.getItemCount());
    }

    @Test
    public void testIsValidWord() {
        GameActivity activity = new GameActivity();
        activity.loadedDictionary = Arrays.asList("apple", "banana");
        assertTrue(activity.isValidWord("APPLE"));
        assertFalse(activity.isValidWord("ORANGE"));
    }

    @Test
    public void testCalculateScore_Win() {
        GameActivity activity = new GameActivity();
        // Формула: (COLS + 1 - attempts) / (float)(COLS + 1)
        // Для COLS=5: (6-3)/6 = 3/6 = 0.5
        float score = activity.calculateScore(true, 3); // win in 3 attempts
        assertEquals(0.5f, score, 0.001f);
    }


    @Test
    public void testCalculateScore_Lose() {
        GameActivity activity = new GameActivity();
        float score = activity.calculateScore(false, 6);
        assertEquals(0f, score, 0.001f);
    }


    @Test
    public void testIsValidWords() {
        GameActivity activity = new GameActivity();
        activity.loadedDictionary = Arrays.asList("apple", "banana", "grape");

        assertTrue(activity.isValidWord("apple"));
        assertTrue(activity.isValidWord("APPLE"));
        assertFalse(activity.isValidWord("orange"));
        assertFalse(activity.isValidWord(""));
    }

    @Test
    public void testIsGameWon() {
        GameActivity activity = new GameActivity();

        // Игра не выиграна
        activity.targetWord = "APPLE";
        assertFalse(activity.isGameWon());

        // Игра выиграна (targetWord сбрасывается при победе)
        activity.targetWord = null;
        activity.currentRow = 3;
        assertTrue(activity.isGameWon());
    }

    @Test
    public void testCheckWord() {
        GameActivity activity = new GameActivity();
        activity.targetWord = "APPLE";

        assertTrue(activity.checkWord("APPLE"));
        assertFalse(activity.checkWord("APRIL"));
        assertFalse(activity.checkWord("")); // пустое слово
        assertFalse(activity.checkWord("APPLES")); // длиннее
        assertFalse(activity.checkWord("APP")); // короче
    }
}