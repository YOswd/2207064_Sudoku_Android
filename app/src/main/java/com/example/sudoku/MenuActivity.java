package com.example.sudoku;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MenuActivity extends AppCompatActivity {

    private String difficulty = "easy";
    private SudokuDBHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        db = new SudokuDBHelper(this);

        Button btnNew = findViewById(R.id.btnNewGame);
        Button btnResume = findViewById(R.id.btnResume);
        Button btnDifficulty = findViewById(R.id.btnDifficulty);
        Button btnScoreboard = findViewById(R.id.btnScoreboard);
        Button btnExit = findViewById(R.id.btnExit);

        btnDifficulty.setOnClickListener(v -> showDifficulty());

        btnNew.setOnClickListener(v -> {
            Intent i = new Intent(this, SudokuActivity.class);
            i.putExtra("difficulty", difficulty);
            i.putExtra("newGame", true);
            startActivity(i);
        });

        btnResume.setOnClickListener(v -> {
            if (!db.hasSavedGame(difficulty)) {
                Toast.makeText(this, "No saved game", Toast.LENGTH_SHORT).show();
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
            i.putExtra("difficulty", difficulty); // pass current difficulty
            startActivity(i);
        });

        btnExit.setOnClickListener(v -> finish());
    }

    private void showDifficulty() {
        String[] d = {"Easy", "Medium", "Hard"};
        new AlertDialog.Builder(this)
                .setTitle("Difficulty")
                .setSingleChoiceItems(d,
                        difficulty.equals("easy") ? 0 :
                                difficulty.equals("medium") ? 1 : 2,
                        (dialog, which) -> difficulty = d[which].toLowerCase())
                .setPositiveButton("OK", null)
                .show();
    }
}
