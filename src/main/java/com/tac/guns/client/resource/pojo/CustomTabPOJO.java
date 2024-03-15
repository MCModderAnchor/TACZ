package com.tac.guns.client.resource.pojo;

import com.google.gson.annotations.SerializedName;
import net.minecraft.world.item.ItemStack;

public class CustomTabPOJO {
    @SerializedName("name")
    private String nameKey;

    @SerializedName("icon")
    private ItemStack iconStack;

    public ItemStack getIconStack() {
        return iconStack;
    }

    public String getNameKey() {
        return nameKey;
    }
}
