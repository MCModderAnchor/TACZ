package com.tacz.guns.resource.pojo.data.block;

import com.google.gson.annotations.SerializedName;
import com.tacz.guns.GunMod;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class BlockData {
    @NotNull
    @SerializedName("filter")
    private ResourceLocation filter = ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "default");

    @SerializedName("tabs")
    private List<TabConfig> tabs = new ArrayList<>();

    @NotNull
    public ResourceLocation getFilter() {
        return filter;
    }

    @NotNull
    public List<TabConfig> getTabs() {
        return tabs.isEmpty() ? TabConfig.DEFAULT_TABS : tabs;
    }
}
