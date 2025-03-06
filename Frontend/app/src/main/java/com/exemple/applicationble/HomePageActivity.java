package com.exemple.applicationble;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class HomePageActivity extends AppCompatActivity{
    Button startButton, exitButton;
    private static final String PREFS_NAME = "MyPrefsFile";
    private static final String FIRST_LAUNCH = "isFirstLaunch";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage);
        startButton = findViewById(R.id.start_button);
        exitButton = findViewById(R.id.exit_button);
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isFirstLaunch = preferences.getBoolean(FIRST_LAUNCH, true); // Par défaut, c'est true

        // Bouton Start
        startButton.setOnClickListener(v -> {
            if (isFirstLaunch) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(FIRST_LAUNCH, false);
                editor.apply();
                Intent intent = new Intent(HomePageActivity.this, FirstConnectionActivity.class);
                startActivity(intent);

            } else {
                Intent intent = new Intent(HomePageActivity.this, ConnexionActivity.class);
                startActivity(intent);
            }

        });
        // Bouton Exit
        exitButton.setOnClickListener(v -> {
            finish(); // Ferme l'activité actuelle
            System.exit(0); // Quitte complètement l'application
        });
    }

}