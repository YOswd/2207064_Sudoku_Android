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

        insertPuzzle(db,"Easy","207300000096702008005009020050020700080060030002030040070200800600803250000005409");
        insertPuzzle(db,"Easy","005083042009500070043000900004002090510809037070400200007000310050004700380690500");
        insertPuzzle(db,"Easy","903008400050107003687302900000600000538200000102004090300906070726851040000403250");
        insertPuzzle(db,"Easy","006871003073056190000349027342000080060020000000003052010704800700598261005000900");
        insertPuzzle(db,"Easy","060080002410060087000074510000001900000820631801049070007090020280053009096218050");
        insertPuzzle(db,"Easy","900006503005000060086300000000039710200504006031820000000002430060000100109400008");
        insertPuzzle(db,"Easy","600801750070549010003602080200306040060150000035084600006410000058000920007900165");
        insertPuzzle(db,"Easy","803100000015402009002005070030040500040090020007050010050800100200507980000004705");

        insertPuzzle(db,"Medium","906000040000970500000004019000050163600000008152030000370400000005098000010000605");
        insertPuzzle(db,"Medium","060500940000094080004102000000009500087405360009700000000307600090240000072008010");
        insertPuzzle(db,"Medium","000300056087005900532700000000000061074000380960000000000003197001200640340001000");
        insertPuzzle(db,"Medium","963804270240905813000307006720680000000200001305040080000400520602000104050030000");
        insertPuzzle(db,"Medium","605189024010000086382405097043051000507000040000700800030000050200078400000506210");
        insertPuzzle(db,"Medium","501072000609000000807540603496007800003004006210090040060000008005063429004028561");
        insertPuzzle(db,"Medium","000003002006710309200840070003000800802030401004000900040069005105087600900100000");
        insertPuzzle(db,"Medium","000000000540070001600450200068520309020000040705094620001089004300010072000000000");

        insertPuzzle(db,"Hard","000000000000060003010009207030084071060000900047000008500800130000640500326050000");
        insertPuzzle(db,"Hard","000400060068307500009000003001083000800602009000750300700000200003206810080001000");
        insertPuzzle(db,"Hard","000003570002007000005000600000060290006000000031800000700900040004001080060000910");
        insertPuzzle(db,"Hard","013900075020005900000700001000000800360010000000007009401060300700580002002473506");
        insertPuzzle(db,"Hard","568000402100000638040080050000026394473891205000345000600009100034100000009530040");
        insertPuzzle(db,"Hard","200405030500090640000000508070020300000914006000600000980006000600080201020000000");
        insertPuzzle(db,"Hard","085906004000008000000750030608000072007803400930000806050047000000600000100305620");
        insertPuzzle(db,"Hard","080000704100000085000030200402910008900007340000000097000706001007040906205800003");
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
