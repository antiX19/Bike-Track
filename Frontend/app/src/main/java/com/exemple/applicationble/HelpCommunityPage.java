package com.exemple.applicationble;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.OnBackPressedDispatcherOwner;
import androidx.appcompat.app.AppCompatActivity;

public class HelpCommunityPage extends AppCompatActivity { //minked to the activity_helper.xml
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_helper);
        ImageView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            if (HelpCommunityPage.this instanceof OnBackPressedDispatcherOwner) {
                OnBackPressedDispatcherOwner dispatcherOwner = HelpCommunityPage.this;
                dispatcherOwner.getOnBackPressedDispatcher().onBackPressed();
            }
        });

    }
}
