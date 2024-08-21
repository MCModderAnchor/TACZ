package com.tacz.guns.resource.pojo.data.gun;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class MoveSpeed {
    @SerializedName("base")
    private float baseMultiplier = 0.0f;

    @SerializedName("aim")
    private float aimMultiplier = 0.0f;

    @SerializedName("reload")
    private float reloadMultiplier = 0.0f;

    public float getBaseMultiplier() {
        return baseMultiplier;
    }

    public float getAimMultiplier() {
        return aimMultiplier;
    }

    public float getReloadMultiplier() {
        return reloadMultiplier;
    }


    public static MoveSpeed of(MoveSpeed moveSpeed, List<MoveSpeed> modifiers) {
        MoveSpeed result = new MoveSpeed();
        result.baseMultiplier = moveSpeed.baseMultiplier;
        result.aimMultiplier = moveSpeed.aimMultiplier;
        result.reloadMultiplier = moveSpeed.reloadMultiplier;
        for (MoveSpeed modifier : modifiers) {
            result.baseMultiplier += modifier.baseMultiplier;
            result.aimMultiplier += modifier.aimMultiplier;
            result.reloadMultiplier += modifier.reloadMultiplier;
        }
        result.baseMultiplier = Math.max(0.0f, result.baseMultiplier);
        result.aimMultiplier = Math.max(0.0f, result.aimMultiplier);
        result.reloadMultiplier = Math.max(0.0f, result.reloadMultiplier);
        return result;
    }
}
