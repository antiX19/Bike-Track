package com.exemple.applicationble;

import static android.app.ProgressDialog.show;
import static android.content.ContentValues.TAG;
import static com.exemple.applicationble.ConnexionActivity.BASE_URL;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText editTextEmailRecovery;
    private Button buttonSendRecovery;

    // Nouveaux éléments pour le code de récupération
    private EditText editTextRecoveryCode;
    private Button buttonValidateRecoveryCode;
    private String recoveryCode; // Code à 6 chiffres généré

    // Liaisons existantes pour la question secrète
    private LinearLayout layoutSecretQuestion;
    private TextView textViewSecretQuestion, buttonBypass;
    private EditText editTextSecretAnswer;
    private Button buttonValidateSecretAnswer;

    // Liaisons pour la réinitialisation du mot de passe
    private LinearLayout layoutNewPassword;
    private EditText editTextNewPassword, editTextConfirmNewPassword;
    private Button buttonUpdatePassword;
    String laquestion;
    String lareponse;
    String email;
    ApiService apiService;

    // Stocke la réponse secrète enregistrée pour comparaison
    private String storedSecretAnswer;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Liaisons existantes
        editTextEmailRecovery = findViewById(R.id.editTextEmailRecovery);
        buttonSendRecovery = findViewById(R.id.buttonSendRecovery);

        // Nouveaux éléments pour le code de récupération
        editTextRecoveryCode = findViewById(R.id.editTextRecoveryCode);
        buttonValidateRecoveryCode = findViewById(R.id.buttonValidateRecoveryCode);
        // Initialement cachés
        editTextRecoveryCode.setVisibility(View.GONE);
        buttonValidateRecoveryCode.setVisibility(View.GONE);

        // Liaisons pour la question secrète
        layoutSecretQuestion = findViewById(R.id.layoutSecretQuestion);
        textViewSecretQuestion = findViewById(R.id.textViewSecretQuestion);
        editTextSecretAnswer = findViewById(R.id.editTextSecretAnswer);
        buttonValidateSecretAnswer = findViewById(R.id.buttonValidateSecretAnswer);
        buttonBypass = findViewById(R.id.buttonBypass);
        buttonBypass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        // Liaisons pour la réinitialisation du mot de passe
        layoutNewPassword = findViewById(R.id.layoutNewPassword);
        editTextNewPassword = findViewById(R.id.editTextNewPassword);
        editTextConfirmNewPassword = findViewById(R.id.editTextConfirmNewPassword);
        buttonUpdatePassword = findViewById(R.id.buttonUpdatePassword);
        Retrofit retrofit = getRetrofitWithCertificate();
        apiService = retrofit.create(ApiService.class);

        // Lorsque l'utilisateur clique sur "Envoyer la demande"
        buttonSendRecovery.setOnClickListener(v -> {
            email = editTextEmailRecovery.getText().toString().trim();
            if (email.isEmpty()) {
                editTextEmailRecovery.setError("Veuillez saisir votre email");
                return;
            }
            // Génération d'un code aléatoire à 6 chiffres
            recoveryCode = String.valueOf(new Random().nextInt(900000) + 100000);
            // Envoi automatique du code par email
            sendRecoveryEmail(email, recoveryCode);
            Toast.makeText(ForgotPasswordActivity.this, "Un code de récupération a été envoyé à " + email, Toast.LENGTH_LONG).show();
            // Affichage du champ et du bouton pour saisir le code
            editTextRecoveryCode.setVisibility(View.VISIBLE);
            buttonValidateRecoveryCode.setVisibility(View.VISIBLE);
        });

        // Listener pour valider le code de récupération
        buttonValidateRecoveryCode.setOnClickListener(v -> {
            String userInput = editTextRecoveryCode.getText().toString().trim();
            if (userInput.isEmpty()) {
                editTextRecoveryCode.setError("Veuillez saisir le code de récupération");
                return;
            }
            if (userInput.equals(recoveryCode)) {
                //Toast.makeText(ForgotPasswordActivity.this, "Code correct", Toast.LENGTH_SHORT).show();
                // Après validation du code, poursuivre la procédure existante
                senddemande();
            } else {
                Toast.makeText(ForgotPasswordActivity.this, "Code incorrect", Toast.LENGTH_SHORT).show();
            }
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
                //Toast.makeText(ForgotPasswordActivity.this, "Réponse correcte. Vous pouvez changer votre mot de passe.", Toast.LENGTH_SHORT).show();
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
            Resepsw resepsw = new Resepsw(email, confirmPass);
            Call<Resepsw> call = apiService.postNewpsw(resepsw);
            call.enqueue(new Callback<Resepsw>() {
                @Override
                public void onResponse(Call<Resepsw> call, Response<Resepsw> response) {
                    if (response.isSuccessful()) {
                        new android.app.AlertDialog.Builder(ForgotPasswordActivity.this)
                                .setTitle("Changement de mot de passe")
                                .setMessage("Votre mot de passe a bien été changé")
                                .setPositiveButton("OK", (dialog, which) -> {
                                    Intent intent = new Intent(ForgotPasswordActivity.this, ConnexionActivity.class);
                                    startActivity(intent);
                                    finish();
                                })
                                .show();
                    } else {
                        //Toast.makeText(ForgotPasswordActivity.this, "Erreur côté serveur : " + response.message(), Toast.LENGTH_SHORT).show();
                        Log.e("serveur", "Erreur côté serveur : " + response.message());
                    }
                }

                @Override
                public void onFailure(Call<Resepsw> call, Throwable t) {
                    //Toast.makeText(ForgotPasswordActivity.this, "Erreur de connexion : " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("test2", "Erreur de connexion", t);
                }
            });
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
                                                break;
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
                            runOnUiThread(() -> {
                                textViewSecretQuestion.setText(laquestion);
                                layoutSecretQuestion.setVisibility(LinearLayout.VISIBLE);
                            });
                            break;
                        }
                    }
                    if(!found) {
                        //Toast.makeText(ForgotPasswordActivity.this, "Cet email n'est pas enregistré", Toast.LENGTH_SHORT).show();
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

    // Méthode pour envoyer automatiquement le code de récupération par email
    private void sendRecoveryEmail(String email, String code) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Remplacez "YOUR_PASSWORD" par le mot de passe réel de l'adresse "jordan.attiogbe@edu.esiee.fr"
                    GMailSender sender = new GMailSender("biketrack2025@gmail.com", "mnco eyng ebcu prdc");
                    sender.sendMail("Code de récupération de mot de passe",
                            "Votre code de récupération est : " + code,
                            "biketrack2025@gmail.com",
                            email);
                } catch (Exception e) {
                    Log.e(TAG, "Erreur lors de l'envoi de l'email de récupération", e);
                }
            }
        }).start();
    }

    private Retrofit getRetrofitWithCertificate() {
        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    // Classe d'aide pour l'envoi d'email via JavaMail
    // Assurez-vous d'avoir ajouté les dépendances nécessaires
    // Dans ForgotPasswordActivity, modifiez la classe GMailSender comme suit :
    public class GMailSender extends javax.mail.Authenticator {
        private String mailhost = "smtp.gmail.com";
        private String user;
        private String password;
        private javax.mail.Session session;

        // Bloc static supprimé

        public GMailSender(String user, String password) {
            this.user = user;
            this.password = password;
            java.util.Properties props = new java.util.Properties();
            props.put("mail.smtp.host", mailhost);
            props.put("mail.smtp.port", "465");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.socketFactory.port", "465");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.fallback", "false");
            session = javax.mail.Session.getDefaultInstance(props, this);
        }

        @Override
        protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
            return new javax.mail.PasswordAuthentication(user, password);
        }

        public synchronized void sendMail(String subject, String body, String senderEmail, String recipients) throws Exception {
            javax.mail.internet.MimeMessage message = new javax.mail.internet.MimeMessage(session);
            message.setSender(new javax.mail.internet.InternetAddress(senderEmail));
            message.setSubject(subject);
            message.setText(body);
            if (recipients.indexOf(',') > 0) {
                message.setRecipients(javax.mail.Message.RecipientType.TO, javax.mail.internet.InternetAddress.parse(recipients));
            } else {
                message.setRecipient(javax.mail.Message.RecipientType.TO, new javax.mail.internet.InternetAddress(recipients));
            }
            javax.mail.Transport.send(message);
        }
    }

}
