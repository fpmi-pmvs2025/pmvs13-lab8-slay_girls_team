package com.example.wordle_project;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.GridLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.wordle_project.databinding.ActivityGameBinding;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.CountDownLatch;

public class GameActivity extends AppCompatActivity {
    private ActivityGameBinding binding;
    private TextView[][] cells;
    private int currentRow = 0;
    private int currentCol = 0;
    private static final int ROWS = 6;
    private static final int COLS = 5;
    private String targetWord;
    private Map<Character, Button> keyButtons = new HashMap<>();
    private int currentPlayerId;
    private String currentPlayerName;

    private static final String WORD_LIST_URL =
    "https://gist.githubusercontent.com/scholtes/94f3c0303ba6a7768b47583aff36654d/raw/73f890e1680f3fa21577fef3d1f06b8d6c6ae318/wordle-La.txt";
    private final OkHttpClient httpClient = new OkHttpClient();

    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGameBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());
        promptForPlayer();
    }

    private void promptForPlayer() {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_player, null);
        EditText etId = dialogView.findViewById(R.id.et_player_id);
        EditText etName = dialogView.findViewById(R.id.et_player_name);

        new AlertDialog.Builder(this)
                .setTitle("Player Info")
                .setView(dialogView)
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> {
                    String idText = etId.getText().toString().trim();
                    String nameText = etName.getText().toString().trim();
                    if (idText.isEmpty() || nameText.isEmpty()) {
                        Toast.makeText(this, "Please enter both ID and name.", Toast.LENGTH_SHORT).show();
                        promptForPlayer();
                    } else {
                        currentPlayerId = Integer.parseInt(idText);
                        currentPlayerName = nameText;

                        // Проверяем существование игрока перед созданием
                        new Thread(() -> {
                            AppDatabase db = AppDatabase.getInstance(this);
                            Player existingPlayer = db.playerDao().findById(currentPlayerId);

                            if (existingPlayer == null) {
                                // Создаем нового игрока только если его нет
                                db.playerDao().insert(new Player(currentPlayerId, currentPlayerName));
                            } else {
                                // Обновляем имя если изменилось
                                if (!existingPlayer.name.equals(currentPlayerName)) {
                                    existingPlayer.name = currentPlayerName;
                                    db.playerDao().update(existingPlayer);
                                }
                            }

                            runOnUiThread(() -> startGame());
                        }).start();
                    }
                })
                .show();
    }

    private void startGame() {
        initGrid();
        initKeyboard();
        binding.btnHint.setOnClickListener(v -> {
            if (targetWord != null) Toast.makeText(this, "Hint: " + targetWord, Toast.LENGTH_SHORT).show();
            else Toast.makeText(this, "Word not loaded yet.", Toast.LENGTH_SHORT).show();
        });
        fetchTargetWord();
    }

    private void fetchTargetWord() {
        Request request = new Request.Builder().url(WORD_LIST_URL).build();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("GameActivity", "Failed to fetch word list: " + e.getMessage());
                runOnUiThread(() -> {
                    targetWord = loadRandomFromRaw();
                    Toast.makeText(GameActivity.this, "Network error, using local list", Toast.LENGTH_SHORT).show();
                });
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) { onFailure(call, new IOException("Unexpected code")); return; }
                String[] words = response.body().string().split("\\r?\\n");
                String picked = words[new Random().nextInt(words.length)].toUpperCase();
                runOnUiThread(() -> targetWord = picked);
            }
        });
    }

    private String loadRandomFromRaw() {
        List<String> list = new ArrayList<>();
        try (InputStream is = getResources().openRawResource(R.raw.wordle);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.length() == COLS) list.add(line.toUpperCase());
            }
        } catch (IOException e) { Log.e("GameActivity", "Error reading raw", e); }
        return list.isEmpty() ? "APPLE" : list.get(new Random().nextInt(list.size()));
    }

    private void initGrid() {
        GridLayout grid = binding.gridWordle;
        grid.removeAllViews(); grid.setRowCount(ROWS); grid.setColumnCount(COLS);
        cells = new TextView[ROWS][COLS];
        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < COLS; c++) {
                TextView cell = new TextView(this);
                GridLayout.LayoutParams p = new GridLayout.LayoutParams(
                        GridLayout.spec(r,1f), GridLayout.spec(c,1f));
                p.width = 0; p.height = 0; p.setMargins(4,4,4,4);
                cell.setLayoutParams(p);
                cell.setGravity(Gravity.CENTER);
                cell.setTextColor(Color.BLACK);
                cell.setTextSize(24);
                cell.setBackgroundResource(R.drawable.cell_background);
                grid.addView(cell);
                cells[r][c] = cell;
            }
    }

    private void initKeyboard() {
        addKeys(binding.keyboardRow1, new String[]{"Q","W","E","R","T","Y","U","I","O","P"});
        addKeys(binding.keyboardRow2, new String[]{"A","S","D","F","G","H","J","K","L"});
        addKeys(binding.keyboardRow3, new String[]{"↳","Z","X","C","V","B","N","M","←"});
    }

    private void addKeys(GridLayout row, String[] keys) {
        row.removeAllViews();
        for (String key : keys) {
            Button btn = new Button(this);
            btn.setText(key); btn.setAllCaps(false);
            GridLayout.LayoutParams p = new GridLayout.LayoutParams();
            p.width=0; p.height=ViewGroup.LayoutParams.WRAP_CONTENT;
            p.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);
            p.setMargins(4,4,4,4);
            btn.setLayoutParams(p);
            keyButtons.put(key.charAt(0), btn);
            btn.setOnClickListener(v -> onKey(key));
            row.addView(btn);
        }
    }

    private void onKey(String key) {
        if (currentRow >= ROWS || targetWord == null) return;
        
        if ("←".equals(key)) {
            if (currentCol > 0) {
                currentCol--;
                cells[currentRow][currentCol].setText("");
                cells[currentRow][currentCol].setBackgroundResource(R.drawable.cell_background);
            }
        } else if ("↳".equals(key)) {
            if (currentCol == COLS) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < COLS; i++) {
                    String cellText = cells[currentRow][i].getText().toString();
                    if (cellText.isEmpty()) {
                        Toast.makeText(this, "Please fill all cells", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    sb.append(cellText);
                }
                String guess = sb.toString();
                boolean win = guess.equals(targetWord);
                
                for (int i = 0; i < COLS; i++) {
                    char g = guess.charAt(i);
                    TextView cell = cells[currentRow][i];
                    Button kb = keyButtons.get(g);
                    if (kb != null) {
                        if (g == targetWord.charAt(i)) {
                            cell.setBackgroundColor(Color.parseColor("#6aaa64"));
                            kb.setBackgroundColor(Color.parseColor("#6aaa64"));
                        } else if (targetWord.indexOf(g) >= 0) {
                            cell.setBackgroundColor(Color.parseColor("#c9b458"));
                            kb.setBackgroundColor(Color.parseColor("#c9b458"));
                        } else {
                            cell.setBackgroundColor(Color.parseColor("#787c7e"));
                            kb.setBackgroundColor(Color.parseColor("#787c7e"));
                        }
                    }
                }
                
                if (win) {
                    recordResult(true, currentRow + 1);
                    showWinDialog();
                } else if (currentRow == ROWS - 1) {
                    recordResult(false, ROWS);
                    showFailDialog();
                } else {
                    currentRow++;
                    currentCol = 0;
                }
            } else {
                Toast.makeText(this, "Please fill all cells", Toast.LENGTH_SHORT).show();
            }
        } else if (key.length() == 1) {
            if (currentCol < COLS) {
                cells[currentRow][currentCol].setText(key);
                currentCol++;
            }
        }
    }

    private void recordResult(boolean win, int attempts) {
        try {
            float coef = win ? (COLS + 1 - attempts) / (float)(COLS + 1) : 0f;
            long ts = System.currentTimeMillis();
            GameResult res = new GameResult(currentPlayerId, win, attempts, coef, ts);
            
            new Thread(() -> {
                try {
                    AppDatabase db = AppDatabase.getInstance(this);
                    if (db != null && db.gameResultDao() != null) {
                        db.gameResultDao().insert(res);
                    }
                } catch (Exception e) {
                    Log.e("GameActivity", "Error recording result: " + e.getMessage());
                }
            }).start();
        } catch (Exception e) {
            Log.e("GameActivity", "Error creating game result: " + e.getMessage());
        }
    }

    private void showWinDialog() {
        // Сначала показываем диалог с загрузкой
        AlertDialog loadingDialog = new AlertDialog.Builder(this)
                .setTitle("Congratulations!")
                .setMessage("You won! The word was: " + targetWord + "\n\nLoading statistics...")
                .setCancelable(false)
                .create();
        loadingDialog.show();

        // Загружаем статистику в фоне
        new Thread(() -> {
            String stats = getGameStatsInBackground();
            handler.post(() -> {
                loadingDialog.dismiss();
                new AlertDialog.Builder(this)
                        .setTitle("Congratulations!")
                        .setMessage("You won! The word was: " + targetWord + "\n\n" + stats)
                        .setCancelable(false)
                        .setPositiveButton("New Game", (d,w) -> resetGame())
                        .show();
            });
        }).start();
    }

    private void showFailDialog() {
        // Аналогично для проигрыша
        AlertDialog loadingDialog = new AlertDialog.Builder(this)
                .setTitle("Game Over")
                .setMessage("You failed. The word was: " + targetWord + "\n\nLoading statistics...")
                .setCancelable(false)
                .create();
        loadingDialog.show();

        new Thread(() -> {
            String stats = getGameStatsInBackground();
            handler.post(() -> {
                loadingDialog.dismiss();
                new AlertDialog.Builder(this)
                        .setTitle("Game Over")
                        .setMessage("You failed. The word was: " + targetWord + "\n\n" + stats)
                        .setCancelable(false)
                        .setPositiveButton("New Game", (d,w) -> resetGame())
                        .show();
            });
        }).start();
    }

    private String getGameStatsInBackground() {
        StringBuilder stats = new StringBuilder();
        try {
            AppDatabase db = AppDatabase.getInstance(this);
            if (db != null && db.gameResultDao() != null) {
                List<GameResult> results = db.gameResultDao().getPlayerResults(currentPlayerId);
                if (results != null && !results.isEmpty()) {
                    int totalGames = results.size();
                    int wins = 0;
                    float totalScore = 0;

                    for (GameResult result : results) {
                        if (result.win) {
                            wins++;
                            totalScore += result.coefficient;
                        }
                    }

                    float winRate = (float) wins / totalGames * 100;
                    float avgScore = wins > 0 ? totalScore / wins : 0;

                    stats.append("Game Statistics:\n");
                    stats.append("Total Games: ").append(totalGames).append("\n");
                    stats.append("Wins: ").append(wins).append("\n");
                    stats.append("Win Rate: ").append(String.format("%.1f%%", winRate)).append("\n");
                    stats.append("Average Score: ").append(String.format("%.2f", avgScore));
                }
            }
        } catch (Exception e) {
            Log.e("GameActivity", "Error getting game stats: " + e.getMessage());
            stats.append("Error loading statistics");
        }
        return stats.toString();
    }


    private String getGameStats() {
        final StringBuilder stats = new StringBuilder();
        final CountDownLatch latch = new CountDownLatch(1);

        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(this);
                if (db != null && db.gameResultDao() != null) {
                    List<GameResult> results = db.gameResultDao().getPlayerResults(currentPlayerId);
                    if (results != null && !results.isEmpty()) {
                        int totalGames = results.size();
                        int wins = 0;
                        float totalScore = 0;

                        for (GameResult result : results) {
                            if (result.win) {
                                wins++;
                                totalScore += result.coefficient;
                            }
                        }

                        float winRate = (float) wins / totalGames * 100;
                        float avgScore = wins > 0 ? totalScore / wins : 0;

                        stats.append("Game Statistics:\n");
                        stats.append("Total Games: ").append(totalGames).append("\n");
                        stats.append("Wins: ").append(wins).append("\n");
                        stats.append("Win Rate: ").append(String.format("%.1f%%", winRate)).append("\n");
                        stats.append("Average Score: ").append(String.format("%.2f", avgScore));
                    }
                }
            } catch (Exception e) {
                Log.e("GameActivity", "Error getting game stats: " + e.getMessage());
                stats.append("Error loading statistics");
            } finally {
                latch.countDown();
            }
        }).start();

        try {
            latch.await(); // Ждем завершения запроса
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return stats.toString();
    }

    private void resetGame() {
        currentRow=0; currentCol=0;
        for (int r=0;r<ROWS;r++) for (int c=0;c<COLS;c++) {
            cells[r][c].setText("");
            cells[r][c].setBackgroundResource(R.drawable.cell_background);
        }
        for (Button b:keyButtons.values()) b.setBackgroundResource(android.R.drawable.btn_default);
        keyButtons.clear();
        initKeyboard();
        targetWord = loadRandomFromRaw();
        Log.d("GameActivity","New word: "+targetWord);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}