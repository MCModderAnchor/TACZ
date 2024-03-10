package com.tac.guns.resource.pojo;

import com.google.gson.annotations.SerializedName;

public enum AttachmentType {
    @SerializedName("scope")
    SCOPE,
    @SerializedName("muzzle")
    MUZZLE,
    @SerializedName("stock")
    STOCK,
    @SerializedName("grip")
    GRIP,
    @SerializedName("laser")
    LASER
}
