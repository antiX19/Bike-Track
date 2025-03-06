package com.exemple.applicationble;

public class Resetpsw {
    private String identifier;

    private String newPassword;

    public Resetpsw(String identifier, String newPassword){
        this.identifier = identifier;
        this.newPassword = newPassword;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
