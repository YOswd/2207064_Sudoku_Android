package com.example.sudoku;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FirestoreHelper {
    
    private FirebaseFirestore db;
    
    public FirestoreHelper() {
        db = FirebaseFirestore.getInstance();
    }
    
    public interface PuzzleCallback {
        void onPuzzleLoaded(int[][] puzzle);
        void onError(String error);
    }
    
    public void getRandomPuzzle(String difficulty, PuzzleCallback callback) {
        android.util.Log.d("FirestoreHelper", "Fetching puzzle for difficulty: " + difficulty);
        
        db.collection("puzzles")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                android.util.Log.d("FirestoreHelper", "Total puzzles in cloud: " + queryDocumentSnapshots.size());
                
                List<String> puzzles = new ArrayList<>();
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    android.util.Log.d("FirestoreHelper", "Checking doc: " + document.getId() + " | Data: " + document.getData());
                    
                    String docDifficulty = document.getString("difficulty");
                    if (docDifficulty == null) docDifficulty = document.getString("Difficulty");

                    if (docDifficulty != null && docDifficulty.equalsIgnoreCase(difficulty)) {
                        String board = document.getString("board");
                        if (board == null) board = document.getString("Board");
                        
                        if (board != null && board.length() == 81) {
                            puzzles.add(board);
                        }
                    }
                }
                
                if (puzzles.isEmpty()) {
                    android.util.Log.w("FirestoreHelper", "No matching puzzles found for: " + difficulty);
                    callback.onError("No puzzles found in cloud for " + difficulty + ". Please check your 'difficulty' and 'board' field names in Firebase console.");
                    return;
                }

                Random random = new Random();
                String selectedPuzzle = puzzles.get(random.nextInt(puzzles.size()));
                int[][] board = stringToBoard(selectedPuzzle);
                callback.onPuzzleLoaded(board);
            })
            .addOnFailureListener(e -> {
                android.util.Log.e("FirestoreHelper", "Critical Firestore error", e);
                callback.onError("Firestore Error: " + e.getMessage());
            });
    }
    
    private int[][] stringToBoard(String s) {
        int[][] b = new int[9][9];
        for (int i = 0; i < 81; i++) {
            b[i / 9][i % 9] = s.charAt(i) - '0';
        }
        return b;
    }
}
