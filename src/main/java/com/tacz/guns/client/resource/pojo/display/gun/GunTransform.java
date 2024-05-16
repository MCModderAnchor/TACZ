package com.tacz.guns.client.resource.pojo.display.gun;

import com.google.gson.annotations.SerializedName;
import com.tacz.guns.client.resource.pojo.TransformScale;

public class GunTransform {
    @SerializedName("scale")
    private TransformScale scale;

    public TransformScale getScale() {
        return scale;
    }
}
