package com.exemple.applicationble;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class EmailInputActivity extends AppCompatActivity {

    private EditText editTextEmailRecovery;
    private Button buttonSendRecovery;
    private String recoveryCode;
    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_input);

        // Liaison des vues
        editTextEmailRecovery = findViewById(R.id.editTextEmailRecovery);
        buttonSendRecovery = findViewById(R.id.buttonSendRecovery);

        buttonSendRecovery.setOnClickListener(v -> {
            email = editTextEmailRecovery.getText().toString().trim();

            if (!isValidEmail(email)) {
                editTextEmailRecovery.setError("Veuillez saisir un email valide");
                return;
            }

            // Génération d'un code à 6 chiffres
            recoveryCode = String.valueOf(new Random().nextInt(900000) + 100000);

            // Envoi du mail
            sendRecoveryEmail(email, recoveryCode);
        });
    }

    private boolean isValidEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void sendRecoveryEmail(String email, String code) {
        new Thread(() -> {
            try {
                // Récupération sécurisée des identifiants depuis les ressources
                String senderEmail = getString(R.string.email_user);
                String senderPassword = getString(R.string.email_password);

                GMailSender sender = new GMailSender(senderEmail, senderPassword);
                sender.sendMail(
                        "Code de récupération de mot de passe",
                        "Votre code de récupération est : " + code,
                        senderEmail,
                        email
                );

                // Confirmation sur UI Thread
                runOnUiThread(() -> {
                    Toast.makeText(this, "Un code a été envoyé à " + email, Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(this, PasswordResetActivity.class);
                    intent.putExtra("email", email);
                    intent.putExtra("recoveryCode", recoveryCode);
                    startActivity(intent);
                    finish();
                });

            } catch (Exception e) {
                Log.e("EmailInputActivity", "Erreur lors de l'envoi de l'email", e);
                runOnUiThread(() ->
                        Toast.makeText(this, "Échec de l'envoi du mail. Vérifiez votre connexion.", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    public class GMailSender extends javax.mail.Authenticator {
        private String mailhost = "smtp.gmail.com";
        private String user;
        private String password;
        private javax.mail.Session session;
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
