package com.example.sudoku;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Random;

public class SudokuDBHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "sudoku.db";
    private static final int DB_VERSION = 1;

    public SudokuDBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL("CREATE TABLE puzzles(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "difficulty TEXT," +
                "board TEXT)");

        db.execSQL("CREATE TABLE saved_game(" +
                "difficulty TEXT PRIMARY KEY," +
                "initial_board TEXT," +
                "current_board TEXT)");

        db.execSQL("CREATE TABLE scoreboard(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "difficulty TEXT," +
                "player_name TEXT," +
                "time INTEGER)");

        insertPuzzle(db,"easy","530070000600195000098000060800060003400803001700020006060000280000419005000080079");
        insertPuzzle(db,"easy","006100070080005000000008300700010004004203700030000020001900080000040000090020");
        insertPuzzle(db,"easy","010020003040003000007000040200080500005406000000000090000700080000030000001004");

        insertPuzzle(db,"medium","000260701680070090190004500820100040004602900050003028009300074040050036703018000");
        insertPuzzle(db,"medium","100007090030020008009600500005300900010080002600004000300000010040000007007000300");
        insertPuzzle(db,"medium","000900002003000400050001000100400000000000000000007010006700050020000100900008000");

        insertPuzzle(db,"hard","000000907000420180000705026100904000050000040000507009920108000034059000507000000");
        insertPuzzle(db,"hard","000900800128000000070060000050007000000504000000300020000010070000000309006005000");
        insertPuzzle(db,"hard","009000000000002310600001000000007040000000000007000000500700002004100000000000900");
    }

    private void insertPuzzle(SQLiteDatabase db, String d, String b) {
        ContentValues cv = new ContentValues();
        cv.put("difficulty", d);
        cv.put("board", b);
        db.insert("puzzles", null, cv);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS puzzles");
        db.execSQL("DROP TABLE IF EXISTS saved_game");
        db.execSQL("DROP TABLE IF EXISTS scoreboard");
        onCreate(db);
    }

    public int[][] getRandomPuzzle(String difficulty) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT board FROM puzzles WHERE difficulty=? ORDER BY RANDOM() LIMIT 1",
                new String[]{difficulty});

        if (c.moveToFirst()) {
            int[][] board = stringToBoard(c.getString(0));
            c.close();
            return board;
        }
        c.close();
        return null;
    }

    public void saveGame(String difficulty, int[][] initial, int[][] current) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("saved_game", "difficulty=?", new String[]{difficulty});

        ContentValues cv = new ContentValues();
        cv.put("difficulty", difficulty);
        cv.put("initial_board", boardToString(initial));
        cv.put("current_board", boardToString(current));
        db.insert("saved_game", null, cv);
    }

    public boolean hasSavedGame(String difficulty) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT 1 FROM saved_game WHERE difficulty=?",
                new String[]{difficulty});
        boolean exists = c.moveToFirst();
        c.close();
        return exists;
    }

    public int[][][] loadGame(String difficulty) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT initial_board,current_board FROM saved_game WHERE difficulty=?",
                new String[]{difficulty});

        if (c.moveToFirst()) {
            int[][][] data = new int[2][9][9];
            data[0] = stringToBoard(c.getString(0));
            data[1] = stringToBoard(c.getString(1));
            c.close();
            return data;
        }
        c.close();
        return null;
    }

    public void insertScore(String difficulty, int time, String name) {
        ContentValues cv = new ContentValues();
        cv.put("difficulty", difficulty);
        cv.put("player_name", name);
        cv.put("time", time);
        getWritableDatabase().insert("scoreboard", null, cv);
    }

    public Cursor getScores() {
        return getReadableDatabase()
                .rawQuery("SELECT * FROM scoreboard ORDER BY time ASC", null);
    }

    public void resetScoreboard() {
        getWritableDatabase().delete("scoreboard", null, null);
    }

    public String boardToString(int[][] b) {
        StringBuilder sb = new StringBuilder();
        for (int[] r : b)
            for (int n : r)
                sb.append(n);
        return sb.toString();
    }

    public int[][] stringToBoard(String s) {
        int[][] b = new int[9][9];
        for (int i = 0; i < 81; i++)
            b[i / 9][i % 9] = s.charAt(i) - '0';
        return b;
    }
}
