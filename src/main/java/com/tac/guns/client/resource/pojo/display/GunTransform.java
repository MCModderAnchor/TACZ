package com.tac.guns.client.resource.pojo.display;

import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;

public class GunTransform {
    @SerializedName("scale")
    @Nullable
    private TransformScale scale;

    @Nullable
    public TransformScale getScale() {
        return scale;
    }
}
