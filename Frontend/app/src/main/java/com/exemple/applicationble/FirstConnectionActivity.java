package com.exemple.applicationble;

import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.OnBackPressedDispatcherOwner;
import androidx.appcompat.app.AppCompatActivity;

public class FirstConnectionActivity extends AppCompatActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        View backButton = findViewById(R.id.backButton);
        Button signin = findViewById(R.id.btnSignIn);
        Button no_signin = findViewById(R.id.btnNoSignIn);

        signin.setOnClickListener(view -> {
                    Intent intent = new Intent(FirstConnectionActivity.this, IdentificationActivity.class);
                    startActivity(intent);

        });

        no_signin.setOnClickListener(view -> {


        });

        backButton.setOnClickListener(v -> {
            if (FirstConnectionActivity.this instanceof OnBackPressedDispatcherOwner) {
                OnBackPressedDispatcherOwner dispatcherOwner = (OnBackPressedDispatcherOwner) FirstConnectionActivity.this;
                dispatcherOwner.getOnBackPressedDispatcher().onBackPressed();
            }
        });
    }
}
