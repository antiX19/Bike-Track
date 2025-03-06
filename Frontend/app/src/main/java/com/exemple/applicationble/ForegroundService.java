package com.exemple.applicationble;

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
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ForegroundService extends Service {

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
                byte[] manufacturerData = scanRecord.getManufacturerSpecificData(0x004C); // Apple ID pour iBeacon

                if (manufacturerData != null && manufacturerData.length >= 23) {
                    // Vérifier les octets d'identification iBeacon (0x02, 0x15)
                    if ((manufacturerData[0] & 0xFF) == 0x02 && (manufacturerData[1] & 0xFF) == 0x15) {
                        // Extraire les 16 octets correspondant à l'UUID iBeacon
                        byte[] uuidBytes = Arrays.copyOfRange(manufacturerData, 2, 18);
                        String hexUuid = bytesToHex(uuidBytes);

                        // Formater en UUID standard (XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX)
                        String formattedUuid = hexUuid.substring(0, 8) + "-" +
                                hexUuid.substring(8, 12) + "-" +
                                hexUuid.substring(12, 16) + "-" +
                                hexUuid.substring(16, 20) + "-" +
                                hexUuid.substring(20);

                        Log.d("BLE_SCAN", "iBeacon UUID trouvé: " + formattedUuid);
                        String shortUuid = formattedUuid.split("-")[0].toUpperCase(); // donne "0000ffe0"
                        Log.d("BLE_SCAN", "NEW UUID trouvé: " + shortUuid);
                        sendLocationToServer(shortUuid);
                       // sendLocationToServer(formattedUuid);
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

    // Méthode pour convertir un tableau de bytes en chaîne hexadécimale
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

        // Vérification de la permission de localisation
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("BLE_SCAN", "Permission de localisation manquante !");
            return;
        }

        ScanFilter filter = new ScanFilter.Builder().build();
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                //.setReportDelay(5000)
                .build();

        bluetoothLeScanner.startScan(Collections.singletonList(filter), settings, scanCallback);
        Log.d("BLE_SCAN", "Scan BLE démarré");
    }
private void checkstatus(String UUID_v){
   /* try {
        // Chargement du certificat depuis res/raw/selfsigned
        InputStream certInputStream = getResources().openRawResource(R.raw.selfsigned);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Certificate ca = cf.generateCertificate(certInputStream);
        certInputStream.close();

        // Création du keystore contenant le certificat
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setCertificateEntry("selfsigned", ca);

        // Création d'un TrustManager qui utilise notre keystore
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);

        // Initialisation du contexte SSL avec le TrustManager
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), new SecureRandom());*/

        // Configuration de Retrofit avec OkHttpClient personnalisé
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://13.36.126.63:3000/") // URL de base de votre serveur
                //.client(new okhttp3.OkHttpClient.Builder()
                  //      .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) tmf.getTrustManagers()[0])
                    //    .hostnameVerifier((hostname, session) -> hostname.equals("13.36.126.63"))
                      //  .build())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ApiService apiService = retrofit.create(ApiService.class);
        Call<List<Velostatus>> call = apiService.getVelostatus();
        call.enqueue(new Callback<List<Velostatus>>() {
            @Override
            public void onResponse(Call<List<Velostatus>> call, Response<List<Velostatus>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    List<Velostatus> velostatusList = response.body();
                    // Récupérer la dernière donnée (on suppose qu'elle est à la fin de la liste)
                    //Velostatus latestData = velostatusList.get(velostatusList.size() - 1);
                    // Supposons que latestData.getStatus() == 1 signifie que le vélo a été retrouvé
                    for( var i : velostatusList){
                        if (i.getStatus() && i.getUUID().equals(UUID_v)) {
                            sendLocationToServer(UUID_v);
                        }
                    }

                } else {
                    Log.e(TAG, "Erreur lors de la récupération des données : " + response.message());
                }
            }

            @Override
            public void onFailure(Call<List<Velostatus>> call, Throwable t) {
                Log.e(TAG, "Erreur de connexion lors du poll du serveur", t);
            }
        });
   // } catch (CertificateException | IOException | KeyStoreException | NoSuchAlgorithmException | KeyManagementException e) {
     //   Log.e(TAG, "Erreur lors du chargement du certificat ou de l'initialisation SSL", e);
//    }


}
    private void sendLocationToServer(String UUID_Velo) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Permission de localisation manquante.");
            return;
        }

        fusedLocationClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    String gps = latitude + "," + longitude;
                    Log.d("TAG", "Latitude : " + latitude + ", Longitude : " + longitude);
                    /*
                   try {
                        // Chargement du certificat depuis res/raw/selfsigned
                       InputStream certInputStream = getResources().openRawResource(R.raw.selfsigned);
                        CertificateFactory cf = CertificateFactory.getInstance("X.509");
                        Certificate ca = cf.generateCertificate(certInputStream);
                        certInputStream.close();

                        // Création du keystore contenant le certificat
                        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                        keyStore.load(null, null);
                        keyStore.setCertificateEntry("selfsigned", ca);

                        // Création d'un TrustManager qui utilise notre keystore
                        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                        tmf.init(keyStore);

                        // Initialisation du contexte SSL avec le TrustManager
                        SSLContext sslContext = SSLContext.getInstance("TLS");
                        sslContext.init(null, tmf.getTrustManagers(), new SecureRandom());

                        // Configuration de Retrofit avec OkHttpClient personnalisé
                        Retrofit retrofit = new Retrofit.Builder()
                                .baseUrl("https://13.36.126.63/") // URL de base de votre serveur
                                .client(new okhttp3.OkHttpClient.Builder()
                                      .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) tmf.getTrustManagers()[0])
                                       .hostnameVerifier((hostname, session) -> hostname.equals("13.36.126.63"))
                                        .build())
                                .addConverterFactory(GsonConverterFactory.create())
                                .build();*/
                    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl("http://13.36.126.63:3000/") // URL de base de votre serveur
                           // .client(new okhttp3.OkHttpClient.Builder()
                             //       .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) tmf.getTrustManagers()[0])
                               //     .hostnameVerifier((hostname, session) -> hostname.equals("13.36.126.63"))
                                 //   .build())
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();
                        ApiService apiService = retrofit.create(ApiService.class);
                        VeloData veloData = new VeloData(UUID_Velo, gps);
                        Log.d("TAG_yyyyyy", "Données à envoyer uuid: " + UUID_Velo +" Données à envoyer gps: " + gps);
                        // Envoi des données via POST
                        // Créer un objet VeloData à envoyer
                        VeloData veloDataq = new VeloData("1003", "45.8556,2.3522");

                        // Envoyer les données via POST
                        Call<VeloData> call = apiService.postVeloData(veloData);
                        call.enqueue(new Callback<VeloData>() {
                            @Override
                            public void onResponse(Call<VeloData> call, Response<VeloData> response) {
                                if (response.isSuccessful()) {
                                    //Toast.makeText(ForegroundService.this, "Données envoyées avec succès", Toast.LENGTH_SHORT).show();
                                    Log.d("TAG_SUCESS", "Données à envoyer uuid avec SUCCESS " );
                                    Toast.makeText(ForegroundService.this, "Données à envoyer uuid avec SUCCESS" , Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(ForegroundService.this, "Erreur côté serveur : " + response.message(), Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(Call<VeloData> call, Throwable t) {
                                //Toast.makeText(ForegroundService.this, "Erreur de connexion : " + t.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    //} catch (CertificateException | IOException | KeyStoreException | NoSuchAlgorithmException | KeyManagementException e) {
                      // Log.e(TAG, "Erreur lors du chargement du certificat ou de l'initialisation SSL", e);
                    //}
                } else {
                    Log.d(TAG, "Aucune dernière position disponible");
                }
            }
        });
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
