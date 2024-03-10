package com.tac.guns.client.resource.pojo.display.gun;

import com.google.gson.annotations.SerializedName;
import com.tac.guns.client.resource.pojo.display.TransformScale;

public class GunTransform {
    @SerializedName("scale")
    private TransformScale scale;

    public TransformScale getScale() {
        return scale;
    }
}
