package com.example.wordle_project;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

/**
 * Адаптер для отображения списка рейтинга
 */
public class RatingAdapter extends RecyclerView.Adapter<RatingAdapter.VH> {
    private List<RatingItem> items = new ArrayList<>();

    /** Обновить набор элементов и перерисовать список */
    public void setItems(List<RatingItem> newItems) {
        items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Инфлейтим макет элемента rating_item.xml
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_rating, parent, false);
        return new VH(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        RatingItem it = items.get(position);
        // Номер в рейтинге (позиция +1)
        holder.rank.setText(String.valueOf(position + 1));
        holder.name.setText(it.getName());
        // Формируем строку "wins/games (avgCoef)"
        String scoreText = String.format("%d/%d (%.2f)",
                it.getWins(),
                it.getGames(),
                it.getAvgCoef());
        holder.score.setText(scoreText);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * ViewHolder для элемента рейтинга
     */
    static class VH extends RecyclerView.ViewHolder {
        TextView rank, name, score;
        VH(View v) {
            super(v);
            rank = v.findViewById(R.id.tv_rank);
            name = v.findViewById(R.id.tv_name);
            score = v.findViewById(R.id.tv_score);
        }
    }
}