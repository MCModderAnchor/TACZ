package com.tac.guns.resource.pojo.data.attachment;

import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;

public class AttachmentData {
    private static final float SILENCE_DEFAULT_VALUE = -1;
    @SerializedName("weight")
    private float weight;
    @SerializedName("ads_addend")
    private float adsAddendTime;
    @SerializedName("recoil_modifier")
    @Nullable
    private RecoilModifier recoilModifier;
    @SerializedName("silence")
    private float silence = SILENCE_DEFAULT_VALUE;

    public boolean isSilencer() {
        return silence == SILENCE_DEFAULT_VALUE;
    }

    public float getWeight() {
        return weight;
    }

    public float getAdsAddendTime() {
        return adsAddendTime;
    }

    @Nullable
    public RecoilModifier getRecoilModifier() {
        return recoilModifier;
    }

    public float getSilence() {
        return silence;
    }
}
