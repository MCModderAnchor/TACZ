package com.tac.guns.client.resource.pojo.skin.attachment;

import com.google.gson.annotations.SerializedName;
import com.tac.guns.client.resource.pojo.TransformScale;

public class AttachmentTransform {
    @SerializedName("scale")
    private TransformScale scale;

    public TransformScale getScale() {
        return scale;
    }
}
