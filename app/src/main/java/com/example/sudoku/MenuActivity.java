package com.example.sudoku;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MenuActivity extends AppCompatActivity {

    private String difficulty = "Easy";
    private SudokuDBHelper db;
    private View btnDifficulty;
    private android.widget.TextView txtDifficultyValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        db = new SudokuDBHelper(this);

        View btnNew = findViewById(R.id.btnNewGame);
        View btnResume = findViewById(R.id.btnResume);
        btnDifficulty = findViewById(R.id.btnDifficulty);
        View btnScoreboard = findViewById(R.id.btnScoreboard);
        Button btnExit = findViewById(R.id.btnExit);
        txtDifficultyValue = findViewById(R.id.txtDifficultyValue);
        txtDifficultyValue.setText(difficulty);

        btnDifficulty.setOnClickListener(v -> showDifficulty());

        btnNew.setOnClickListener(v -> {
            if (db.hasSavedGame(difficulty)) {
                new AlertDialog.Builder(this)
                        .setTitle("Start New Game")
                        .setMessage("Previous saved game will be discarded! Do you wish to continue")
                        .setPositiveButton("OK", (dialog, which) -> startNewGame())
                        .setNegativeButton("Cancel", null)
                        .show();
            } else {
                startNewGame();
            }
        });

        btnResume.setOnClickListener(v -> {
            if (!db.hasSavedGame(difficulty)) {
                Toast.makeText(this, "No saved game for " + difficulty, Toast.LENGTH_SHORT).show();
                return;
            }
            android.database.Cursor cursor = db.getSavedGame(difficulty);
            if (cursor.moveToFirst()) {
                Intent i = new Intent(this, SudokuActivity.class);
                i.putExtra("difficulty", difficulty);
                i.putExtra("initial", cursor.getString(0));
                i.putExtra("current", cursor.getString(1));
                i.putExtra("savedTime", cursor.getInt(2));
                cursor.close();
                startActivity(i);
            } else {
                cursor.close();
                Toast.makeText(this, "Error loading saved game", Toast.LENGTH_SHORT).show();
            }
        });

        btnScoreboard.setOnClickListener(v -> {
            Intent i = new Intent(this, ScoreboardActivity.class);
            i.putExtra("difficulty", difficulty); 
            startActivity(i);
        });

        btnExit.setOnClickListener(v -> finish());
    }

    private void startNewGame() {
        db.deleteSavedGame(difficulty);
        
        Intent i = new Intent(this, SudokuActivity.class);
        i.putExtra("difficulty", difficulty);
        i.putExtra("newGame", true);
        startActivity(i);
    }

    private void showDifficulty() {
        String[] d = {"Easy", "Medium", "Hard"};
        int checkedItem = 0;
        if (difficulty.equals("Medium")) checkedItem = 1;
        else if (difficulty.equals("Hard")) checkedItem = 2;

        new AlertDialog.Builder(this)
                .setTitle("Select Difficulty")
                .setSingleChoiceItems(d, checkedItem, (dialog, which) -> {
                    difficulty = d[which];
                    txtDifficultyValue.setText(difficulty);
                })
                .setPositiveButton("OK", null)
                .show();
    }
}
