package com.tac.guns.client.resource.pojo.display;

import com.google.gson.annotations.SerializedName;

public class GunModelTexture {
    @SerializedName("name")
    private String name;

    @SerializedName("location")
    private String location;

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }
}
