package com.exemple.applicationble;

import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateAccountActivity extends AppCompatActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accountsettings);

        EditText emailInput = findViewById(R.id.emailInput);
        EditText usernameInput = findViewById(R.id.usernameInput);
        EditText passwordInput = findViewById(R.id.passwordInput);
        EditText confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        EditText pinCodeInput = findViewById(R.id.pinCodeInput);
        Spinner questions = findViewById(R.id.securityQuestionSpinner);
        EditText question_answer = findViewById(R.id.securityAnswerInput);
        TextView findBike = findViewById(R.id.btnFindBike);
        ImageView backButton = findViewById(R.id.backButton);
        ListView Ble_Devices = findViewById(R.id.listViewBleDevices);

        backButton.setOnClickListener(view -> finish());

    }

}
