package com.example.applicationble;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity{
    Button startButton, exitButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        startButton = findViewById(R.id.start_button);
        exitButton = findViewById(R.id.exit_button);

        // Bouton Start
        startButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        // Bouton Exit
        exitButton.setOnClickListener(v -> {
            finish(); // Ferme l'activité actuelle
            System.exit(0); // Quitte complètement l'application
        });
    }

}
