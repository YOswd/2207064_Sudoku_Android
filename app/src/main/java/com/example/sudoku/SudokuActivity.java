package com.example.sudoku;

import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
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
    private static final int COLOR_SELECTED = Color.parseColor("#B3E5FC"); // light blue
    private static final int COLOR_RELATED  = Color.parseColor("#E1F5FE"); // lighter blue
    private static final int COLOR_NORMAL   = Color.WHITE;
    private static final int COLOR_FIXED    = Color.LTGRAY;
    private static final int COLOR_ERROR    = Color.parseColor("#FFCDD2"); // light red

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

        loadNewPuzzle();
        parentLayout.post(() -> { // ensure layout measured before sizing
            setupGrid();
            setupButtons();
        });
    }

    /** Load a random puzzle from DB **/
    private void loadNewPuzzle() {
        initialBoard = dbHelper.getRandomPuzzle(difficulty);
        copyInitialToCurrent();
        isSolved = false;
    }

    private void copyInitialToCurrent() {
        for (int r = 0; r < 9; r++)
            System.arraycopy(initialBoard[r], 0, currentBoard[r], 0, 9);
    }

    /** Setup the 9x9 grid **/
    private void setupGrid() {
        grid.removeAllViews();
        grid.setColumnCount(9);
        grid.setRowCount(9);

        int parentWidth = parentLayout.getWidth() - dpToPx(16);
        int parentHeight = parentLayout.getHeight() - dpToPx(16);

        boolean isLandscape =
                getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        int buttonSpace = isLandscape ? buttonLayout.getWidth() : 0;
        int boardSize = Math.min(parentWidth - buttonSpace, parentHeight);
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
                bg.setColor(value != 0 ? COLOR_FIXED : COLOR_NORMAL);
                bg.setStroke(1, Color.BLACK);
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
                    final int row = r, col = c;

                    cell.setOnClickListener(v -> {
                        selectedCell = cell;
                        highlightSelection(row, col);
                    });

                    cell.addTextChangedListener(new android.text.TextWatcher() {
                        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                            currentBoard[row][col] = s.toString().isEmpty() ? 0 : Integer.parseInt(s.toString());
                            highlightRuleBreaks();
                            checkCompletion();
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

    /** Setup bottom/right buttons **/
    private void setupButtons() {
        buttonLayout.removeAllViews();

        Button btnSolve = new Button(this); btnSolve.setText("Solve");
        Button btnClear = new Button(this); btnClear.setText("Clear");
        Button btnNew = new Button(this); btnNew.setText("New");
        Button btnSave = new Button(this); btnSave.setText("Save");

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
            if (solveSudoku()) Toast.makeText(this, "Solved!", Toast.LENGTH_SHORT).show();
            else Toast.makeText(this, "Cannot solve puzzle", Toast.LENGTH_SHORT).show();
            isSolved = true;
        });

        btnClear.setOnClickListener(v -> { if (!isSolved) clearUserCells(); });

        btnSave.setOnClickListener(v -> {
            dbHelper.saveGame(difficulty, initialBoard, currentBoard);
            Toast.makeText(this, "Game saved!", Toast.LENGTH_SHORT).show();
        });

        btnNew.setOnClickListener(v -> { loadNewPuzzle(); setupGrid(); });
    }

    /** Highlight duplicates and invalid numbers **/
    private void highlightRuleBreaks() {
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                if (initialBoard[r][c] == 0) {
                    GradientDrawable bg = (GradientDrawable) cells[r][c].getBackground();
                    if (currentBoard[r][c] != 0 && !isCellValid(currentBoard, r, c))
                        bg.setColor(COLOR_ERROR);
                    else
                        bg.setColor(COLOR_NORMAL);
                    cells[r][c].setBackground(bg);
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

    /** Highlight selection row, column, and box **/
    private void highlightSelection(int selRow, int selCol) {
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                GradientDrawable bg = (GradientDrawable) cells[r][c].getBackground();
                bg.setColor(initialBoard[r][c] != 0 ? COLOR_FIXED : COLOR_NORMAL);
                cells[r][c].setBackground(bg);
            }
        }

        for (int i = 0; i < 9; i++) {
            if (initialBoard[selRow][i] == 0)
                ((GradientDrawable) cells[selRow][i].getBackground()).setColor(COLOR_RELATED);
            if (initialBoard[i][selCol] == 0)
                ((GradientDrawable) cells[i][selCol].getBackground()).setColor(COLOR_RELATED);
        }

        int boxRow = (selRow / 3) * 3;
        int boxCol = (selCol / 3) * 3;
        for (int r = boxRow; r < boxRow + 3; r++)
            for (int c = boxCol; c < boxCol + 3; c++)
                if (initialBoard[r][c] == 0)
                    ((GradientDrawable) cells[r][c].getBackground()).setColor(COLOR_RELATED);

        ((GradientDrawable) cells[selRow][selCol].getBackground()).setColor(COLOR_SELECTED);
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

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    /** Check if puzzle is complete **/
    private void checkCompletion() {
        for (int r = 0; r < 9; r++)
            for (int c = 0; c < 9; c++)
                if (currentBoard[r][c] == 0 || !isCellValid(currentBoard, r, c))
                    return;

        isSolved = true;
        showScoreDialog();
    }

    /** Prompt user to enter name and save score **/
    private void showScoreDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Congratulations! You solved the puzzle");

        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Enter your name");
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String name = input.getText().toString().trim();
            int time = 0; // can be replaced with timer
            dbHelper.insertScore(difficulty, time, name);
            Toast.makeText(this, "Score saved!", Toast.LENGTH_SHORT).show();
            finish(); // return to menu
        });
        builder.setCancelable(false);
        builder.show();
    }
}
