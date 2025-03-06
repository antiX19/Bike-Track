package com.exemple.applicationble;

import static com.exemple.applicationble.LoginActivity.ps_mod_Id;
import static com.exemple.applicationble.LoginActivity.ps_mod_UUID;
import static com.exemple.applicationble.LoginActivity.ps_mod_name;
import static com.exemple.applicationble.LoginActivity.ps_mod_pin;
import static com.exemple.applicationble.CreateAccountActivity.PIN;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainPageActivity {

    private Button buttonVeloVole, buttonVeloTrouve;
    private EditText Scanner;
    private ListView lvModules;
    // Liste pour afficher le nom et le statut du périphérique
    private ArrayList<String> deviceList;
    // Liste pour stocker les objets BluetoothDevice détectés
    private ArrayList<BluetoothDevice> bleDeviceList;
    private ArrayAdapter<String> adapter;
    //ApiService apiService;
    static Intent monitoringServiceIntent;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;

    public static boolean vole = false;
    String deviceName;
    String baseName;

    // Variables pour le scan BLE
    private BluetoothAdapter mBluetoothAdapter;
    private boolean new_status;
    private BluetoothLeScanner mBluetoothLeScanner;
    private BluetoothGatt mBluetoothGatt;

    public static boolean bool = false;

    // Périphérique actuellement connecté
    private BluetoothDevice currentConnectedDevice = null;

    // UUID du service et de la caractéristique
    private static final UUID SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    private static final UUID CHARACTERISTIC_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");

    // Handler pour le délai entre l'envoi des commandes
    private Handler commandHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        buttonEco = findViewById(R.id.btnEco);
        buttonVeloVole = findViewById(R.id.buttonVeloVole);
        buttonVeloTrouve = findViewById(R.id.buttonVeloTrouve);
        // Récupération de la ListView
        Scanner = findViewById(R.id.scanBLE);
        lvModules = findViewById(R.id.listViewBleDevices);

        ImageButton buttonMenu = findViewById(R.id.buttonMenu);

        // Désactivation initiale du bouton "Non Visible" (maintenant "économie d'énergie")
        buttonEco.setEnabled(false);
        // Changement du libellé du bouton "Non Visible"

        if(vole){
            buttonVeloVole.setEnabled(false); // Désactiver "Vélo Volé"
        } else {
            buttonVeloTrouve.setEnabled(false); // Désactiver "Vélo Trouvé"
        }

        // Initialisation des listes et de l'adapter
        deviceList = new ArrayList<>();
        bleDeviceList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceList);
        lvModules.setAdapter(adapter);

        Scanner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startBleScan();
                Toast.makeText(MainPageActivity.this, "Scan BLE lancé", Toast.LENGTH_SHORT).show();
            }
        });

        // Configuration du bouton menu
        buttonMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(MainPageActivity.this, v);
                popupMenu.getMenu().add("Sign out");
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(android.view.MenuItem item) {
                        if ("Sign out".equals(item.getTitle())) {
                            SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.remove("isLoggedIn");
                            editor.apply();
                            Intent intent = new Intent(MainPageActivity.this, LoginActivity.class);
                            startActivity(intent);
                            finish();
                            return true;
                        }
                        return false;
                    }
                });
                popupMenu.show();
            }
        });
        buttonEco.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(bool){
                    buttonEco.setText("Economie d'énergie");
                    if (currentConnectedDevice != null && mBluetoothGatt != null) {
                        Handler handler = new Handler();
                        handler.postDelayed(() -> sendAtCommand("AT+POWE3"), 0);
                        handler.postDelayed(() -> sendAtCommand("AT+PWRM1"), 200);
                        handler.postDelayed(() -> sendAtCommand("AT+ADVI5"), 400);

                    } else {
                        Toast.makeText(HomeActivity.this, "Aucun module connecté", Toast.LENGTH_SHORT).show();
                    }
                }else{
                    buttonEco.setText("Mode normal");
                    if (currentConnectedDevice != null && mBluetoothGatt != null) {
                        Handler handler = new Handler();
                        handler.postDelayed(() -> sendAtCommand("AT+POWE2"), 0);
                        handler.postDelayed(() -> sendAtCommand("AT+ADVIA"), 200);
                        handler.postDelayed(() -> sendAtCommand("AT+PWRM0"), 400);

                        //buttonEco.setEnabled(false);
                    } else {
                        Toast.makeText(HomeActivity.this, "Aucun module connecté", Toast.LENGTH_SHORT).show();
                    }

                }
                bool = !bool;
            }
        });




        Log.d("UUID_VERIF", "L'UUID est : "+ ps_mod_UUID);
        // Bouton "Vélo Volé"
        buttonVeloVole.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                monitoringServiceIntent = new Intent(HomeActivity.this, MonitoringService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(monitoringServiceIntent);
                } else {
                    startService(monitoringServiceIntent);
                }
                vole = true;
                String pseuo = pseudo_conn;
                new_status = true;
                poststatus(new_status);
                Toast.makeText(HomeActivity.this, "Bouton Vélo Volé cliqué", Toast.LENGTH_SHORT).show();
                buttonVeloVole.setEnabled(false); // Désactiver "Vélo Volé"
                buttonVeloTrouve.setEnabled(true); // Activer "Vélo Trouvé"
            }
        });

        // Bouton "Vélo Trouvé"
        buttonVeloTrouve.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vole = false;
                String pseuo = pseudo_conn;
                new_status = false;
                poststatustrue(new_status);
                Toast.makeText(HomeActivity.this, "Bouton Vélo Trouvé cliqué", Toast.LENGTH_SHORT).show();
                stopService(monitoringServiceIntent);
                buttonVeloTrouve.setEnabled(false);
                buttonVeloVole.setEnabled(true);
            }
        });

        // Configuration de la ListView pour la sélection d'un module BLE
        lvModules.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothDevice selectedDevice = bleDeviceList.get(position);
                SharedPreferences sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
                String storedPin = sharedPreferences.getString("PIN_CODE", null);
                if (storedPin != null) {
                    Log.d("PIN", "Stored PIN is: " + storedPin);
                } else {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("PIN_CODE", PIN);
                    editor.apply();
                    Log.d("PIN", "Stored PIN is: " + PIN);
                }
                // Vérifier que l'appareil est déjà apparié
                if (selectedDevice.getBondState() != BluetoothDevice.BOND_BONDED) {
                    try {
                        Method setPinMethod = selectedDevice.getClass().getMethod("setPin", byte[].class);
                        byte[] pinBytes = String.valueOf(PIN).getBytes("UTF-8");
                        Boolean result = (Boolean) setPinMethod.invoke(selectedDevice, new Object[]{pinBytes});
                        if (result != null && result) {
                            Toast.makeText(HomeActivity.this, "PIN auto défini: " + PIN, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(HomeActivity.this, "Échec de la définition du PIN", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(HomeActivity.this, "Erreur lors de la définition du PIN", Toast.LENGTH_SHORT).show();
                    }
                    // Lance le processus d'appariement et informe l'utilisateur
                    selectedDevice.createBond();
                    Toast.makeText(HomeActivity.this, "Module non connecté. Veuillez entrer le code PIN sur le module.", Toast.LENGTH_LONG).show();
                    return;
                } else {
                    // Si l'appareil est déjà apparié, procéder à la connexion
                    if (currentConnectedDevice != null &&
                            currentConnectedDevice.getAddress().equals(selectedDevice.getAddress())) {
                        if (mBluetoothGatt != null) {
                            mBluetoothGatt.disconnect();
                            mBluetoothGatt.close();
                            mBluetoothGatt = null;
                        }
                        currentConnectedDevice = null;
                        Toast.makeText(HomeActivity.this, "Déconnecté du module", Toast.LENGTH_SHORT).show();
                        updateDeviceStatus(selectedDevice, false);
                        buttonEco.setEnabled(false);
                    } else {
                        if (currentConnectedDevice != null && mBluetoothGatt != null) {
                            mBluetoothGatt.disconnect();
                            mBluetoothGatt.close();
                            mBluetoothGatt = null;
                        }
                        mBluetoothGatt = selectedDevice.connectGatt(HomeActivity.this, false, mGattCallback);
                        currentConnectedDevice = selectedDevice;
                        Toast.makeText(HomeActivity.this, "Connexion en cours à " +
                                        ((selectedDevice.getName() != null && !selectedDevice.getName().isEmpty())
                                                ? selectedDevice.getName() : selectedDevice.getAddress()),
                                Toast.LENGTH_SHORT).show();
                        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled() && mBluetoothLeScanner != null) {
                            mBluetoothLeScanner.stopScan(mScanCallback);
                        }
                    }
                }
            }
        });
        ;

        // Démarrer le scan BLE
        startBleScan();
    }

    // ScanCallback filtrant selon le nom enregistré
    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            BluetoothDevice device = result.getDevice();
            // Filtrer uniquement par ps_mod_name
            if (device.getName() == null || !device.getName().equals(ps_mod_name)) {
                return;
            }
            deviceName = device.getName();
            final String baseName = (deviceName != null && !deviceName.isEmpty())
                    ? deviceName : device.getAddress();
            final String displayName = baseName + " - déconnecté";
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    boolean alreadyPresent = false;
                    for (BluetoothDevice d : bleDeviceList) {
                        if (d.getAddress().equals(device.getAddress())) {
                            alreadyPresent = true;
                            break;
                        }
                    }
                    if (!alreadyPresent) {
                        bleDeviceList.add(device);
                        deviceList.add(displayName);
                        adapter.notifyDataSetChanged();
                    }
                }
            });
        }

        @Override
        public void onBatchScanResults(java.util.List<ScanResult> results) {
            for (ScanResult result : results) {
                BluetoothDevice device = result.getDevice();
                // Filtrer uniquement par ps_mod_name
                if (device.getName() == null || !device.getName().equals(ps_mod_name)) {
                    continue;
                }
                deviceName = device.getName();
                final String baseName = (deviceName != null && !deviceName.isEmpty())
                        ? deviceName : device.getAddress();
                final String displayName = baseName + " - déconnecté";
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!bleDeviceList.contains(device)) {
                            bleDeviceList.add(device);
                            deviceList.add(displayName);
                            adapter.notifyDataSetChanged();
                        }
                    }
                });
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            runOnUiThread(() ->
                    Toast.makeText(MainPageActivity.this, "Scan échoué: " + errorCode, Toast.LENGTH_SHORT).show());
        }
    };

    // Callback pour la connexion BLE incluant la découverte des services
    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Toast.makeText(MainPageActivity.this, "Connecté au module BLE", Toast.LENGTH_SHORT).show();
                        // Activation du bouton "économie d'énergie" dès que le module est connecté
                        buttonEco.setEnabled(true);
                        // Lancer la découverte des services
                        gatt.discoverServices();
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        Toast.makeText(MainPageActivity.this, "Déconnecté du module BLE", Toast.LENGTH_SHORT).show();
                        updateDeviceStatus(gatt.getDevice(), false);
                        // Désactivation du bouton en cas de déconnexion
                        buttonEco.setEnabled(false);
                    }
                }
            });
        }
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainPageActivity.this, "Services découverts", Toast.LENGTH_SHORT).show();
                        updateDeviceStatus(gatt.getDevice(), true);
                    }
                });
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainPageActivity.this, "Échec de la découverte des services", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    };

    // Met à jour le statut affiché pour le périphérique dans la ListView
    private void updateDeviceStatus(BluetoothDevice device, boolean connected) {
        int index = -1;
        for (int i = 0; i < bleDeviceList.size(); i++) {
            if (bleDeviceList.get(i).getAddress().equals(device.getAddress())) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            baseName = device.getName();
            if (baseName == null || baseName.isEmpty()) {
                baseName = device.getAddress();
            }
            String statusText = connected ? " - connecté" : " - déconnecté";
            deviceList.set(index, baseName + statusText);
            adapter.notifyDataSetChanged();
        }
    }

    // Méthode pour envoyer une commande AT au module BLE avec des Toasts de confirmation
    private void sendAtCommand(String command) {
        if (mBluetoothGatt == null) {
            Toast.makeText(MainPageActivity.this, "mBluetoothGatt est null", Toast.LENGTH_SHORT).show();
            return;
        }
        BluetoothGattService service = mBluetoothGatt.getService(SERVICE_UUID);
        if (service == null) {
            Toast.makeText(MainPageActivity.this, "Service non découvert", Toast.LENGTH_SHORT).show();
            return;
        }
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(CHARACTERISTIC_UUID);
        if (characteristic == null) {
            Toast.makeText(MainPageActivity.this, "Caractéristique non trouvée", Toast.LENGTH_SHORT).show();
            return;
        }
        characteristic.setValue(command.getBytes());
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        boolean success = mBluetoothGatt.writeCharacteristic(characteristic);
        if (!success) {
            Toast.makeText(MainPageActivity.this, "Échec de l'envoi de la commande: " + command, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(MainPageActivity.this, "Commande envoyée: " + command, Toast.LENGTH_SHORT).show();
        }
    }



    public void poststatus(boolean status) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://13.36.126.63:3000/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ApiService apiService = retrofit.create(ApiService.class);
        Velostatus velostatus = new Velostatus(ps_mod_Id, status);
        Call<Velostatus> call = apiService.postVelostatus(ps_mod_UUID, velostatus);
        Log.d("serveur", "Requête envoyée " + call.request().toString() + " And veloData is " + velostatus);
        call.enqueue(new Callback<Velostatus>() {
            @Override
            public void onResponse(Call<Velostatus> call, Response<Velostatus> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(HomeActivity.this, "Données envoyées avec succès", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(HomeActivity.this, "Erreur côté serveur : " + response.message(), Toast.LENGTH_SHORT).show();
                    Log.e("serveur", "Erreur côté serveur : " + response.message());
                }
            }

            @Override
            public void onFailure(Call<Velostatus> call, Throwable t) {
                Toast.makeText(HomeActivity.this, "Erreur de connexion : " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("test2", "Erreur de connexion", t);
            }
        });
    }

    public void poststatustrue(boolean status) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://13.36.126.63:3000/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ApiService apiService = retrofit.create(ApiService.class);
        Velostatustrue velostatustrue = new Velostatustrue(ps_mod_Id, status);
        Call<Velostatustrue> call = apiService.postVelostatustrue(ps_mod_UUID, velostatustrue);
        Log.d("serveur", "Requête envoyée " + call.request().toString() + " And veloData is " + velostatustrue);
        call.enqueue(new Callback<Velostatustrue>() {
            @Override
            public void onResponse(Call<Velostatustrue> call, Response<Velostatustrue> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(HomeActivity.this, "Données envoyées avec succès", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(HomeActivity.this, "Erreur côté serveur : " + response.message(), Toast.LENGTH_SHORT).show();
                    Log.e("serveur", "Erreur côté serveur : " + response.message());
                }
            }

            @Override
            public void onFailure(Call<Velostatustrue> call, Throwable t) {
                Toast.makeText(HomeActivity.this, "Erreur de connexion : " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("test2", "Erreur de connexion", t);
            }
        });
    }

    // Méthode pour démarrer le scan BLE
    private void startBleScan() {
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth désactivé ou non disponible", Toast.LENGTH_SHORT).show();
            return;
        }
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mBluetoothLeScanner.startScan(mScanCallback);
        Toast.makeText(this, "Scan BLE démarré...", Toast.LENGTH_SHORT).show();
    }

}
