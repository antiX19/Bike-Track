package com.exemple.applicationble;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        initializeUI();
    }

    private void initializeUI() {
        Button startButton = findViewById(R.id.start_button);
        Button exitButton = findViewById(R.id.exit_button);
        startButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, com.exemple.applicationble.LoginActivity.class);
            startActivity(intent);
        });
        exitButton.setOnClickListener(v -> exitApp());
    }

    private void exitApp() {
        moveTaskToBack(true);
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(1);
    }
}
