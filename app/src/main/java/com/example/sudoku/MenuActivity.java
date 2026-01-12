package com.example.sudoku;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MenuActivity extends AppCompatActivity {

    private String difficulty = "Easy";
    private SudokuDBHelper db;
    private Button btnDifficulty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        db = new SudokuDBHelper(this);

        Button btnNew = findViewById(R.id.btnNewGame);
        Button btnResume = findViewById(R.id.btnResume);
        btnDifficulty = findViewById(R.id.btnDifficulty);
        Button btnScoreboard = findViewById(R.id.btnScoreboard);
        Button btnExit = findViewById(R.id.btnExit);

        updateDifficultyButtonText();

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
            int[][][] boards = db.loadGame(difficulty);
            Intent i = new Intent(this, SudokuActivity.class);
            i.putExtra("difficulty", difficulty);
            i.putExtra("initial", db.boardToString(boards[0]));
            i.putExtra("current", db.boardToString(boards[1]));
            startActivity(i);
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
                    updateDifficultyButtonText();
                })
                .setPositiveButton("OK", null)
                .show();
    }

    private void updateDifficultyButtonText() {
        String cap = difficulty.substring(0, 1).toUpperCase() + difficulty.substring(1);
        btnDifficulty.setText("Difficulty: " + cap);
    }
}
