package com.tacz.guns.client.resource.pojo.display.gun;

import com.google.gson.annotations.SerializedName;

public enum DamageStyle {
    @SerializedName("total")
    TOTAL,
    @SerializedName("per_projectile")
    PER_PROJECTILE
}
