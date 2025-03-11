package com.exemple.applicationble;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class HomePageActivity extends AppCompatActivity implements View.OnTouchListener {
    Button startButton, exitButton;
    private static final String PREFS_NAME = "MyPrefsFile";
    private static final String FIRST_LAUNCH = "isFirstLaunch";

    Animation scaleUp, scaleDown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage);

        startButton = findViewById(R.id.start_button);
        exitButton = findViewById(R.id.exit_button);

        // Chargement des animations
        scaleUp = AnimationUtils.loadAnimation(this, R.anim.scale_up);
        scaleDown = AnimationUtils.loadAnimation(this, R.anim.scale_down);

        // Définition du listener pour l'animation
        startButton.setOnTouchListener(this);
        exitButton.setOnTouchListener(this);

        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(FIRST_LAUNCH, true);
        editor.apply();
        boolean isFirstLaunch = preferences.getBoolean(FIRST_LAUNCH, true);

        // Bouton Start
        startButton.setOnClickListener(v -> {
            if (isFirstLaunch) {
                editor.putBoolean(FIRST_LAUNCH, false);
                editor.apply();
                Intent intent = new Intent(HomePageActivity.this, FirstConnectionActivity.class);
                startActivity(intent);
            } else {
                Intent intent = new Intent(HomePageActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        // Bouton Exit
        exitButton.setOnClickListener(v -> {
            finish();
            System.exit(0);
        });
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            view.startAnimation(scaleDown);
        } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
            view.startAnimation(scaleUp);
        }
        return false;
    }
}
