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

    @SerializedName("melee")
    @Nullable
    private MeleeData meleeData = null;

    public float getWeight() {
        return weight;
    }

    public int getExtendedMagLevel() {
        return extendedMagLevel;
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
