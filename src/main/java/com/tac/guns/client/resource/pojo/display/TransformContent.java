package com.tac.guns.client.resource.pojo.display;

import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;
import java.util.List;

public class TransformContent {
    @SerializedName("rotation")
    @Nullable
    private List<Float> rotation;
    @SerializedName("translation")
    @Nullable
    private List<Float> translation;
    @SerializedName("scale")
    @Nullable
    private List<Float> scale;

    @Nullable
    public List<Float> getRotation() {
        return rotation;
    }

    @Nullable
    public List<Float> getTranslation() {
        return translation;
    }

    @Nullable
    public List<Float> getScale() {
        return scale;
    }
}
