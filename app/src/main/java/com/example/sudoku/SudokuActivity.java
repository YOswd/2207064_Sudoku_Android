package com.example.sudoku;

import android.app.AlertDialog;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
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
    private int selectedRow = -1, selectedCol = -1;
    private LinearLayout keypadLayout;
    private LinearLayout centerLayout;

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
        if (difficulty == null) difficulty = "Easy";

        grid = findViewById(R.id.sudokuGrid);
        parentLayout = findViewById(R.id.parentLayout);
        buttonLayout = findViewById(R.id.buttonLayout);

        timerView = new TextView(this);
        timerView.setTextSize(18);
        timerView.setTextColor(Color.BLACK);
        timerView.setGravity(Gravity.CENTER);
        parentLayout.addView(timerView, 0);

        if (savedInstanceState != null) {
            difficulty = savedInstanceState.getString("difficulty", "Easy");
            initialBoard = dbHelper.stringToBoard(savedInstanceState.getString("initialBoard"));
            currentBoard = dbHelper.stringToBoard(savedInstanceState.getString("currentBoard"));
            isSolved = savedInstanceState.getBoolean("isSolved", false);
            startTime = savedInstanceState.getLong("startTime", SystemClock.elapsedRealtime());
            selectedRow = savedInstanceState.getInt("selectedRow", -1);
            selectedCol = savedInstanceState.getInt("selectedCol", -1);
        } else {
            String initialStr = getIntent().getStringExtra("initial");
            String currentStr = getIntent().getStringExtra("current");
            
            if (initialStr != null && currentStr != null) {
                initialBoard = dbHelper.stringToBoard(initialStr);
                currentBoard = dbHelper.stringToBoard(currentStr);
                isSolved = false;
                startTime = SystemClock.elapsedRealtime();
                updateTimer();
            } else {
                loadNewPuzzle();
            }
        }

        initializeButtonObjects();
        parentLayout.post(this::setupGrid);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setupGrid();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("initialBoard", dbHelper.boardToString(initialBoard));
        outState.putString("currentBoard", dbHelper.boardToString(currentBoard));
        outState.putBoolean("isSolved", isSolved);
        outState.putLong("startTime", startTime);
        outState.putString("difficulty", difficulty);
        outState.putInt("selectedRow", selectedRow);
        outState.putInt("selectedCol", selectedCol);
    }

    private void loadNewPuzzle() {
        Toast.makeText(this, "Loading puzzle from cloud...", Toast.LENGTH_SHORT).show();
        FirestoreHelper firestoreHelper = new FirestoreHelper();
        firestoreHelper.getRandomPuzzle(difficulty, new FirestoreHelper.PuzzleCallback() {
            @Override
            public void onPuzzleLoaded(int[][] puzzle) {
                android.util.Log.d("SudokuActivity", "Puzzle successfully loaded from Firestore");
                initialBoard = puzzle;
                copyInitialToCurrent();
                isSolved = false;
                startTime = SystemClock.elapsedRealtime();
                updateTimer();
                setupGrid();
            }

            @Override
            public void onError(String error) {
                android.util.Log.e("SudokuActivity", "Firestore error: " + error);
                Toast.makeText(SudokuActivity.this, "Cloud error. Loading local puzzle...", Toast.LENGTH_SHORT).show();
                
                int[][] localPuzzle = dbHelper.getRandomLocalPuzzle(difficulty);
                if (localPuzzle != null) {
                    initialBoard = localPuzzle;
                    copyInitialToCurrent();
                    isSolved = false;
                    startTime = SystemClock.elapsedRealtime();
                    updateTimer();
                    setupGrid();
                } else {
                    Toast.makeText(SudokuActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void copyInitialToCurrent() {
        for (int r = 0; r < 9; r++)
            System.arraycopy(initialBoard[r], 0, currentBoard[r], 0, 9);
    }

    private void setupGrid() {
        parentLayout.removeAllViews();

        if (centerLayout == null) {
            centerLayout = new LinearLayout(this);
            centerLayout.setOrientation(LinearLayout.VERTICAL);
            centerLayout.setGravity(Gravity.CENTER);
        }
        centerLayout.removeAllViews();
        
        if (keypadLayout == null) {
            keypadLayout = new LinearLayout(this);
        }
        keypadLayout.removeAllViews();
        
        buttonLayout.removeAllViews();

        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

        int paddingOffset = dpToPx(32);
        int verticalElementsOffset = dpToPx(60);
        
        int availableWidth = screenWidth - paddingOffset - dpToPx(8);
        int availableHeight = screenHeight - paddingOffset - verticalElementsOffset;

        int boardSize;
        if (isLandscape) {
            boardSize = Math.min(availableHeight, availableWidth);
        } else {
            boardSize = Math.min(availableWidth, availableHeight);
        }

        if (isLandscape) {
             int sidePanelsWidth = dpToPx(180);
             if (boardSize > (availableWidth - sidePanelsWidth)) {
                 boardSize = availableWidth - sidePanelsWidth;
             }
        }

        if (boardSize < 0) boardSize = 0;

        if (isLandscape) {
            parentLayout.setOrientation(LinearLayout.HORIZONTAL);
            parentLayout.setGravity(Gravity.CENTER);
            
            setupButtons(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
            btnParams.setMargins(0, 0, dpToPx(16), 0);
            parentLayout.addView(buttonLayout, btnParams);
            
            if (timerView.getParent() != null) ((ViewGroup)timerView.getParent()).removeView(timerView);
            centerLayout.addView(timerView);
            
            if (grid.getParent() != null) ((ViewGroup)grid.getParent()).removeView(grid);
            LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(boardSize, boardSize);
            grid.setLayoutParams(gridParams);
            centerLayout.addView(grid);
            
            parentLayout.addView(centerLayout, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            
            setupKeypad(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams keyParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
            keyParams.setMargins(dpToPx(16), 0, 0, 0);
            parentLayout.addView(keypadLayout, keyParams);

        } else {
            parentLayout.setOrientation(LinearLayout.VERTICAL);
            parentLayout.setGravity(Gravity.CENTER);
            
            if (timerView.getParent() != null) ((ViewGroup)timerView.getParent()).removeView(timerView);
            parentLayout.addView(timerView);
            
            if (grid.getParent() != null) ((ViewGroup)grid.getParent()).removeView(grid);
            LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(boardSize, boardSize);
            grid.setLayoutParams(gridParams);
            parentLayout.addView(grid);
            
            setupKeypad(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams keyParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            keyParams.setMargins(0, dpToPx(16), 0, dpToPx(8));
            parentLayout.addView(keypadLayout, keyParams);
            
            setupButtons(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            parentLayout.addView(buttonLayout, btnParams);
        }

        rebuildGridContent(boardSize);
    }
    
    private void rebuildGridContent(int boardSize) {
        grid.removeAllViews();
        grid.setColumnCount(9);
        grid.setRowCount(9);

        int totalMarginLoss = dpToPx(36);
        int availableCellSpace = boardSize - totalMarginLoss;
        if (availableCellSpace < 0) availableCellSpace = 0;
        int cellSize = availableCellSpace / 9;

        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                EditText cell = new EditText(this);
                cell.setGravity(Gravity.CENTER);
                cell.setTextSize(18);
                cell.setPadding(0,0,0,0);
                cell.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1)});
                cell.setInputType(InputType.TYPE_CLASS_NUMBER);
                
                cell.setShowSoftInputOnFocus(false);
                cell.setFocusable(false);
                cell.setClickable(true);

                int value = initialBoard[r][c];
                if (value != 0) {
                    cell.setText(String.valueOf(value));
                    cell.setEnabled(false);
                } else {
                    cell.setText(currentBoard[r][c] == 0 ? "" : String.valueOf(currentBoard[r][c]));

                    final int row = r;
                    final int col = c;
                    cell.setOnClickListener(v -> {
                        selectedCell = cell;
                        selectedRow = row;
                        selectedCol = col;
                        updateGridColors();
                    });
                }

                GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
                lp.width = cellSize;
                lp.height = cellSize;
                
                int rightMargin = (c + 1) % 3 == 0 && c != 8 ? dpToPx(4) : dpToPx(1);
                int bottomMargin = (r + 1) % 3 == 0 && r != 8 ? dpToPx(4) : dpToPx(1);
                lp.setMargins(dpToPx(1), dpToPx(1), rightMargin, bottomMargin);
                
                grid.addView(cell, lp);
                cells[r][c] = cell;
            }
        }

        if (selectedRow != -1 && selectedCol != -1) {
            selectedCell = cells[selectedRow][selectedCol];
        }

        updateGridColors();
    }
    
    private void setupKeypad(int orientation) {
        keypadLayout.setOrientation(orientation);
        keypadLayout.setGravity(Gravity.CENTER);
        
        for (int i = 1; i <= 9; i++) {
            Button b = new Button(this);
            b.setText(String.valueOf(i));
            final int num = i;
            b.setOnClickListener(v -> onKeypadInput(num));
            
            LinearLayout.LayoutParams lp;
            if (orientation == LinearLayout.HORIZONTAL) {
                lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                lp.setMargins(dpToPx(2), 0, dpToPx(2), 0);
            } else {
                lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 0, 1f);
                lp.setMargins(0, dpToPx(2), 0, dpToPx(2));
                b.setWidth(dpToPx(60));
            }
            keypadLayout.addView(b, lp);
        }
    }
    
    private void onKeypadInput(int num) {
        if (selectedCell != null && selectedRow != -1 && selectedCol != -1) {
            if (initialBoard[selectedRow][selectedCol] == 0) {
                currentBoard[selectedRow][selectedCol] = num;
                selectedCell.setText(String.valueOf(num));
                
                updateGridColors();
                checkGameCompletion();
                updateTimer();
            }
        }
    }

    private void setupButtons(int orientation) {
        buttonLayout.setOrientation(orientation);
        
        Button[] buttons = {btnSolve, btnClear, btnNew, btnSave};
        if (btnSolve == null) { 
             initializeButtonObjects();
             buttons = new Button[]{btnSolve, btnClear, btnNew, btnSave};
        }

        for (Button b : buttons) {
            if (b.getParent() != null) ((ViewGroup)b.getParent()).removeView(b);
            
            LinearLayout.LayoutParams lp;
            if (orientation == LinearLayout.HORIZONTAL) {
                lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            } else {
                lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 0, 1f);
                b.setWidth(dpToPx(80));
            }
            lp.setMargins(4,4,4,4);
            buttonLayout.addView(b, lp);
        }
    }
    
    private void initializeButtonObjects() {
        btnSolve = new Button(this); btnSolve.setText("Solve");
        btnClear = new Button(this); btnClear.setText("Clear");
        btnNew = new Button(this); btnNew.setText("New");
        btnSave = new Button(this); btnSave.setText("Save");
        
        btnSolve.setOnClickListener(v -> {
            if (isSolved) return;
            if (solveSudoku()) {
                Toast.makeText(this, "Puzzle solved! (No Score)", Toast.LENGTH_SHORT).show();
                isSolved = true;
                btnSave.setEnabled(false);
                btnClear.setEnabled(false);
            } else {
                Toast.makeText(this, "Cannot solve puzzle", Toast.LENGTH_SHORT).show();
            }
        });

        btnClear.setOnClickListener(v -> { if (!isSolved) clearUserCells(); });
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

    private void updateGridColors() {
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                EditText cell = cells[r][c];
                int bg = COLOR_NORMAL;
                
                if (initialBoard[r][c] != 0) {
                    bg = COLOR_FIXED;
                }
                
                if (selectedRow != -1 && selectedCol != -1) {
                     boolean isSelected = (r == selectedRow && c == selectedCol);
                     boolean isRelated = (r == selectedRow || c == selectedCol || 
                             ((r/3 == selectedRow/3) && (c/3 == selectedCol/3)));
                             
                     if (isSelected) {
                         bg = COLOR_SELECTED; 
                     } else if (isRelated && initialBoard[r][c] == 0) {
                         bg = COLOR_RELATED; 
                     }
                }

                if (isCellConflicting(r, c)) {
                    bg = Color.RED; 
                }
                
                cell.setBackgroundColor(bg);
                cell.setTextColor(Color.BLACK);
            }
        }
    }
    
    private boolean isCellConflicting(int r, int c) {
        int val = currentBoard[r][c];
        if (val == 0) return false; 
        
        for (int i = 0; i < 9; i++) {
            if (i != c && currentBoard[r][i] == val) return true;
        }
        for (int i = 0; i < 9; i++) {
            if (i != r && currentBoard[i][c] == val) return true;
        }
        int startRow = (r / 3) * 3;
        int startCol = (c / 3) * 3;
        for (int rr = startRow; rr < startRow + 3; rr++) {
            for (int cc = startCol; cc < startCol + 3; cc++) {
                if ((rr != r || cc != c) && currentBoard[rr][cc] == val) return true;
            }
        }
        return false;
    }

    private boolean isCellValid(int[][] board, int row, int col) {
        return !isCellConflicting(row, col); 
    }

    private void clearUserCells() {
        if (selectedCell != null && selectedRow != -1 && selectedCol != -1) {
            if (initialBoard[selectedRow][selectedCol] == 0) {
                 cells[selectedRow][selectedCol].setText("");
                 currentBoard[selectedRow][selectedCol] = 0;
                 updateGridColors();
                 checkGameCompletion();
                 updateTimer();
            } else {
                Toast.makeText(this, "Cannot clear fixed cell", Toast.LENGTH_SHORT).show();
            }
        } else {
             Toast.makeText(this, "Select a cell to clear", Toast.LENGTH_SHORT).show();
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
                cells[r][c].setBackgroundColor(COLOR_FIXED);
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
        loadNewPuzzle();
        btnSave.setEnabled(true);
        btnClear.setEnabled(true);
        selectedCell = null;
        selectedRow = -1;
        selectedCol = -1;
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
            finish();
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
