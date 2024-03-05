package com.tac.guns.resource.pojo;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;

public class AmmoIndexPOJO {
    @SerializedName("name")
    private String name;

    @SerializedName("display")
    private ResourceLocation display;

    @SerializedName("stack_size")
    private int stackSize;

    public String getName() {
        return name;
    }

    public ResourceLocation getDisplay() {
        return display;
    }

    public int getStackSize() {
        return stackSize;
    }
}
