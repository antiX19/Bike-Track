package com.exemple.applicationble;

public class LoginData {
    private String identifier;
    //private String email;
    private String psw;

    public LoginData(String identifier, String psw) {
        this.identifier = identifier;
        this.psw = psw;
    }

    public String getemailOrPseudo() {
        return identifier;
    }

    public void setemailOrPseudo(String emailOrPseudo) {
        this.identifier = emailOrPseudo;
    }

    public String getPsw() {
        return psw;
    }

    public void setPsw(String psw) {
        this.psw = psw;
    }
}
