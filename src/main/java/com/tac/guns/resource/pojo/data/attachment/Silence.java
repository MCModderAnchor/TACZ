package com.tac.guns.resource.pojo.data.attachment;

import com.google.gson.annotations.SerializedName;

public class Silence {
    @SerializedName("distance_addend")
    private int distanceAddend = 0;

    @SerializedName("use_silence_sound")
    private boolean useSilenceSound = false;

    public int getDistanceAddend() {
        return distanceAddend;
    }

    public boolean isUseSilenceSound() {
        return useSilenceSound;
    }
}
