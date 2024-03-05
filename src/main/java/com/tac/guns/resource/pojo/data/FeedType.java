package com.tac.guns.resource.pojo.data;

import com.google.gson.annotations.SerializedName;

public enum FeedType {
    /**
     * 弹匣供弹
     */
    @SerializedName("magazine")
    MAGAZINE,
    /**
     * 手动供弹
     */
    @SerializedName("manual")
    MANUAL
}
