package com.tac.guns.client.resource.pojo.data;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;

public class GunData {
    @SerializedName("ammo")
    private ResourceLocation ammoId;

    public ResourceLocation getAmmoId() {
        return ammoId;
    }
}
