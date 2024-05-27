package com.tacz.guns.resource.pojo.data.attachment;

import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;

public class AttachmentData {
    @SerializedName("silence")
    @Nullable
    private Silence silence;

    @SerializedName("weight")
    private float weight = 0;

    @SerializedName("ads_addend")
    private float adsAddendTime = 0;

    @SerializedName("extended_mag_level")
    private int extendedMagLevel = 0;

    @SerializedName("inaccuracy_addend")
    private float inaccuracyAddend = 0;

    @SerializedName("recoil_modifier")
    @Nullable
    private RecoilModifier recoilModifier = null;

    @Nullable
    public Silence getSilence() {
        return silence;
    }

    public float getWeight() {
        return weight;
    }

    public int getExtendedMagLevel() {
        return extendedMagLevel;
    }

    public float getAdsAddendTime() {
        return adsAddendTime;
    }

    public float getInaccuracyAddend() {
        return inaccuracyAddend;
    }

    @Nullable
    public RecoilModifier getRecoilModifier() {
        return recoilModifier;
    }
}
