package com.exemple.applicationble;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity{

    private EditText etUsername, etPassword;
    private CheckBox cbRememberMe;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        cbRememberMe = findViewById(R.id.cbRememberMe);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);

        sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        boolean isRemembered = sharedPreferences.getBoolean("remember", false);

        if (isRemembered) {
            etUsername.setText(sharedPreferences.getString("username", ""));
            etPassword.setText(sharedPreferences.getString("password", ""));
            cbRememberMe.setChecked(true);
        }

        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString();
            String password = etPassword.getText().toString();
            boolean remember = cbRememberMe.isChecked();

            if (remember) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("username", username);
                editor.putString("password", password);
                editor.putBoolean("remember", true);
                editor.apply();
            } else {
                sharedPreferences.edit().clear().apply();
            }

            // Simuler une connexion rÃ©ussie
            Toast.makeText(this, "Connexion réussie !", Toast.LENGTH_SHORT).show();
        });

        tvForgotPassword.setOnClickListener(v -> {
            Toast.makeText(this, "Redirection vers la récupération du mot de passe", Toast.LENGTH_SHORT).show();
        });
    }
}

