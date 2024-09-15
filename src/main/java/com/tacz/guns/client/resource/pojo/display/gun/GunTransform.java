package com.tacz.guns.client.resource.pojo.display.gun;

import com.google.gson.annotations.SerializedName;
import com.tacz.guns.client.resource.pojo.TransformScale;

public class GunTransform {
    @SerializedName("scale")
    private TransformScale scale;

    public static GunTransform getDefault() {
        GunTransform gunTransform = new GunTransform();
        gunTransform.scale = TransformScale.getGunDefault();
        return gunTransform;
    }

    public TransformScale getScale() {
        return scale;
    }
}
