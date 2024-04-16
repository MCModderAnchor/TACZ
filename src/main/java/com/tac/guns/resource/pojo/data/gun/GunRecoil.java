package com.tac.guns.resource.pojo.data.gun;

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

    public float getRandomPitch(float modifier) {
        float left = modifierNumber(pitch.getLeft(), modifier);
        float right = modifierNumber(pitch.getRight(), modifier);
        return (float) (left + Math.random() * (right - left));
    }

    public float getRandomYaw(float modifier) {
        float left = modifierNumber(yaw.getLeft(), modifier);
        float right = modifierNumber(yaw.getRight(), modifier);
        return (float) (left + Math.random() * (right - left));
    }

    private float modifierNumber(float number, float modifier) {
        if (modifier == 0) {
            return number;
        }
        return number * Math.max(0, 1 + modifier);
    }
}
