package com.tacz.guns.resource.pojo.data.gun;

import com.google.gson.annotations.SerializedName;

public class GunFireModeAdjustData {
    @SerializedName("damage")
    private float damageAmount = 0f;

    @SerializedName("rpm")
    private int roundsPerMinute = 0;

    @SerializedName("speed")
    private float speed = 0f;

    @SerializedName("knockback")
    private float knockback = 0f;

    @SerializedName("armor_ignore")
    private float armorIgnore = 0f;

    @SerializedName("head_shot_multiplier")
    private float headShotMultiplier = 0f;

    @SerializedName("aim_inaccuracy")
    private float aimInaccuracy = 0f;

    @SerializedName("other_inaccuracy")
    private float otherInaccuracy = 0f;

    public float getDamageAmount() {
        return damageAmount;
    }

    public int getRoundsPerMinute() {
        return roundsPerMinute;
    }

    public float getSpeed() {
        return speed;
    }

    public float getKnockback() {
        return knockback;
    }

    public float getArmorIgnore() {
        return armorIgnore;
    }

    public float getHeadShotMultiplier() {
        return headShotMultiplier;
    }

    public float getAimInaccuracy() {
        return aimInaccuracy;
    }

    public float getOtherInaccuracy() {
        return otherInaccuracy;
    }
}
