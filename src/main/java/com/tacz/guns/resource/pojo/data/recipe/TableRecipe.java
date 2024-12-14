package com.tacz.guns.resource.pojo.data.recipe;

import com.google.gson.annotations.SerializedName;
import com.tacz.guns.crafting.GunSmithTableIngredient;
import com.tacz.guns.crafting.result.GunSmithTableResult;

import java.util.List;

public class TableRecipe {
    @SerializedName("materials")
    private List<GunSmithTableIngredient> materials;

    @SerializedName("result")
    private GunSmithTableResult result;

    public List<GunSmithTableIngredient> getMaterials() {
        return materials;
    }

    public GunSmithTableResult getResult() {
        return result;
    }
}
