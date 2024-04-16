package com.tac.guns.resource.pojo.data.attachment;

import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;

public class AttachmentData {
    @SerializedName("silence")
    private Silence silence = new Silence();

    @SerializedName("weight")
    private float weight;

    @SerializedName("ads_addend")
    private float adsAddendTime;

    @SerializedName("inaccuracy_addend")
    private float inaccuracyAddend;

    @SerializedName("recoil_modifier")
    @Nullable
    private RecoilModifier recoilModifier;

    public Silence getSilence() {
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
