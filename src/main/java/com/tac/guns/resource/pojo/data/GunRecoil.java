package com.tac.guns.resource.pojo.data;

import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.tuple.Pair;

public class GunRecoil {
    @SerializedName("pitch")
    private Pair<Float, Float> pitch = Pair.of(0.5f, 0.7f);

    @SerializedName("yaw")
    private Pair<Float, Float> yaw = Pair.of(-0.1f, 0.1f);

    public Pair<Float, Float> getPitch() {
        return pitch;
    }

    public Pair<Float, Float> getYaw() {
        return yaw;
    }

    public float getRandomPitch() {
        return (float) (pitch.getLeft() + Math.random() * (pitch.getRight() - pitch.getLeft()));
    }

    public float getRandomYaw() {
        return (float) (yaw.getLeft() + Math.random() * (yaw.getRight() - yaw.getLeft()));
    }
}
