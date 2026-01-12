package com.example.sudoku;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Random;

public class SudokuDBHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "sudoku.db";
    private static final int DB_VERSION = 8;

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

        insertPuzzle(db,"Easy","530070000600195000098000060800060003400803001700020006060000280000419005000080079");
        insertPuzzle(db,"Easy","000260701680070090190004500820100040004602900050003028009300074040050036703018000");
        insertPuzzle(db,"Easy","302609005000000300000003010700000006060708020900000001090500000004000000800402107");
        insertPuzzle(db,"Easy","200080300060070084030500209000105408000000000402706000301007040720040060004010003");
        insertPuzzle(db,"Easy","000000907000420180000705026100904000050000040000507009920108000034059000507000000");
        insertPuzzle(db,"Easy","000900800128006400070800060800430007500000009600079008090005010003600284005004000");
        insertPuzzle(db,"Easy","300000000005009000200504000020000700160000058704310600000890100000067080000005437");
        insertPuzzle(db,"Easy","050807020600010090702540006070020301504000908103080070900075604060090002080406050");

        insertPuzzle(db,"Medium","000260701680070090190004500820100040004602900050003028009300074040050036703018000");
        insertPuzzle(db,"Medium","100007090030020008009600500005300900010080002600004000300000010040000007007000300");
        insertPuzzle(db,"Medium","000900002003000400050001000100400000000000000000007010006700050020000100900008000");
        insertPuzzle(db,"Medium","006100720000050004000003060007000080010708050080000600030500000100060000040000100");
        insertPuzzle(db,"Medium","000907000007000800000000000030040020050600040060010030000000000009000100000103000");
        insertPuzzle(db,"Medium","300200000000107000706030500070009080900020004010800050009040301000702000000008006");
        insertPuzzle(db,"Medium","004000700200601000070000020008002030000050000060300800050000040000704001007000600");
        insertPuzzle(db,"Medium","000503000000070200010000050006000040500804007070000800090000030003010000000706000");

        insertPuzzle(db,"Hard","000000907000420180000705026100904000050000040000507009920108000034059000507000000");
        insertPuzzle(db,"Hard","000900800128000000070060000050007000000504000000300020000010070000000309006005000");
        insertPuzzle(db,"Hard","009000000000002310600001000000007040000000000007000000500700002004100000000000900");
        insertPuzzle(db,"Hard","030050040008010500460000012070502080000603000040109030250000098001020600080060000");
        insertPuzzle(db,"Hard","568000402100000638040080050000026394473891205000345000600009100034100000009530040");
        insertPuzzle(db,"Hard","100920000524010000000000070050008102000000000402700090060000000000030945000071006");
        insertPuzzle(db,"Hard","005300000800000020070010500400005300010070006003200080060500009004000030000009700");
        insertPuzzle(db,"Hard","000900002400000600001080000020040000500100007000070030000060900006000004200003000");
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
