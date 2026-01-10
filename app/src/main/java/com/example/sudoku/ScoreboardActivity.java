package com.example.sudoku;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class ScoreboardActivity extends AppCompatActivity {

    private ListView listViewScores;
    private SudokuDBHelper dbHelper;
    private String difficulty = "easy"; // default, will get from intent

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scoreboard);

        dbHelper = new SudokuDBHelper(this);
        listViewScores = findViewById(R.id.listViewScores);

        String diff = getIntent().getStringExtra("difficulty");
        if (diff != null) difficulty = diff;

        loadScores();

        Button btnReset = findViewById(R.id.btnResetScores);
        btnReset.setOnClickListener(v -> {
            dbHelper.resetScoresForDifficulty(difficulty);
            loadScores();
            Toast.makeText(this, difficulty + " scores reset!", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadScores() {
        Cursor cursor = dbHelper.getScoresForDifficulty(difficulty);
        ArrayList<String> scoreList = new ArrayList<>();

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    String player = cursor.getString(cursor.getColumnIndex("player_name"));
                    int time = cursor.getInt(cursor.getColumnIndex("time"));
                    scoreList.add(player + " - " + time + "s");
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        if (scoreList.isEmpty()) scoreList.add("No scores for " + difficulty);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, scoreList);
        listViewScores.setAdapter(adapter);
    }
}
