package com.example.sudoku;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SudokuActivity extends AppCompatActivity {

    private EditText[][] cells = new EditText[9][9];
    private EditText selectedCell = null;
    private String difficulty;
    private int[][] initialBoard = new int[9][9];
    private int[][] currentBoard = new int[9][9];
    private SudokuDBHelper dbHelper;
    private boolean isSolved = false;

    private GridLayout grid;
    private LinearLayout parentLayout;
    private LinearLayout buttonLayout;
    private TextView timerView;

    private Button btnSolve, btnClear, btnNew, btnSave;

    private static final int COLOR_SELECTED = Color.parseColor("#B3E5FC");
    private static final int COLOR_RELATED  = Color.parseColor("#E1F5FE");
    private static final int COLOR_NORMAL   = Color.WHITE;
    private static final int COLOR_FIXED    = Color.LTGRAY;

    private long startTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sudoku);

        dbHelper = new SudokuDBHelper(this);

        difficulty = getIntent().getStringExtra("difficulty");
        if (difficulty == null) difficulty = "easy";

        grid = findViewById(R.id.sudokuGrid);
        parentLayout = findViewById(R.id.parentLayout);
        buttonLayout = findViewById(R.id.buttonLayout);

        timerView = new TextView(this);
        timerView.setTextSize(18);
        timerView.setTextColor(Color.BLACK);
        timerView.setGravity(Gravity.CENTER);
        parentLayout.addView(timerView, 0);
        startTime = SystemClock.elapsedRealtime();

        loadNewPuzzle();
        setupButtons();
        parentLayout.post(() -> setupGrid());

    }

    private void loadNewPuzzle() {
        int[][] puzzle = dbHelper.getRandomPuzzle(difficulty);
        if (puzzle != null) {
            initialBoard = puzzle;
            copyInitialToCurrent();
            isSolved = false;
            startTime = SystemClock.elapsedRealtime();
            updateTimer();
        } else {
            Toast.makeText(this, "No puzzle found for " + difficulty, Toast.LENGTH_SHORT).show();
        }
    }

    private void copyInitialToCurrent() {
        for (int r = 0; r < 9; r++)
            System.arraycopy(initialBoard[r], 0, currentBoard[r], 0, 9);
    }

    private void setupGrid() {
        grid.removeAllViews();
        grid.setColumnCount(9);
        grid.setRowCount(9);

        int screenWidth = getResources().getDisplayMetrics().widthPixels - dpToPx(16);
        int screenHeight = getResources().getDisplayMetrics().heightPixels - dpToPx(16);

        boolean isLandscape =
                getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        int boardSize;
        if (isLandscape) {
            int buttonWidth = dpToPx(120);
            boardSize = Math.min(screenHeight, screenWidth - buttonWidth - dpToPx(16));
        } else {
            boardSize = Math.min(screenWidth, screenHeight);
        }

        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(boardSize, boardSize);
        grid.setLayoutParams(gridParams);

        int cellSize = boardSize / 9;

        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                CellView cell = new CellView(this, r, c);

                int value = initialBoard[r][c];
                if (value != 0) {
                    cell.setText(String.valueOf(value));
                    cell.setEnabled(false);
                    cell.setBackgroundColor(COLOR_FIXED);
                    cell.setTextColor(0xFF000000);
                } else {
                    cell.setText("");
                    cell.setBackgroundColor(COLOR_NORMAL);
                    cell.setTextColor(0xFF000000);

                    final int row = r;
                    final int col = c;
                    cell.setOnClickListener(v -> {
                        selectedCell = cell;
                        highlightSelection(row, col);
                    });

                    cell.addTextChangedListener(new android.text.TextWatcher() {
                        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                            String t = s.toString();
                            currentBoard[row][col] = t.isEmpty() ? 0 : Integer.parseInt(t);
                            highlightRuleBreaks();
                            checkGameCompletion();
                            updateTimer();
                        }
                        @Override public void afterTextChanged(android.text.Editable s) {}
                    });
                }

                GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
                lp.width = cellSize;
                lp.height = cellSize;
                grid.addView(cell, lp);
                cells[r][c] = cell;
            }
        }

        highlightRuleBreaks();
    }

    private void setupButtons() {
        buttonLayout.removeAllViews();

        btnSolve = new Button(this); btnSolve.setText("Solve");
        btnClear = new Button(this); btnClear.setText("Clear");
        btnNew = new Button(this); btnNew.setText("New");
        btnSave = new Button(this); btnSave.setText("Save");

        Button[] buttons = {btnSolve, btnClear, btnNew, btnSave};

        for (Button b : buttons) {
            LinearLayout.LayoutParams lp;
            if (buttonLayout.getOrientation() == LinearLayout.HORIZONTAL)
                lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            else
                lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 0, 1f);
            lp.setMargins(4,4,4,4);
            buttonLayout.addView(b, lp);
        }

        btnSolve.setOnClickListener(v -> {
            if (isSolved) return;

            if (solveSudoku()) {
                Toast.makeText(this, "Puzzle solved!", Toast.LENGTH_SHORT).show();
                isSolved = true;

                btnSave.setEnabled(false);
                btnClear.setEnabled(false);

                showScoreDialog();
            } else {
                Toast.makeText(this, "Cannot solve puzzle", Toast.LENGTH_SHORT).show();
            }
        });

        btnClear.setOnClickListener(v -> {
            if (!isSolved) clearUserCells();
        });

        btnNew.setOnClickListener(v -> startNewGame());

        btnSave.setOnClickListener(v -> {
            if (!isSolved) {
                dbHelper.saveGame(difficulty, initialBoard, currentBoard);
                Toast.makeText(this, "Game saved!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Cannot save solved game", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void highlightRuleBreaks() {
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                EditText cell = cells[r][c];

                if (initialBoard[r][c] != 0) {
                    cell.setBackgroundColor(COLOR_FIXED);
                    cell.setTextColor(0xFF000000);
                } else {
                    if (currentBoard[r][c] != 0 && !isCellValid(currentBoard, r, c)) {
                        cell.setBackgroundColor(Color.RED);
                    } else {
                        cell.setBackgroundColor(COLOR_NORMAL);
                    }
                    cell.setTextColor(0xFF000000);
                }
            }
        }

        if (selectedCell != null) {
            for (int r = 0; r < 9; r++) {
                for (int c = 0; c < 9; c++) {
                    if (cells[r][c] == selectedCell) {
                        cells[r][c].setBackgroundColor(COLOR_SELECTED);
                    }
                }
            }
        }
    }


    private boolean isCellValid(int[][] board, int row, int col) {
        int num = board[row][col];
        if (num == 0) return true;

        for (int c = 0; c < 9; c++)
            if (c != col && board[row][c] == num) return false;

        for (int r = 0; r < 9; r++)
            if (r != row && board[r][col] == num) return false;

        int startRow = row - row % 3;
        int startCol = col - col % 3;
        for (int r = startRow; r < startRow + 3; r++)
            for (int c = startCol; c < startCol + 3; c++)
                if ((r != row || c != col) && board[r][c] == num) return false;

        return true;
    }

    private void highlightSelection(int selRow, int selCol) {
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                EditText cell = cells[r][c];
                if (initialBoard[r][c] != 0) cell.setBackgroundColor(COLOR_FIXED);
                else cell.setBackgroundColor(COLOR_NORMAL);
                cell.setTextColor(0xFF000000); // numbers always black
            }
        }

        for (int i = 0; i < 9; i++) {
            if (initialBoard[selRow][i] == 0)
                cells[selRow][i].setBackgroundColor(COLOR_RELATED);
            if (initialBoard[i][selCol] == 0)
                cells[i][selCol].setBackgroundColor(COLOR_RELATED);
        }

        int boxRow = (selRow / 3) * 3;
        int boxCol = (selCol / 3) * 3;
        for (int r = boxRow; r < boxRow + 3; r++)
            for (int c = boxCol; c < boxCol + 3; c++)
                if (initialBoard[r][c] == 0)
                    cells[r][c].setBackgroundColor(COLOR_RELATED);

        cells[selRow][selCol].setBackgroundColor(COLOR_SELECTED);
    }

    private void clearUserCells() {
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                if (initialBoard[r][c] == 0) {
                    cells[r][c].setText("");
                    currentBoard[r][c] = 0;
                    GradientDrawable bg = (GradientDrawable) cells[r][c].getBackground();
                    bg.setColor(COLOR_NORMAL);
                    cells[r][c].setBackground(bg);
                }
    }

    private boolean solveSudoku() {
        int[][] board = new int[9][9];
        for (int r = 0; r < 9; r++)
            System.arraycopy(currentBoard[r], 0, board[r], 0, 9);

        if (!solve(board)) return false;

        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++) {
                cells[r][c].setText(String.valueOf(board[r][c]));
                cells[r][c].setEnabled(false);
                GradientDrawable bg = (GradientDrawable) cells[r][c].getBackground();
                bg.setColor(COLOR_FIXED);
                cells[r][c].setBackground(bg);
            }

        currentBoard = board;
        return true;
    }

    private boolean solve(int[][] board) {
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++) {
                if (board[r][c] == 0) {
                    for (int n = 1; n <= 9; n++) {
                        if (isValid(board, r, c, n)) {
                            board[r][c] = n;
                            if (solve(board)) return true;
                            board[r][c] = 0;
                        }
                    }
                    return false;
                }
            }
        return true;
    }

    private boolean isValid(int[][] board, int row, int col, int num) {
        for (int i = 0; i < 9; i++)
            if (board[row][i] == num || board[i][col] == num) return false;

        int startRow = row - row % 3;
        int startCol = col - col % 3;
        for (int r = startRow; r < startRow + 3; r++)
            for (int c = startCol; c < startCol + 3; c++)
                if (board[r][c] == num) return false;

        return true;
    }

    private void startNewGame() {
        int[][] puzzle = dbHelper.getRandomPuzzle(difficulty);
        if (puzzle == null) {
            Toast.makeText(this, "No puzzle found for " + difficulty, Toast.LENGTH_SHORT).show();
            return;
        }

        initialBoard = puzzle;
        copyInitialToCurrent();
        isSolved = false;

        btnSave.setEnabled(true);
        btnClear.setEnabled(true);

        startTime = SystemClock.elapsedRealtime();
        updateTimer();

        setupGrid();
    }

    private void checkGameCompletion() {
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                if (currentBoard[r][c] == 0 || !isCellValid(currentBoard, r, c))
                    return;

        isSolved = true;
        btnSave.setEnabled(false);
        btnClear.setEnabled(false);
        showScoreDialog();
    }

    private void showScoreDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Congratulations! You completed the puzzle");

        final EditText input = new EditText(this);
        input.setHint("Enter your name");
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String name = input.getText().toString().trim();
            int time = (int)((SystemClock.elapsedRealtime() - startTime) / 1000);
            dbHelper.insertScore(difficulty, time, name);
            Toast.makeText(this, "Score saved!", Toast.LENGTH_SHORT).show();
        });

        builder.setCancelable(false);
        builder.show();
    }

    private void updateTimer() {
        int seconds = (int)((SystemClock.elapsedRealtime() - startTime) / 1000);
        timerView.setText("Time: " + seconds + "s");
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
