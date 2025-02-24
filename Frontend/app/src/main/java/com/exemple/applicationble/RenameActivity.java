package com.exemple.applicationble;

import android.bluetooth.BluetoothGatt;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class RenameActivity extends AppCompatActivity {

    private EditText editTextModuleName;
    private Handler handler = new Handler();
    private Button buttonConfirm;
    private BluetoothGatt bluetoothGatt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rename);

        editTextModuleName = findViewById(R.id.editTextModuleName);
        buttonConfirm = findViewById(R.id.buttonConfirm);

        // Récupérer la connexion BLE active depuis IdentificationActivity via la méthode statique
        bluetoothGatt = IdentificationActivity.getCurrentGatt();

        if (bluetoothGatt == null) {
            Toast.makeText(this, "Aucune connexion BLE active", Toast.LENGTH_SHORT).show();
            finish();
        }

        buttonConfirm.setOnClickListener(v -> {
            String newName = editTextModuleName.getText().toString().trim();
            if (newName.isEmpty()) {
                Toast.makeText(RenameActivity.this, "Entrez un nom valide", Toast.LENGTH_SHORT).show();
                return;
            }
            String command = "AT+NAME" + newName;
            IdentificationActivity.sendAtCommand(command);
            final String[] commands = {
                    "AT+MODE1",      // Note : j'ai retiré l'espace dans "AT+ MODE1"
                    "AT+NOTI1",
                    "AT+IBEA1",
                    "AT+MARJ0xFFFA",
                    "AT+MINOR0xFFFA"
            };

            // Envoi de chaque commande avec un délai de 2 secondes entre chacune
            for (int i = 0; i < commands.length; i++) {
                final String commandes = commands[i];
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        IdentificationActivity.sendAtCommand(commandes);
                    }
                }, i * 2000); // 2000 ms de délai par commande
            }

            // Appel de la fonction qui récupère le UUID depuis le site et envoie la commande AT+IBE0
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    fetchUuidFromWebsiteAndSendIbeCommand();
                }
            }, commands.length * 2000 + 1000);
        });
    }

    /**
     * Récupère le UUID depuis le site internet, affiche le premier groupe en majuscules dans une pop-up et
     * envoie la commande AT+IBE0 correspondante.
     */
    private void fetchUuidFromWebsiteAndSendIbeCommand() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("https://www.uuidgenerator.net/api/version4");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(5000);
                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        String uuid = reader.readLine();
                        reader.close();
                        if (uuid != null && !uuid.isEmpty()) {
                            // Extrait le premier groupe (8 caractères) et le met en majuscules
                            String firstGroup = uuid.split("-")[0].toUpperCase();
                            String command = "AT+IBE0" + firstGroup;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // Affiche une pop-up avec la variable firstGroup
                                    new AlertDialog.Builder(RenameActivity.this)
                                            .setTitle("UUID First Group")
                                            .setMessage("firstGroup: " + firstGroup)
                                            .setPositiveButton("OK", null)
                                            .show();

                                    IdentificationActivity.sendAtCommand(command);
                                    Toast.makeText(RenameActivity.this, "Commande envoyée: " + command, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(RenameActivity.this, "Erreur HTTP: " + responseCode, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(RenameActivity.this, "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }
}
