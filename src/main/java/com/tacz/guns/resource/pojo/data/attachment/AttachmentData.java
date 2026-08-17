package com.tacz.guns.resource.pojo.data.attachment;

import com.google.common.collect.Maps;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.tacz.guns.api.modifier.JsonProperty;

import javax.annotation.Nullable;
import java.util.Map;

public class AttachmentData {
    @Expose(serialize = false, deserialize = false)
    private Map<String, JsonProperty<?>> modifier = Maps.newHashMap();

    @SerializedName("weight")
    private float weight = 0;

    @SerializedName("extended_mag_level")
    private int extendedMagLevel = 0;

    /**
     * 枪口火光倍率（renew 粒子模式）：1.0 不变，0.5 缩小 50%，0 完全隐藏火光层。
     * 未填写时保留旧消音器行为：隐藏整组枪口效果。
     */
    @SerializedName("flash_hider")
    @Nullable
    private Float flashHider = null;

    /**
     * 枪口烟雾倍率（renew 粒子模式）：1.0 不变，0 完全隐藏烟雾层。
     */
    @SerializedName("smoke_hider")
    @Nullable
    private Float smokeHider = null;

    @SerializedName("melee")
    @Nullable
    private MeleeData meleeData = null;

    public float getWeight() {
        return weight;
    }

    public int getExtendedMagLevel() {
        return extendedMagLevel;
    }

    public boolean hasFlashHider() {
        return flashHider != null && Float.isFinite(flashHider) && flashHider >= 0.0f;
    }

    public float getFlashHider() {
        return hasFlashHider() ? flashHider : 1.0f;
    }

    public boolean hasSmokeHider() {
        return smokeHider != null && Float.isFinite(smokeHider) && smokeHider >= 0.0f;
    }

    public float getSmokeHider() {
        return hasSmokeHider() ? smokeHider : 1.0f;
    }

    @Nullable
    public MeleeData getMeleeData() {
        return meleeData;
    }

    public void addModifier(String id, JsonProperty<?> jsonProperty) {
        modifier.put(id, jsonProperty);
    }

    public Map<String, JsonProperty<?>> getModifier() {
        return modifier;
    }
}
