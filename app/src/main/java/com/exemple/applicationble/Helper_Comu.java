package com.exemple.applicationble;

import static com.exemple.applicationble.HomeActivity.monitoringServiceIntent;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class Helper_Comu extends AppCompatActivity {
    private ImageView buttonBypass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Affiche le layout activity_helper.xml
        setContentView(R.layout.activity_helper);
        buttonBypass=findViewById(R.id.backButton);
        buttonBypass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Helper_Comu.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}
