package com.example.sudoku;

import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sudoku);

        dbHelper = new SudokuDBHelper(this);

        difficulty = getIntent().getStringExtra("difficulty");
        if (difficulty == null) difficulty = "easy";

        loadNewPuzzle();

        grid = findViewById(R.id.sudokuGrid);
        parentLayout = findViewById(R.id.parentLayout);
        buttonLayout = findViewById(R.id.buttonLayout);

        setupGrid();
        setupButtons();
    }

    private void loadNewPuzzle() {
        initialBoard = dbHelper.getRandomPuzzle(difficulty);
        copyInitialToCurrent();
        isSolved = false;
    }

    private void copyInitialToCurrent() {
        for (int r = 0; r < 9; r++)
            System.arraycopy(initialBoard[r], 0, currentBoard[r], 0, 9);
    }

    private void setupGrid() {
        grid.removeAllViews();
        grid.setColumnCount(9);
        grid.setRowCount(9);

        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;

        boolean isLandscape =
                getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        int totalWidth = getResources().getDisplayMetrics().widthPixels - dpToPx(16); // parent padding
        int totalHeight = getResources().getDisplayMetrics().heightPixels - dpToPx(16);

        int buttonWidth = 0;
        if (isLandscape) buttonWidth = dpToPx(80);

        int boardSize = isLandscape ? Math.min(totalWidth - buttonWidth, totalHeight) : Math.min(totalWidth, totalHeight);

        int cellSize = boardSize / 9;

        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                EditText cell = new EditText(this);
                cell.setGravity(Gravity.CENTER);
                cell.setTextSize(18);
                cell.setInputType(InputType.TYPE_CLASS_NUMBER);
                cell.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1)});

                int value = initialBoard[r][c];

                GradientDrawable bg = new GradientDrawable();
                bg.setColor(value != 0 ? Color.LTGRAY : Color.WHITE);
                cell.setBackground(bg);

                GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
                lp.width = cellSize;
                lp.height = cellSize;
                lp.setMargins((c % 3 == 0) ? 4 : 1, (r % 3 == 0) ? 4 : 1,
                        (c == 8) ? 4 : 1, (r == 8) ? 4 : 1);
                cell.setLayoutParams(lp);

                if (value != 0) {
                    cell.setText(String.valueOf(value));
                    cell.setEnabled(false);
                    cell.setTextColor(Color.BLACK);
                } else {
                    cell.setText("");
                    cell.setTextColor(Color.BLACK);

                    final int row = r;
                    final int col = c;

                    cell.setOnClickListener(v -> {
                        if (selectedCell != null) resetCellBorder(selectedCell);
                        selectedCell = cell;
                        highlightSelectedCell(cell);
                    });

                    cell.addTextChangedListener(new android.text.TextWatcher() {
                        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                            String t = s.toString();
                            currentBoard[row][col] = t.isEmpty() ? 0 : Integer.parseInt(t);
                            highlightRuleBreaks();
                        }
                        @Override public void afterTextChanged(Editable s) {}
                    });
                }

                cells[r][c] = cell;
                grid.addView(cell);
            }
        }
        highlightRuleBreaks();
    }

    private void setupButtons() {
        buttonLayout.removeAllViews();

        Button btnSolve = new Button(this);
        btnSolve.setText("Solve");
        Button btnClear = new Button(this);
        btnClear.setText("Clear");
        Button btnSave = new Button(this);
        btnSave.setText("Save");
        Button btnNew = new Button(this);
        btnNew.setText("New");

        Button[] buttons = {btnSolve, btnClear, btnSave, btnNew};

        for (Button b : buttons) {
            LinearLayout.LayoutParams lp;
            if (buttonLayout.getOrientation() == LinearLayout.HORIZONTAL)
                lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            else
                lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
            lp.setMargins(4,4,4,4);
            buttonLayout.addView(b, lp);
        }

        btnSolve.setOnClickListener(v -> {
            if (isSolved) return;
            if (solveSudoku()) Toast.makeText(this, "Solved!", Toast.LENGTH_SHORT).show();
            else Toast.makeText(this, "Cannot solve puzzle", Toast.LENGTH_SHORT).show();
            isSolved = true;
        });

        btnClear.setOnClickListener(v -> {
            if (!isSolved) clearUserCells();
        });

        btnSave.setOnClickListener(v -> {
            if (!isSolved) {
                dbHelper.saveGame(difficulty, initialBoard, currentBoard);
                Toast.makeText(this, "Game saved!", Toast.LENGTH_SHORT).show();
            }
        });

        btnNew.setOnClickListener(v -> {
            loadNewPuzzle();
            setupGrid();
        });
    }

    private void highlightRuleBreaks() {
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                if (initialBoard[r][c] == 0) {
                    GradientDrawable bg = (GradientDrawable) cells[r][c].getBackground();
                    if (currentBoard[r][c] != 0 && !isCellValid(currentBoard, r, c))
                        bg.setColor(Color.RED);
                    else
                        bg.setColor(Color.WHITE);
                    cells[r][c].setBackground(bg);
                }
    }

    // Fixed for highlighting invalid cells
    private boolean isCellValid(int[][] board, int row, int col) {
        int num = board[row][col];
        if (num == 0) return true;

        for (int i = 0; i < 9; i++) {
            if (i != col && board[row][i] == num) return false;
            if (i != row && board[i][col] == num) return false;
        }

        int startRow = row - row % 3;
        int startCol = col - col % 3;
        for (int r = startRow; r < startRow + 3; r++)
            for (int c = startCol; c < startCol + 3; c++)
                if ((r != row || c != col) && board[r][c] == num) return false;

        return true;
    }

    // Fixed for solver
    private boolean isValid(int[][] board, int row, int col, int num) {
        for (int i = 0; i < 9; i++) {
            if (board[row][i] == num) return false;
            if (board[i][col] == num) return false;
        }

        int startRow = row - row % 3;
        int startCol = col - col % 3;
        for (int r = startRow; r < startRow + 3; r++)
            for (int c = startCol; c < startCol + 3; c++)
                if (r != row || c != col)
                    if (board[r][c] == num) return false;

        return true;
    }

    private void resetCellBorder(EditText cell) {
        GradientDrawable bg = (GradientDrawable) cell.getBackground();
        int row = 0, col = 0;
        outer: for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                if (cells[r][c] == cell) { row = r; col = c; break outer; }
        bg.setColor(initialBoard[row][col] != 0 ? Color.LTGRAY : Color.WHITE);
        cell.setBackground(bg);
    }

    private void highlightSelectedCell(EditText cell) {
        GradientDrawable bg = (GradientDrawable) cell.getBackground();
        bg.setColor(Color.CYAN);
        cell.setBackground(bg);
    }

    private void clearUserCells() {
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                if (initialBoard[r][c] == 0) {
                    cells[r][c].setText("");
                    currentBoard[r][c] = 0;
                    GradientDrawable bg = (GradientDrawable) cells[r][c].getBackground();
                    bg.setColor(Color.WHITE);
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
                bg.setColor(Color.LTGRAY);
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

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
