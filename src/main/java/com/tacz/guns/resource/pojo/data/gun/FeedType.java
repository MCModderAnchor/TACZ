package com.tacz.guns.resource.pojo.data.gun;

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
    MANUAL,
    /**
     * 燃料供弹(消耗单个物品补满弹药)
     */
    @SerializedName("fuel")
    FUEL
}
