package com.exemple.applicationble;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;


import androidx.appcompat.app.AppCompatActivity;

public class FirstConnectionActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_connection);

        // Find buttons
        Button signin = findViewById(R.id.btnSignIn);
        Button no_signin = findViewById(R.id.btnNoSignIn);

        // Sign in button -> Navigate to CreateAccountActivity
        signin.setOnClickListener(view -> {
            startActivity(new Intent(this, CreateAccountActivity.class));
            finish(); // Close this activity to avoid stacking
        });

        // No sign-in button -> Navigate to HelpCommunityActivity
        no_signin.setOnClickListener(view -> {
            startActivity(new Intent(this, HelpCommunityActivity.class));
            finish();
        });
    }
}
