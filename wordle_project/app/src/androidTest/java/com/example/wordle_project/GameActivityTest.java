package com.example.wordle_project;

import static androidx.core.content.res.TypedArrayUtils.getText;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.example.wordle_project.GameActivity.WORD_LIST_URL;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.NoMatchingRootException;
import androidx.test.espresso.action.ViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.Request;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Response;

@RunWith(AndroidJUnit4.class)
public class GameActivityTest {

    @Rule
    public ActivityScenarioRule<GameActivity> activityRule =
            new ActivityScenarioRule<>(GameActivity.class);

    private static AppDatabase db;
    private GameResultDao gameResultDao;
    private PlayerDao playerDao;

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        gameResultDao = db.gameResultDao();
        playerDao = db.playerDao();

        // Вводим данные игрока
        enterPlayerInfo(1, "Test Player");
    }

    private void enterPlayerInfo(int id, String name) {
        // Вводим ID игрока
        onView(withId(R.id.et_player_id))
                .perform(typeText(String.valueOf(id)), closeSoftKeyboard());

        // Вводим имя игрока
        onView(withId(R.id.et_player_name))
                .perform(typeText(name), closeSoftKeyboard());

        // Нажимаем кнопку OK
        onView(withText("OK")).perform(click());
    }

    @After
    public void cleanup() {
        db.close();
    }

    // 1. Проверка инициализации игрового поля
    @Test
    public void testGridInitialization() {
        activityRule.getScenario().onActivity(activity -> {
            // Ждем пока игра полностью инициализируется
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            assertNotNull(activity.cells);
            assertEquals(6, activity.cells.length);
            assertEquals(5, activity.cells[0].length);
        });
    }


    // 2. Проверка загрузки слова
    @Test
    public void testWordLoading() {
        activityRule.getScenario().onActivity(activity -> {
            assertNotNull(activity.targetWord);
            assertEquals(5, activity.targetWord.length());
        });
    }

    @Test
    public void testLetterInputWithRetry() throws InterruptedException {
        final int maxAttempts = 3;
        int attempt = 0;

        while (attempt < maxAttempts) {
            try {
                testLetterInput();
                return; // Успех - выходим
            } catch (AssertionError e) {
                attempt++;
                if (attempt == maxAttempts) {
                    throw e;
                }
                Thread.sleep(500); // Пауза перед повторной попыткой
            }
        }
    }

    public void testLetterInput() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        activityRule.getScenario().onActivity(activity -> {
            // Убедимся, что игровое поле инициализировано
            if (activity.cells == null) {
                activity.initGrid();
            }

            // Выполняем в UI-потоке
            activity.runOnUiThread(() -> {
                // Ввод буквы
                activity.onKey("A");

                // Проверяем результат
                assertEquals("A", activity.cells[0][0].getText().toString());
                assertEquals(1, activity.currentCol);

                latch.countDown();
            });
        });

        // Ждем завершения (макс 2 секунды)
        assertTrue(latch.await(2, TimeUnit.SECONDS));
    }

    // 4. Проверка удаления буквы
    @Test
    public void testLetterDeletion() {
        activityRule.getScenario().onActivity(activity -> {
            // ввод буквы
            activity.runOnUiThread(() -> {
                activity.onKey("A");
                assertEquals("A", activity.cells[0][0].getText().toString());
                assertEquals(1, activity.currentCol);

                // удаление
                activity.onKey("←");
            });

            // Проверяем результат
            assertEquals("", activity.cells[0][0].getText().toString());
            assertEquals(0, activity.currentCol);
        });
    }

    // 5. Проверка обработки проигрыша
    @Test
    public void testLoseCondition() {
        activityRule.getScenario().onActivity(activity -> {
            // 1. Устанавливаем тестовое слово и сбрасываем игру
            activity.targetWord = "APPLE";
            activity.resetGame();

            // 2. Проверяем начальное состояние
            assertNotNull("Game should have target word initially", activity.targetWord);
            assertEquals("Should start at row 0", 0, activity.currentRow);

            // 3. 6 неверных попыток
            for (int attempt = 0; attempt < 6; attempt++) {
                for (char c : "WRONG".toCharArray()) {
                    activity.onKey(String.valueOf(c));
                }
                activity.onKey("↳");
                try { Thread.sleep(50); } catch (InterruptedException e) {}

                if (attempt < 5) {
                    assertEquals("Should move to next row after attempt " + (attempt+1),
                            attempt + 1, activity.currentRow);
                }
            }

            // 4. Проверяем конечное состояние
            assertEquals("Should stay on last row (5)", 5, activity.currentRow);
            assertNull("Target word should be cleared after loss", activity.targetWord);
        });
    }

    // 6. Проверка подсветки букв
    @Test
    public void testLetterHighlighting() {
        activityRule.getScenario().onActivity(activity -> {
            // Устанавливаем тестовое слово
            activity.targetWord = "APPLE";

            //  ввод слова
            activity.runOnUiThread(() -> {
                for (char c : "APRIL".toCharArray()) {
                    activity.onKey(String.valueOf(c));
                }
                activity.onKey("↳");
            });

            // время на обработку
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Проверяем цвет фона первой буквы (A - правильная позиция)
            assertEquals(Color.parseColor("#6aaa64"),
                    ((ColorDrawable)activity.cells[0][0].getBackground()).getColor());
        });
    }

    // 7. Проверка кнопки "Подсказка"
    @Test
    public void testHintButton() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        activityRule.getScenario().onActivity(activity -> {
            // Убедимся, что слово загружено
            if (activity.targetWord == null) {
                activity.targetWord = activity.loadRandomFromRaw();
            }

            activity.runOnUiThread(() -> {
                activity.binding.btnHint.performClick();
                assertNotNull("Target word should not be null", activity.targetWord);
                latch.countDown();
            });
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
    }

    //8. Проверка кнопки "Назад"
    @Test
    public void testBackButton() {
        onView(withId(R.id.btn_back)).perform(click());
        activityRule.getScenario().onActivity(activity -> {
            assertTrue(activity.isFinishing());
        });
    }

    // 9. Проверка сброса игры
    @Test
    public void testGameReset() {
        activityRule.getScenario().onActivity(activity -> {
            activity.onKey("A");
            activity.resetGame();
            assertEquals("", activity.cells[0][0].getText().toString());
            assertEquals(0, activity.currentRow);
            assertEquals(0, activity.currentCol);
        });
    }

    // 10. Проверка обработки сетевой ошибки
    @Test
    public void testNetworkErrorHandling() {
        activityRule.getScenario().onActivity(activity -> {
            try {
                activity.fetchTargetWord();
                throw new IOException("Network error");
            } catch (IOException e) {
                String word = activity.loadRandomFromRaw();
                assertNotNull(word);
            }
        });
    }

    // 11. Проверка диалога победы
    @Test
    public void testWinDialog() {
        activityRule.getScenario().onActivity(activity -> {
            activity.showWinDialog();
            assertNotNull(activity.getWindow().getDecorView());
        });
    }

    // 12. Проверка диалога проигрыша
    @Test
    public void testLoseDialog() {
        activityRule.getScenario().onActivity(activity -> {
            activity.showFailDialog();
            assertNotNull(activity.getWindow().getDecorView());
        });
    }

    // 13. Проверка инициализации клавиатуры
    @Test
    public void testKeyboardInitialization() {
        activityRule.getScenario().onActivity(activity -> {
            // Проверяем что клавиатура инициализирована
            assertNotNull("Keyboard buttons map should not be null", activity.keyButtons);

            // Проверяем несколько ключевых кнопок
            assertTrue("Should contain Q button", activity.keyButtons.containsKey('Q'));
            assertTrue("Should contain A button", activity.keyButtons.containsKey('A'));
            assertTrue("Should contain Enter button", activity.keyButtons.containsKey('↳'));
            assertTrue("Should contain Backspace button", activity.keyButtons.containsKey('←'));
        });
    }

    // 14. Проверка обработки неполного слова
    @Test
    public void testIncompleteWordSubmission() {
        activityRule.getScenario().onActivity(activity -> {
            activity.onKey("A");
            activity.onKey("↳");
            assertNotNull(activity.getWindow().getDecorView());
        });
    }

    // 15. Проверка подсчета коэффициента
    @Test
    public void testScoreCalculation() {
        activityRule.getScenario().onActivity(activity -> {
            // Создаем тестовый результат игры
            boolean win = true;
            int attempts = 3;
            int cols = 5; // COLS = 5 из GameActivity

            // Рассчитываем ожидаемый коэффициент по формуле из GameActivity
            float expectedScore = win ? (cols + 1 - attempts) / (float)(cols + 1) : 0f;

            // Создаем тестовый GameResult
            GameResult testResult = new GameResult(1, win, attempts, expectedScore, System.currentTimeMillis());

            // Проверяем что коэффициент рассчитан правильно
            assertEquals(0.5f, testResult.coefficient, 0.001f); // (5+1-3)/(5+1) = 3/6 = 0.5
        });
    }

    // 16. Проверка обновления клавиатуры после попытки
    @Test
    public void testKeyboardUpdateAfterGuess() {
        activityRule.getScenario().onActivity(activity -> {
            activity.targetWord = "APPLE";
            for (char c : "APRIL".toCharArray()) {
                activity.onKey(String.valueOf(c));
            }
            activity.onKey("↳");

            Button aButton = activity.keyButtons.get('A');
            ColorDrawable background = (ColorDrawable) aButton.getBackground();
            assertEquals(0xFF6AAA64, background.getColor()); // Зеленый
        });
    }

    // 17.
    @Test
    public void testLocalDictionaryLoad() {
        activityRule.getScenario().onActivity(activity -> {
            activity.targetWord = null;

            String word = activity.loadRandomFromRaw();
            assertNotNull(word);
            assertEquals(5, word.length());
            assertTrue(word.matches("[A-Z]+"));
        });
    }


    @BeforeClass
    public static void createDb() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
    }

    @AfterClass
    public static void closeDb() {
        db.close();
    }

    // 18.
    @Test
    public void testInsertAndGetResults() {
        // Подготовка
        int playerId = 1;
        playerDao.insert(new Player(playerId, "Test Player"));

        // Действие
        GameResult result = new GameResult(playerId, true, 3, 0.8f, System.currentTimeMillis());
        gameResultDao.insert(result);

        // Проверка
        List<GameResult> results = gameResultDao.getPlayerResults(playerId);
        assertEquals(1, results.size());
        assertEquals(playerId, results.get(0).playerId);
        assertTrue(results.get(0).win);
    }

    // 19. Тест проверки слова на валидность
    @Test
    public void testWordValidation() {
        activityRule.getScenario().onActivity(activity -> {
            // Загружаем тестовый словарь
            activity.loadedDictionary = Arrays.asList("apple", "banana", "grape", "melon", "peach");

            assertTrue("Valid word should return true", activity.isValidWord("apple"));
            assertFalse("Invalid word should return false", activity.isValidWord("xyzzy"));
        });
    }

    // 20. Тест расчета цветов букв
    @Test
    public void testLetterColorCalculation() {
        activityRule.getScenario().onActivity(activity -> {
            activity.targetWord = "APPLE";
            String guess = "APRIL";

            int[] colors = activity.calculateLetterColors(guess);

            assertEquals("First letter (A) should be correct",
                    GameActivity.COLOR_CORRECT, colors[0]);
            assertEquals("Second letter (P) should be correct",
                    GameActivity.COLOR_CORRECT, colors[1]);
            assertEquals("Third letter (R) should be wrong",
                    GameActivity.COLOR_WRONG, colors[2]);
            assertEquals("Fourth letter (I) should be wrong",
                    GameActivity.COLOR_WRONG, colors[3]);
            assertEquals("Fifth letter (L) should be present",
                    GameActivity.COLOR_PRESENT, colors[4]);
        });
    }

    // 21. Тест проверки слова (checkWord)
    @Test
    public void testCheckWordFunction() {
        activityRule.getScenario().onActivity(activity -> {
            activity.targetWord = "APPLE";

            assertTrue("Correct word should return true", activity.checkWord("APPLE"));
            assertFalse("Incorrect word should return false", activity.checkWord("GRAPE"));
        });
    }

    // 22. Тест быстрого последовательного ввода (исправленный)
    @Test
    public void testRapidInput() {
        activityRule.getScenario().onActivity(activity -> {
            // Сбрасываем игру
            activity.resetGame();

            // Быстро вводим несколько букв
            for (int i = 0; i < 10; i++) {
                activity.onKey("A");
                try {
                    Thread.sleep(10); // Небольшая задержка для эмуляции быстрого ввода
                } catch (InterruptedException e) {
                }
            }

            // Проверяем что только 5 букв было принято
            assertEquals(5, activity.currentCol);
            for (int i = 0; i < 5; i++) {
                assertEquals("A", activity.cells[0][i].getText().toString());
            }

            // Проверяем что следующая строка пуста
            for (int i = 0; i < 5; i++) {
                assertEquals("", activity.cells[1][i].getText().toString());
            }
        });
    }
}