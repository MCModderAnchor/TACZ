package com.tac.guns.api.gun;

import com.google.gson.annotations.SerializedName;

public enum FireMode {
    /**
     * 全自动
     */
    @SerializedName("auto")
    AUTO,
    /**
     * 半自动
     */
    @SerializedName("semi")
    SEMI,
    /**
     * 多连发
     */
    @SerializedName("burst")
    BURST
}
