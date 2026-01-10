package com.example.sudoku;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnPlay = findViewById(R.id.btnPlay);
        Button btnExit = findViewById(R.id.btnExit);

        btnPlay.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, MenuActivity.class)));

        btnExit.setOnClickListener(v -> finish());
    }
}
