package com.exemple.applicationble;

public class Velostatustrue {
    private int user_id;
    private boolean status;

    private String UUID_velo;

    public Velostatustrue(int user_id, boolean status) {
        this.user_id = user_id;
        this.status = status;
    }

    public int getId() {
        return user_id;
    }
    public void setid(int user_id){
        this.user_id = user_id;
    }

    public boolean getStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getUUID() {
        return UUID_velo;
    }

    public void setUUID(String UUID) {
        this.UUID_velo = UUID_velo;
    }
}
