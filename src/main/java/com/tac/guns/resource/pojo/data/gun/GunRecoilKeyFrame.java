package com.tac.guns.resource.pojo.data.gun;

import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

public class GunRecoilKeyFrame implements Comparable<GunRecoilKeyFrame>{
    @SerializedName("time")
    private float time;
    @SerializedName("value")
    private float[] value = new float[]{0, 0};

    public float getTime() {
        return time;
    }

    public void setTime(float time) {
        this.time = time;
    }

    public float[] getValue() {
        return value;
    }

    public void setValue(float[] value) {
        this.value = value;
    }

    @Override
    public int compareTo(@NotNull GunRecoilKeyFrame o) {
        return Double.compare(this.time, o.time);
    }
}
