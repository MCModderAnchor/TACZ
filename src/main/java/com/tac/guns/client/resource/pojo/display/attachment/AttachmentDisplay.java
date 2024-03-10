package com.tac.guns.client.resource.pojo.display.attachment;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;

public class AttachmentDisplay {
    @SerializedName("slot")
    private ResourceLocation slotTextureLocation;

    public ResourceLocation getSlotTextureLocation() {
        return slotTextureLocation;
    }
}
