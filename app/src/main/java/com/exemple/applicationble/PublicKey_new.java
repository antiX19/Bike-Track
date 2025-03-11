package com.exemple.applicationble;

import com.google.gson.annotations.SerializedName;

public class PublicKey_new {
    @SerializedName("bob")
    private String bob;

    @SerializedName("alice")
    private String alice;

    @SerializedName("UUID_velo")
    private String UUID_velo;

    public PublicKey_new(String bob, String alice, String UUID_velo) {
        this.bob = bob;
        this.alice = alice;
        this.UUID_velo = UUID_velo;
    }

    public void setUUID(String UUID) {
        this.UUID_velo = UUID;
    }

    public String getUUID() {
        return UUID_velo;
    }

    public String getPublicKeyali() {
        return alice;
    }

    public String getPublicKeybob() {
        return bob;
    }

    public void setPublicKeyali(String publicKeyali) {
        this.alice = publicKeyali;
    }

    public void setPublicKeybob(String publicKeybob) {
        this.bob = publicKeybob;
    }
}
