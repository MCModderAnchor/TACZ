package com.tac.guns.resource.pojo.data.attachment;

import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;

public class AttachmentData {
    private static final float SILENCE_DEFAULT_VALUE = -1;

    @SerializedName("silence")
    private float silence = SILENCE_DEFAULT_VALUE;

    @SerializedName("weight")
    private float weight;

    @SerializedName("ads_addend")
    private float adsAddendTime;

    @SerializedName("inaccuracy_addend")
    private float inaccuracyAddend;

    @SerializedName("recoil_modifier")
    @Nullable
    private RecoilModifier recoilModifier;

    public boolean isSilencer() {
        return silence == SILENCE_DEFAULT_VALUE;
    }

    public float getSilence() {
        return silence;
    }

    public float getWeight() {
        return weight;
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
