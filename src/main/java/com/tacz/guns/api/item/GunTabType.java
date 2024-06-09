package com.tacz.guns.api.item;

import com.google.gson.annotations.SerializedName;

public enum GunTabType {
    @SerializedName("pistol")
    PISTOL,

    @SerializedName("sniper")
    SNIPER,

    @SerializedName("rifle")
    RIFLE,

    @SerializedName("shotgun")
    SHOTGUN,

    @SerializedName("smg")
    SMG,

    @SerializedName("rpg")
    RPG,

    @SerializedName("mg")
    MG
}
