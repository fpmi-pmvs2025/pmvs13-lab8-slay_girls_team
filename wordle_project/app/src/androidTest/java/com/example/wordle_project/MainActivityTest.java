// app/src/androidTest/java/com/example/wordle_project/MainActivityTest.java
package com.example.wordle_project;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import org.junit.Rule;
import org.junit.Test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MainActivityTest {
    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    // проверка кнопки старт
    @Test
    public void testStartButton() {
        Espresso.onView(withId(R.id.btn_start)).perform(click());
    }

    // проверка загрузки рейтинга игроков
    @Test
    public void testRatingButtonLaunchesRating() {
        ActivityScenario.launch(MainActivity.class);

        Espresso.onView(ViewMatchers.withId(R.id.btn_rating))
                .perform(ViewActions.click());

        Espresso.onView(ViewMatchers.withId(R.id.rv_rating))
                .check(matches(isDisplayed()));
    }

    // создание результата
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
}