package com.exemple.applicationble;

public class UsersData {

    private String nom_module;

    private String pin;

    private String pseudo;
    private String email;

    private String psw;

    private String UUID_velo;


    private String secret_answer;

    private int secret_question_id;

    // Constructeur
    public UsersData(String pseudo, String email, String psw, String UUID, String nom_module, String pin, int secret_question_id, String secret_answer) {
        this.UUID_velo = UUID;
        this.pseudo = pseudo;
        this.email = email;
        this.psw = psw;
        this.nom_module = nom_module;
        this.pin = pin;
        this.secret_question_id = secret_question_id;
        this.secret_answer =secret_answer;
    }

   // public UsersData(String UUID_velo) {
       // this.UUID_velo = UUID_velo;
    //}

    // Getters et setters

    public int getSecret_question_id() {
        return secret_question_id;
    }

    public String getSecret_answer() {
        return secret_answer;
    }

    public void setSecret_answer(String secret_answer) {
        this.secret_answer = secret_answer;
    }

    public void setSecret_question_id(int secret_question_id) {
        this.secret_question_id = secret_question_id;
    }

    public String getUUID_velo() { return UUID_velo; }
    public void setUUID_velo(String UUID) { this.UUID_velo = UUID; }

    public String getBLE_name() {
        return nom_module;
    }

    public void setBLE_name(String BLE_name) {
        this.nom_module = BLE_name;
    }

    public String getBLE_Pin() {
        return pin;
    }

    public void setBLE_Pin(String BLE_Pin) {
        this.pin = BLE_Pin;
    }


    public String getPseudo() { return pseudo; }
    public void setPseudo(String pseudo) { this.pseudo = pseudo; }


    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPsw() { return psw; }
    public void setPsw(String psw) { this.psw = psw; }

}
