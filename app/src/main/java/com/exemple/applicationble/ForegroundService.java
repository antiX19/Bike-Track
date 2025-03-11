package com.exemple.applicationble;

import static com.exemple.applicationble.DiffieHellman.decodePublicKey;
import static com.exemple.applicationble.DiffieHellman.encodePublicKey;
import static com.exemple.applicationble.DiffieHellman.encryptedfunction;
import static com.exemple.applicationble.MonitoringService.getbobosecret;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.security.MessageDigest;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Collections;

import javax.crypto.SecretKey;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ForegroundService extends Service {

    private SecretKey aliceSharedKey;
    public static DiffieHellman.DiffieHellmanManager aliceManager;
    byte[] encryptedMessage;
    private static final String TAG = "ForegroundService";
    private static final String CHANNEL_ID = "BLE_SERVICE_CHANNEL";
    private BluetoothLeScanner bluetoothLeScanner;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForegroundService();
        Log.d(TAG, "Service créé");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        startBleScan();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "BLE Scanner Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void startForegroundService() {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Scanner BLE Actif")
                .setContentText("Recherche des appareils BLE à proximité...")
                .build();

        startForeground(1, notification);
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            ScanRecord scanRecord = result.getScanRecord();
            if (scanRecord != null) {
                // Récupérer les données du fabricant
                byte[] manufacturerData = scanRecord.getManufacturerSpecificData(0x004C);

                if (manufacturerData != null && manufacturerData.length >= 23) {
                    if ((manufacturerData[0] & 0xFF) == 0x02 && (manufacturerData[1] & 0xFF) == 0x15) {
                        byte[] uuidBytes = Arrays.copyOfRange(manufacturerData, 2, 18);
                        String hexUuid = bytesToHex(uuidBytes);
                        String formattedUuid = hexUuid.substring(0, 8) + "-" +
                                hexUuid.substring(8, 12) + "-" +
                                hexUuid.substring(12, 16) + "-" +
                                hexUuid.substring(16, 20) + "-" +
                                hexUuid.substring(20);
                        Log.d("BLE_SCAN", "iBeacon UUID trouvé: " + formattedUuid);
                        String shortUuid = formattedUuid.split("-")[0].toUpperCase();
                        Log.d("BLE_SCAN", "NEW UUID trouvé: " + shortUuid);
                        sendLocationToServer(shortUuid);
                    }
                } else {
                    Log.d("BLE_SCAN", "Pas de données iBeacon pour : " + result.getDevice().getAddress());
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("BLE_SCAN", "Échec du scan BLE : " + errorCode);
            new Handler().postDelayed(() -> startBleScan(), 1000);
        }
    };

    private String bytesToHex(byte[] bytes) {
        final char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static DiffieHellman.DiffieHellmanManager getmanageralice(){
        return aliceManager;
    }

    private void startBleScan() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.e("BLE_SCAN", "Bluetooth désactivé ou non disponible !");
            return;
        }

        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bluetoothLeScanner == null) {
            Log.e("BLE_SCAN", "Scanner BLE non disponible !");
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("BLE_SCAN", "Permission de localisation manquante !");
            return;
        }

        ScanFilter filter = new ScanFilter.Builder().build();
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .build();

        bluetoothLeScanner.startScan(Collections.singletonList(filter), settings, scanCallback);
        Log.d("BLE_SCAN", "Scan BLE démarré");
    }

    private void sendLocationToServer(String UUID_Velo) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Permission de localisation manquante.");
            return;
        }

        fusedLocationClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        String gps = latitude + "," + longitude;

                        new Thread(() -> {
                            try {
                                if (aliceManager == null) {
                                    aliceManager = new DiffieHellman.DiffieHellmanManager();
                                    aliceManager.generateKeyPair();
                                }

                                Retrofit retrofit = new Retrofit.Builder()
                                        .baseUrl("http://13.36.126.63:3000/")
                                        .addConverterFactory(GsonConverterFactory.create())
                                        .build();
                                ApiService apiService = retrofit.create(ApiService.class);

                                String alicekey = encodePublicKey(aliceManager.getPublicKey());
                                PublicKey_new publicKey = new PublicKey_new(null, alicekey, UUID_Velo);
                                Call<PublicKey_new> postCall = apiService.postpublickey(publicKey);

                                postCall.clone().enqueue(new Callback<PublicKey_new>() {
                                    @Override
                                    public void onResponse(Call<PublicKey_new> call, Response<PublicKey_new> response) {
                                        if (response.isSuccessful()) {
                                            Log.d("serveur2", "Clé publique envoyée avec succès");
                                        } else {
                                            Log.e("serveur2", "Erreur côté serveur : " + response.message());
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<PublicKey_new> call, Throwable t) {
                                        Log.e("serveur", "Erreur de connexion : " + t.getMessage(), t);
                                    }
                                });

                                // Appel synchrone pour récupérer la clé publique de Bob
                                Call<PublicKey_new> getCall = apiService.getPublicKey(UUID_Velo);
                                Response<PublicKey_new> getResponse = getCall.execute();
                                if (getResponse.isSuccessful() && getResponse.body() != null) {
                                    String bobkey = getResponse.body().getPublicKeybob();
                                    Log.d("GET_KEY", "Clé publique Bob (brute) : " + bobkey);
                                    PublicKey publicbobkey = decodePublicKey(bobkey);
                                    if (publicbobkey != null) {
                                        Log.d("DECODE_KEY", "Clé publique décodée avec succès : " + publicbobkey);
                                        aliceManager.setPeerPublicKey(publicbobkey);
                                    } else {
                                        Log.e("DECODE_KEY", "Échec du décodage !");
                                    }
                                } else {
                                    Log.e("GET_KEY", "Erreur côté serveur : " + getResponse.message());
                                }

                                aliceSharedKey = aliceManager.generateSharedSecret();
                                Log.d("ENCRYYYYY", "aliceSharedKey  :: " + aliceSharedKey);

                                if (getbobosecret() != null) {
                                    boolean keysEqual = MessageDigest.isEqual(aliceSharedKey.getEncoded(), getbobosecret().getEncoded());
                                    Log.d("BOOLEAN", "keysEqual  :: " + keysEqual);
                                } else {
                                    Log.d("BOOLEAN", "bobSharedKey n'est pas initialisé, on ne peut pas comparer les clés");
                                }

                                encryptedMessage = encryptedfunction(gps, aliceSharedKey);
                                String gps_encry = Base64.encodeToString(encryptedMessage, Base64.NO_WRAP);

                                VeloData veloData = new VeloData(UUID_Velo, gps_encry);
                                Call<VeloData> call2 = apiService.postVeloData(veloData);

                                call2.clone().enqueue(new Callback<VeloData>() {
                                    @Override
                                    public void onResponse(Call<VeloData> call, Response<VeloData> response) {
                                        if (response.isSuccessful()) {
                                            Log.d("TAG_SUCESS", "Données GPS envoyées avec SUCCESS");
                                        } else {
                                            Log.e("TAG_FAIL", "Erreur côté serveur : " + response.message());
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<VeloData> call, Throwable t) {
                                        Log.e("TAG_FAIL", "Erreur de connexion : " + t.getMessage(), t);
                                    }
                                });

                            } catch (Exception e) {
                                Log.e("ForegroundService", "Erreur lors du traitement post-DH", e);
                            }
                        }).start();

                    } else {
                        Log.d(TAG, "Aucune dernière position disponible");
                    }
                });
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
