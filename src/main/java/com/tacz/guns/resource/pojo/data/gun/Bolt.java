package com.tacz.guns.resource.pojo.data.gun;

import com.google.gson.annotations.SerializedName;

public enum Bolt {
    /**
     * 开膛待击
     */
    @SerializedName("open_bolt")
    OPEN_BOLT,
    /**
     * 闭膛待击
     */
    @SerializedName("closed_bolt")
    CLOSED_BOLT,
    /**
     * 手动上膛
     */
    @SerializedName("manual_action")
    MANUAL_ACTION
}
