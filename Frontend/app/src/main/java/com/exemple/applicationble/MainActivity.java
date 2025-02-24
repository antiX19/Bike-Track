package com.exemple.applicationble;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import com.exemple.applicationble.R;

public class MainActivity extends AppCompatActivity {

    private Button buttonVelo, buttonCommu, buttonConnexion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonVelo = findViewById(R.id.button_velo);
        buttonCommu = findViewById(R.id.button_commu);
        buttonConnexion = findViewById(R.id.button_connexion);

        buttonVelo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Lancement de l'activité d'identification pour "J'ai un vélo"
                Intent intent = new Intent(MainActivity.this, IdentificationActivity.class);
                startActivity(intent);
            }
        });

        // Autres listeners pour les autres boutons...
    }
}
