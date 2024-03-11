package com.tac.guns.client.resource.pojo.skin.attachment;

import com.google.gson.annotations.SerializedName;
import com.tac.guns.client.resource.pojo.TransformScale;

import javax.annotation.Nullable;

public class AttachmentTransform {
    @Nullable
    @SerializedName("scale")
    private TransformScale scale;
    
    @Nullable
    public TransformScale getScale() {
        return scale;
    }
}
