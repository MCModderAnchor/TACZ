package com.tacz.guns.resource.pojo.data.block;

import com.google.gson.annotations.SerializedName;
import com.tacz.guns.GunMod;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class BlockData {
    @NotNull
    @SerializedName("filter")
    private ResourceLocation filter = new ResourceLocation(GunMod.MOD_ID, "default");

    @NotNull
    public ResourceLocation getFilter() {
        return filter;
    }
}
