package com.tac.guns.client.resource.pojo.display;

import com.google.gson.annotations.SerializedName;

public class GunModelTexture {
    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("location")
    private String location;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }
}
