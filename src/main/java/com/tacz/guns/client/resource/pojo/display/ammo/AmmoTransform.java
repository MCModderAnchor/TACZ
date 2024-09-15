package com.tacz.guns.client.resource.pojo.display.ammo;

import com.google.gson.annotations.SerializedName;
import com.tacz.guns.client.resource.pojo.TransformScale;

public class AmmoTransform {
    @SerializedName("scale")
    private TransformScale scale;

    public static AmmoTransform getDefault() {
        AmmoTransform ammoTransform = new AmmoTransform();
        ammoTransform.scale = TransformScale.getAmmoDefault();
        return ammoTransform;
    }

    public TransformScale getScale() {
        return scale;
    }
}
