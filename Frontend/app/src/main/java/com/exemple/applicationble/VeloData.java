package com.exemple.applicationble;

public class VeloData {
    private String UUID_velo;
    private String gps;
    private String timestamp;

    // Constructeur
    public VeloData(String UUID_velo, String gps) {
        this.UUID_velo = UUID_velo;
        this.gps = gps;
    }

    // Getters et setters
    public String getUUID_velo() { return UUID_velo; }
    public void setUUID_velo(String UUID_velo) { this.UUID_velo = UUID_velo; }

    public String getGps() { return gps; }
    public void setGps(String gps) { this.gps = gps; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}