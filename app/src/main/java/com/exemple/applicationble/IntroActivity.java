package com.exemple.applicationble;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class IntroActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "MyAppPrefs";
    private static final String KEY_FIRST_LAUNCH = "isFirstLaunch";

    private Button startButton;
    private Button exitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Vérifier si l'intro a déjà été affichée
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isFirstLaunch = prefs.getBoolean(KEY_FIRST_LAUNCH, true);
        if (!isFirstLaunch) {
            launchHomeActivity();
            return;
        }

        setContentView(R.layout.activity_homepage); // Votre layout d'intro

        startButton = findViewById(R.id.start_button);
        exitButton = findViewById(R.id.exit_button);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Mémoriser que l'intro a été affichée
                SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                editor.putBoolean(KEY_FIRST_LAUNCH, false);
                editor.apply();

                launchHomeActivity();
            }
        });

        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Quitte l'application
                finishAffinity();
            }
        });
    }

    private void launchHomeActivity() {
        // Lance HomeActivity (assurez-vous qu'elle est bien déclarée dans le manifeste)
        Intent intent = new Intent(IntroActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
