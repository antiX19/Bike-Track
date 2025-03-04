package com.exemple.applicationble;

import static com.exemple.applicationble.ConnexionActivity.ps_mod_UUID;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MonitoringService extends Service {

    private static final String TAG = "MonitoringService";
    private static final String CHANNEL_ID = "MonitoringServiceChannel";
    private Handler handler;
    private int status = 1 ;
    private Runnable pollingRunnable;
    private ApiService apiService;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        // Démarrer le service en premier plan
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Service de Monitoring")
                .setContentText("En écoute des alertes serveur")
                .setSmallIcon(R.drawable.ic_notification) // Assurez-vous que R.drawable.ic_notification existe
                .build();

        startForeground(2, notification);

        // Initialisation de Retrofit avec SSL (certificat auto-signé)
     /*  try {
            InputStream certInputStream = getResources().openRawResource(R.raw.selfsigned);
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate ca = cf.generateCertificate(certInputStream);
            certInputStream.close();

            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setCertificateEntry("selfsigned", ca);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), new SecureRandom());

            OkHttpClient client = new OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) tmf.getTrustManagers()[0])
                    .hostnameVerifier((hostname, session) -> hostname.equals("13.36.126.63"))
                    .build();
*/
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("http://13.36.126.63:3000/") // URL de base du serveur
                   // .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            apiService = retrofit.create(ApiService.class);
     //   } catch (Exception e) {
       //     Log.e(TAG, "Erreur lors de l'initialisation de Retrofit", e);
        //}
        // Démarrer le polling du serveur
        handler = new Handler();
        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                pollServer();
                // Relance le polling toutes les 15 secondes (modifiable selon vos besoins)
                handler.postDelayed(this, 15000);
            }
        };
        handler.post(pollingRunnable);
    }

    /**
     * Interroge le serveur pour récupérer la liste des VeloData et vérifie le status.
     */
    private void pollServer() {
        if (apiService == null) {
            Log.e(TAG, "ApiService non initialisé");
            return;
        }
        Call<List<VeloData>> call = apiService.getVeloData(ps_mod_UUID);
        call.enqueue(new Callback<List<VeloData>>() {
            @Override
            public void onResponse(Call<List<VeloData>> call, retrofit2.Response<List<VeloData>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    List<VeloData> veloDataList = response.body();
                    // Récupérer la dernière donnée (on suppose qu'elle est à la fin de la liste)
                    VeloData latestData = veloDataList.get(veloDataList.size() - 1);
                    // Supposons que latestData.getStatus() == 1 signifie que le vélo a été retrouvé
                    String gps = latestData.getGps(); // Format attendu : "latitude,longitude" (ex: "45.551151,2.5515")
                    sendNotification("Bonne nouvelle",
                                "Votre vélo a été retrouvé !\nCliquez pour voir l'emplacement.",
                    gps);
                } else {
                    Log.e(TAG, "Erreur lors de la récupération des données : " + response.message());
                }
            }

            @Override
            public void onFailure(Call<List<VeloData>> call, Throwable t) {
                Log.e(TAG, "Erreur de connexion lors du poll du serveur", t);
            }
        });
    }

    /**
     * Affiche une notification qui, lorsqu'elle est cliquée, ouvre Google Maps sur l'emplacement indiqué.
     *
     * @param title   Le titre de la notification.
     * @param message Le message de la notification.
     * @param gps     Les coordonnées GPS au format "latitude,longitude".
     */
    private void sendNotification(String title, String message, String gps) {
        // Créez une URI pour Google Maps.
        // Par exemple, "geo:48.8566,2.3522?q=48.8566,2.3522" affichera un marqueur sur l'emplacement.
        String uri = "geo:" + gps + "?q=" + gps;
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        mapIntent.setPackage("com.google.android.apps.maps");

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                mapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // Nécessaire pour Android Oreo+
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);


        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            // Utilisation d'un identifiant unique pour chaque notification
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    /**
     * Crée le canal de notification requis pour Android O et plus.
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Canal Service de Monitoring",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(pollingRunnable);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
