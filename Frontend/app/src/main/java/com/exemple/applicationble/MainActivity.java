package com.exemple.applicationble;

import static android.content.ContentValues.TAG;

import static com.exemple.applicationble.ConnexionActivity.pseudo_conn;
import static com.exemple.applicationble.IdentificationActivity.radioQuestion1;
import static com.exemple.applicationble.IdentificationActivity.radioQuestion2;
import static com.exemple.applicationble.IdentificationActivity.radioQuestion3;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.exemple.applicationble.R;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private Button buttonVelo, buttonCommu, buttonConnexion;
    private static final int PERMISSION_REQUEST_CODE = 1;

    public static List<String> question = new ArrayList<>();

    public static  boolean  isLoggedIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        isLoggedIn = prefs.getBoolean("isLoggedIn", false);
        if (isLoggedIn && pseudo_conn != null) {
            // Si connecté, rediriger directement vers l'interface principale (HomeActivity)
            Intent intent = new Intent(MainActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        demande();
        setContentView(R.layout.activity_main);
        buttonVelo = findViewById(R.id.button_velo);
        buttonCommu = findViewById(R.id.button_commu);
        buttonConnexion = findViewById(R.id.button_connexion);

        // Demande des autorisations nécessaires
        //requestPermissions();

        buttonCommu.setOnClickListener(v -> startForegroundService());
        buttonVelo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startForegroundService();
                // Lancement de l'activité d'identification pour "J'ai un vélo";
                Intent intent = new Intent(MainActivity.this, IdentificationActivity.class);
                startActivity(intent);
            }
        });

        // Ajout du listener pour le bouton "Connexion"
        buttonConnexion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               startForegroundService();
                // Lancement de l'activité de connexion
                Intent intent = new Intent(MainActivity.this, ConnexionActivity.class);
                startActivity(intent);
            }
        });

        // Autres listeners pour les autres boutons...
    }
private void demande(){
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, 102);
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Android 10+
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                Manifest.permission.FOREGROUND_SERVICE_LOCATION
        }, 101);
    } else {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION
        }, 101);
    }

}


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("PERMISSION", "Permission accordée !");
            } else {
                Log.e("PERMISSION", "Permission refusée !");
                Toast.makeText(this, "L'application nécessite la permission pour fonctionner", Toast.LENGTH_SHORT).show();
            }
        }
    }


    public void startForegroundService() {
        Intent serviceIntent = new Intent(this, ForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

    }
}
