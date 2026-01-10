package com.example.sudoku;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MenuActivity extends AppCompatActivity {

    private Button btnNewGame, btnResume, btnScoreboard, btnDifficulty, btnExit;
    private SudokuDBHelper dbHelper;
    private String difficulty = "easy"; // default

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        dbHelper = new SudokuDBHelper(this);

        btnNewGame = findViewById(R.id.btnNewGame);
        btnResume = findViewById(R.id.btnResume);
        btnScoreboard = findViewById(R.id.btnScoreboard);
        btnDifficulty = findViewById(R.id.btnDifficulty);
        btnExit = findViewById(R.id.btnExit);

        btnDifficulty.setOnClickListener(v -> showDifficultyDialog());

        btnNewGame.setOnClickListener(v -> startNewGame());

        btnResume.setOnClickListener(v -> {
            if (dbHelper.hasSavedGame(difficulty)) {
                int[][][] boards = dbHelper.loadGame(difficulty);
                if (boards != null) {
                    Intent i = new Intent(this, SudokuActivity.class);
                    i.putExtra("difficulty", difficulty);
                    i.putExtra("initialBoard", dbHelper.boardToString(boards[0]));
                    i.putExtra("currentBoard", dbHelper.boardToString(boards[1]));
                    startActivity(i);
                }
            } else {
                Toast.makeText(this, "No saved game for " + difficulty, Toast.LENGTH_SHORT).show();
            }
        });

        btnScoreboard.setOnClickListener(v -> {
            Intent i = new Intent(this, ScoreboardActivity.class);
            i.putExtra("difficulty", difficulty);
            startActivity(i);
        });

        btnExit.setOnClickListener(v -> finish());
    }

    private void showDifficultyDialog() {
        String[] levels = {"Easy", "Medium", "Hard"};
        int currentIndex = difficulty.equals("easy") ? 0 : difficulty.equals("medium") ? 1 : 2;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Difficulty")
                .setSingleChoiceItems(levels, currentIndex, (dialog, which) -> difficulty = levels[which].toLowerCase())
                .setPositiveButton("OK", (dialog, which) -> btnDifficulty.setText("Difficulty: " + capitalize(difficulty)))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void startNewGame() {
        int[][] initialBoard = dbHelper.getRandomPuzzle(difficulty);
        int[][] currentBoard = new int[9][9];
        for (int r = 0; r < 9; r++)
            System.arraycopy(initialBoard[r], 0, currentBoard[r], 0, 9);

        dbHelper.saveGame(difficulty, initialBoard, currentBoard);

        Intent i = new Intent(this, SudokuActivity.class);
        i.putExtra("difficulty", difficulty);
        i.putExtra("initialBoard", dbHelper.boardToString(initialBoard));
        i.putExtra("currentBoard", dbHelper.boardToString(currentBoard));
        startActivity(i);
    }

    private String capitalize(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
