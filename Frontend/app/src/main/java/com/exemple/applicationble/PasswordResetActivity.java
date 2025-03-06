package com.exemple.applicationble;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class PasswordResetActivity extends AppCompatActivity {

    private EditText editTextRecoveryCode;
    private Button buttonValidateRecoveryCode;
    private EditText editTextNewPassword, editTextConfirmPassword;
    private Button buttonUpdatePassword;

    private String email;
    private String recoveryCode; // Code reçu depuis l'activité précédente

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_reset);

        // Récupérer les données passées depuis l'activité précédente
        email = getIntent().getStringExtra("email");
        recoveryCode = getIntent().getStringExtra("recoveryCode");

        // Liaison des vues
        editTextRecoveryCode = findViewById(R.id.editTextRecoveryCode);
        buttonValidateRecoveryCode = findViewById(R.id.buttonValidateRecoveryCode);
        editTextNewPassword = findViewById(R.id.editTextNewPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        buttonUpdatePassword = findViewById(R.id.buttonUpdatePassword);

        // Initialement, cacher les champs de modification du mot de passe
        editTextNewPassword.setVisibility(View.GONE);
        editTextConfirmPassword.setVisibility(View.GONE);
        buttonUpdatePassword.setVisibility(View.GONE);

        // Gestion du clic sur "Valider le code"
        buttonValidateRecoveryCode.setOnClickListener(v -> {
            String userInput = editTextRecoveryCode.getText().toString().trim();
            if (userInput.isEmpty()) {
                editTextRecoveryCode.setError("Veuillez saisir le code de récupération");
                return;
            }

            if (userInput.equals(recoveryCode)) {
                Toast.makeText(this, "Code correct", Toast.LENGTH_SHORT).show();

                // Afficher les champs pour modifier le mot de passe
                editTextNewPassword.setVisibility(View.VISIBLE);
                editTextConfirmPassword.setVisibility(View.VISIBLE);
                buttonUpdatePassword.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(this, "Code incorrect", Toast.LENGTH_SHORT).show();
            }
        });

        // Gestion du clic sur "Mettre à jour le mot de passe"
        buttonUpdatePassword.setOnClickListener(v -> {
            String newPassword = editTextNewPassword.getText().toString().trim();
            String confirmPassword = editTextConfirmPassword.getText().toString().trim();

            if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(this, "Les mots de passe doivent être identiques", Toast.LENGTH_SHORT).show();
                return;
            }

            // Simuler la mise à jour du mot de passe (vous pouvez appeler une API ici)
            updatePassword(email, newPassword);
        });
    }

    private void updatePassword(String email, String newPassword) {
        // Simuler la mise à jour du mot de passe (remplacez par une vraie logique)
        Log.d("PasswordResetActivity", "Mot de passe mis à jour pour " + email + ": " + newPassword);
        Toast.makeText(this, "Mot de passe mis à jour avec succès", Toast.LENGTH_SHORT).show();

        // Rediriger vers l'écran de connexion
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}
