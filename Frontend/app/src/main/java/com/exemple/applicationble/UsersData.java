package com.exemple.applicationble;

public class UsersData {


    private String pseudo;
    private String email;

    private String psw;

    private String UUID_velo;

    // Constructeur
    public UsersData(String pseudo, String email, String psw) {
        this.pseudo = pseudo;
        this.email = email;
        this.psw = psw;
    }

    public UsersData(String UUID_velo) {
        this.UUID_velo = UUID_velo;
    }

    // Getters et setters
    public String getUUID_velo() { return UUID_velo; }
    public void setUUID_velo(String UUID_velo) { this.UUID_velo = UUID_velo; }

    public String getPseudo() { return pseudo; }
    public void setPseudo(String pseudo) { this.pseudo = pseudo; }


    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPsw() { return psw; }
    public void setPsw(String psw) { this.psw = psw; }

}
