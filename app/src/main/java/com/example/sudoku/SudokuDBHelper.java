package com.example.sudoku;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class SudokuDBHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "sudoku.db";
    private static final int DB_VERSION = 9;
    private Context context;

    public SudokuDBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.context = context;
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

        loadPuzzlesFromJson(db);
    }

    private void loadPuzzlesFromJson(SQLiteDatabase db) {
        try {
            InputStream is = context.getAssets().open("puzzles.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);

            JSONArray jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                insertPuzzle(db, obj.getString("difficulty"), obj.getString("board"));
            }
            Log.d("SudokuDBHelper", "Successfully loaded " + jsonArray.length() + " puzzles from JSON");
        } catch (Exception e) {
            Log.e("SudokuDBHelper", "Error loading puzzles from JSON", e);
        }
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

    public void saveGame(String difficulty, int[][] initial, int[][] current) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("saved_game", "difficulty=?", new String[]{difficulty});

        ContentValues cv = new ContentValues();
        cv.put("difficulty", difficulty);
        cv.put("initial_board", boardToString(initial));
        cv.put("current_board", boardToString(current));
        db.insert("saved_game", null, cv);
    }

    public void deleteSavedGame(String difficulty) {
        getWritableDatabase().delete("saved_game", "difficulty=?", new String[]{difficulty});
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

    public Cursor getScoresForDifficulty(String difficulty) {
        return getReadableDatabase().rawQuery(
                "SELECT * FROM scoreboard WHERE difficulty=? ORDER BY time ASC",
                new String[]{difficulty});
    }

    public void resetScoresForDifficulty(String difficulty) {
        getWritableDatabase().delete("scoreboard", "difficulty=?", new String[]{difficulty});
    }


    public int[][] getRandomLocalPuzzle(String difficulty) {
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
