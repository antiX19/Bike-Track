package com.exemple.applicationble;

public class ResetPswd {
    private String identifier;

    private String newPassword;

    public ResetPswd(String identifier, String newPassword){
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
