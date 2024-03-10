package com.tac.guns.resource.pojo.data.attachment;

import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;

public class AttachmentData {
    private static final float SILENCE_DEFAULT_VALUE = -1;
    @SerializedName("weight")
    private float weight;
    @SerializedName("ads_addend")
    private float adsAddend;
    @SerializedName("recoil_modifier")
    private RecoilModifier recoilModifier;
    @SerializedName("silence")
    private float silence = SILENCE_DEFAULT_VALUE;
    @SerializedName("zoom")
    @Nullable
    private float[] zoom;
    @SerializedName("fov")
    private float fov = 70;

    public boolean isSilencer(){
        return silence == SILENCE_DEFAULT_VALUE;
    }

    public float getWeight() {
        return weight;
    }

    public float getAdsAddend() {
        return adsAddend;
    }

    public RecoilModifier getRecoilModifier() {
        return recoilModifier;
    }

    public float getSilence() {
        return silence;
    }

    @Nullable
    public float[] getZoom() {
        return zoom;
    }

    public float getFov() {
        return fov;
    }
}
