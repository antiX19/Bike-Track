package com.exemple.applicationble;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class HelpCommunityActivity extends AppCompatActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_helpcommunity);
        ImageView backbutton = findViewById(R.id.backButton);

        backbutton.setOnClickListener(view -> {
            Intent intent = new Intent(HelpCommunityActivity.this, CreateAccountActivity.class);
            startActivity(intent);
        });
    }
}
