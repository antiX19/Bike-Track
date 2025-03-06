package com.exemple.applicationble;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceManager {

    private static final String PREF_NAME = "AppPreferences";
    private static final String KEY_EMAIL_USER = "email_user";

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    public PreferenceManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    // Enregistrer un nouvel email
    public void setEmailUser(String email) {
        editor.putString(KEY_EMAIL_USER, email);
        editor.apply();
    }

    // Récupérer l'email enregistré
    public String getEmailUser() {
        return sharedPreferences.getString(KEY_EMAIL_USER, "biketrack2025@gmail.com"); // Valeur par défaut
    }
}
