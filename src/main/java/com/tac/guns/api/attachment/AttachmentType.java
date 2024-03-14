package com.tac.guns.api.attachment;

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
    LASER,
    /**
     * 用来表示物品不是配件的情况。
     * 它必须在最后声明，请不要挪动它的声明顺序！
     */
    NONE
}
