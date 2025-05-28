package com.example.wordle_project;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class RatingActivity extends AppCompatActivity {
    private RatingAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rating);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.rv_rating);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RatingAdapter();
        rv.setAdapter(adapter);

        loadRatingData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Обновляем данные при возврате в активити
        loadRatingData();
    }

    private void loadRatingData() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            // Берём уже агрегированные данные из БД
            List<RatingStats> stats = db.gameResultDao().getRatingStats();

            // Преобразуем их в RatingItem для адаптера
            List<RatingItem> items = new ArrayList<>();
            for (RatingStats s : stats) {
                items.add(new RatingItem(
                        s.name,
                        s.wins,
                        s.games,
                        s.avgCoef
                ));
            }

            runOnUiThread(() -> adapter.setItems(items));
        }).start();
    }
}