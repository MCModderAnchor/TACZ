package com.tac.guns.client.resource.pojo.display;

import com.google.gson.annotations.SerializedName;

public class GunTransform {
    @SerializedName("scale")
    private TransformScale scale;

    public TransformScale getScale() {
        return scale;
    }
}
