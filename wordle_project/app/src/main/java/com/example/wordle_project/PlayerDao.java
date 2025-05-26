package com.example.wordle_project;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface PlayerDao {
    @Insert(onConflict=OnConflictStrategy.REPLACE) void insert(Player p);
    @Query("SELECT * FROM players WHERE id = :id") Player findById(int id);
    @Update
    void update(Player p);
    @Query("SELECT * FROM players") List<Player> getAll();
}
