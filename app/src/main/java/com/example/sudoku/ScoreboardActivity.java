package com.example.sudoku;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class ScoreboardActivity extends AppCompatActivity {

    private ListView listViewScores;
    private SudokuDBHelper dbHelper;
    private String difficulty = "easy"; // default

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
            dbHelper.resetScoreboard();
            loadScores();
            Toast.makeText(this, "Scoreboard reset!", Toast.LENGTH_SHORT).show();
        });

    }

    private void loadScores() {
        Cursor cursor = dbHelper.getScores();
        ArrayList<String> scoreList = new ArrayList<>();

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    String diff = cursor.getString(cursor.getColumnIndex("difficulty"));
                    int time = cursor.getInt(cursor.getColumnIndex("time"));

                    if (diff.equalsIgnoreCase(difficulty)) {
                        scoreList.add(diff + " - " + time + "s");
                    }
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
