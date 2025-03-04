package com.exemple.applicationble;

import static android.app.ProgressDialog.show;
import static android.content.ContentValues.TAG;
import static com.exemple.applicationble.ConnexionActivity.BASE_URL;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText editTextEmailRecovery;
    private Button buttonSendRecovery;

    // Nouveaux champs pour la question secrète
    private LinearLayout layoutSecretQuestion;
    private TextView textViewSecretQuestion;
    private EditText editTextSecretAnswer;
    private Button buttonValidateSecretAnswer;

    // Champs pour réinitialiser le mot de passe
    private LinearLayout layoutNewPassword;
    private EditText editTextNewPassword, editTextConfirmNewPassword;
    private Button buttonUpdatePassword;
    String laquestion;

    String lareponse;
    String email;
    ApiService apiService;

    // Stocke la réponse secrète enregistrée pour comparaison
    private String storedSecretAnswer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Liaisons existantes
        editTextEmailRecovery = findViewById(R.id.editTextEmailRecovery);
        buttonSendRecovery = findViewById(R.id.buttonSendRecovery);

        // Liaisons pour la question secrète
        layoutSecretQuestion = findViewById(R.id.layoutSecretQuestion);
        textViewSecretQuestion = findViewById(R.id.textViewSecretQuestion);
        editTextSecretAnswer = findViewById(R.id.editTextSecretAnswer);
        buttonValidateSecretAnswer = findViewById(R.id.buttonValidateSecretAnswer);

        // Liaisons pour la réinitialisation du mot de passe
        layoutNewPassword = findViewById(R.id.layoutNewPassword);
        editTextNewPassword = findViewById(R.id.editTextNewPassword);
        editTextConfirmNewPassword = findViewById(R.id.editTextConfirmNewPassword);
        buttonUpdatePassword = findViewById(R.id.buttonUpdatePassword);
        Retrofit retrofit = getRetrofitWithCertificate();
        apiService = retrofit.create(ApiService.class);
        // Initialement, les layouts secret et new password sont cachés (déjà définis dans le XML).

        buttonSendRecovery.setOnClickListener(v -> {
            email = editTextEmailRecovery.getText().toString().trim();
            if (email.isEmpty()) {
                editTextEmailRecovery.setError("Veuillez saisir votre email");
                return;
            }
            // Appel de la méthode qui vérifie si l'email existe et récupère la question secrète
            senddemande();
            Toast.makeText(ForgotPasswordActivity.this, "Demande de réinitialisation envoyée pour " + email, Toast.LENGTH_LONG).show();
        });

        // Listener pour valider la réponse secrète
        buttonValidateSecretAnswer.setOnClickListener(v -> {
            String userAnswer = editTextSecretAnswer.getText().toString().trim();
            Log.d(TAG, "Réponse saisie: " + userAnswer + " | Réponse stockée: " + storedSecretAnswer);
            if (userAnswer.isEmpty()) {
                editTextSecretAnswer.setError("Veuillez saisir votre réponse");
                return;
            }
            if (userAnswer.equalsIgnoreCase(storedSecretAnswer)) {
                Toast.makeText(ForgotPasswordActivity.this, "Réponse correcte. Vous pouvez changer votre mot de passe.", Toast.LENGTH_SHORT).show();
                layoutNewPassword.setVisibility(LinearLayout.VISIBLE);
            } else {
                Toast.makeText(ForgotPasswordActivity.this, "La réponse à la question secrète est incorrecte", Toast.LENGTH_SHORT).show();
                layoutNewPassword.setVisibility(LinearLayout.GONE);
                editTextSecretAnswer.setError("Réponse incorrecte");
            }
        });



        // Listener pour mettre à jour le mot de passe
        buttonUpdatePassword.setOnClickListener(v -> {
            String newPass = editTextNewPassword.getText().toString().trim();
            String confirmPass = editTextConfirmNewPassword.getText().toString().trim();
            if(newPass.isEmpty() || confirmPass.isEmpty()){
                Toast.makeText(ForgotPasswordActivity.this, "Veuillez remplir les deux champs", Toast.LENGTH_SHORT).show();
                return;
            }
            if(!newPass.equals(confirmPass)){
                Toast.makeText(ForgotPasswordActivity.this, "Les mots de passe doivent être identiques", Toast.LENGTH_SHORT).show();
                editTextNewPassword.setError("Les mots de passe ne correspondent pas");
                editTextConfirmNewPassword.setError("Les mots de passe ne correspondent pas");
                return;
            }
            Resepsw resepsw = new Resepsw(email,confirmPass);
            // Envoyer les données via POST
            Call<Resepsw> call = apiService.postNewpsw(resepsw);
            //Log.d("serveur", "Requête envoyée " + call.request().toString() + " And userData is " + Resepsw);
            call.enqueue(new Callback<Resepsw>() {
                @Override
                public void onResponse(Call<Resepsw> call, Response<Resepsw> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(ForgotPasswordActivity.this, "Données envoyées avec succès", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(ForgotPasswordActivity.this, "Erreur côté serveur : " + response.message(), Toast.LENGTH_SHORT).show();
                        Log.e("serveur", "Erreur côté serveur : " + response.message());
                    }
                }

                @Override
                public void onFailure(Call<Resepsw> call, Throwable t) {
                    Toast.makeText(ForgotPasswordActivity.this, "Erreur de connexion : " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("test2", "Erreur de connexion", t);
                }
            });
            // Ici, appelez votre API pour mettre à jour le mot de passe
            Toast.makeText(ForgotPasswordActivity.this, "Mot de passe changé avec succès", Toast.LENGTH_LONG).show();
        });
    }

    // Méthode pour vérifier l'existence de l'email et récupérer la question secrète
    private void senddemande() {
        Call<List<Usersdata2>> call = apiService.getUsersData();
        call.enqueue(new Callback<List<Usersdata2>>() {
            @Override
            public void onResponse(Call<List<Usersdata2>> call, Response<List<Usersdata2>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    List<Usersdata2> usersDataList = response.body();
                    boolean found = false;
                    for (Usersdata2 user : usersDataList) {
                        if(user.getEmail() != null && user.getEmail().equals(email)) {
                            found = true;
                            // Récupération de la question secrète et de la réponse enregistrée
                            Call<List<Secretquestion>> call_be = apiService.getscretquestion();
                            call_be.enqueue(new Callback<List<Secretquestion>>() {
                                @Override
                                public void onResponse(Call<List<Secretquestion>> call_be, Response<List<Secretquestion>> response) {
                                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                                        List<Secretquestion> secretQuestionList = response.body();
                                        for (Secretquestion i : secretQuestionList) {
                                            if (i.getId() == user.getSecret_question_id()) {
                                                laquestion = i.getQuestion();
                                                lareponse = user.getSecret_answer();
                                                break;  // Dès qu'on a trouvé la question, on quitte la boucle
                                            }
                                        }
                                        storedSecretAnswer = lareponse;
                                        runOnUiThread(() -> {
                                            textViewSecretQuestion.setText(laquestion);
                                            layoutSecretQuestion.setVisibility(LinearLayout.VISIBLE);
                                        });
                                    } else {
                                        Log.e(TAG, "Erreur lors de la récupération des données : " + response.message());
                                    }
                                }

                                @Override
                                public void onFailure(Call<List<Secretquestion>> call_be, Throwable t) {
                                    Log.e(TAG, "Erreur de connexion lors du poll du serveur", t);
                                }
                            });


                            String secretQuestion = laquestion;
                            storedSecretAnswer = lareponse/*user.getSecretAnswer()*/;
                            runOnUiThread(() -> {
                                textViewSecretQuestion.setText(secretQuestion);
                                layoutSecretQuestion.setVisibility(LinearLayout.VISIBLE);
                            });
                            break;
                        }
                    }
                    if(!found) {
                        Toast.makeText(ForgotPasswordActivity.this, "Cet email n'est pas enregistré", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "Erreur lors de la récupération des données : " + response.message());
                }
            }

            @Override
            public void onFailure(Call<List<Usersdata2>> call, Throwable t) {
                Log.e(TAG, "Erreur de connexion lors du poll du serveur", t);
            }
        });
    }

    private Retrofit getRetrofitWithCertificate() {
        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                // .client(client) // Si vous avez configuré un client SSL personnalisé
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }


}
