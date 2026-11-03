package com.tacz.guns.client.resource.pojo.display.ammo;

import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;
import java.util.List;

public class HitSurfaceParticle {
    private static final int MAX_COUNT = 32;
    private static final int MAX_LIFE_TIME = 200;
    private static final float MAX_OFFSET = 1.0f;
    private static final float MAX_SPEED = 2.0f;
    private static final float MAX_SPREAD_ANGLE = 89.0f;

    @SerializedName("name")
    private String name;

    @SerializedName("count")
    private int count = 1;

    @Nullable
    @SerializedName("count_min")
    private Integer countMin;

    @Nullable
    @SerializedName("count_max")
    private Integer countMax;

    @Nullable
    @SerializedName("count_range")
    private List<Integer> countRange;

    @SerializedName("life_time")
    private int lifeTime = 20;

    @Nullable
    @SerializedName("color")
    private String color;

    @Nullable
    @SerializedName("alpha")
    private Float alpha;

    @Nullable
    @SerializedName("scale")
    private Float scale;

    @SerializedName("surface_offset")
    private float surfaceOffset = 0.03f;

    @SerializedName("random_offset")
    private float randomOffset = 0.0f;

    @SerializedName("normal_speed")
    private float normalSpeed = 0.08f;

    @SerializedName("random_speed")
    private float randomSpeed = 0.02f;

    @SerializedName("spread_angle")
    private float spreadAngle = 30.0f;

    public String getName() {
        return name;
    }

    public int getCount() {
        return clamp(count, 0, MAX_COUNT);
    }

    public int getCountMin() {
        if (countRange != null && !countRange.isEmpty() && countRange.get(0) != null) {
            return clamp(countRange.get(0), 0, MAX_COUNT);
        }
        return countMin == null ? getCount() : clamp(countMin, 0, MAX_COUNT);
    }

    public int getCountMax() {
        if (countRange != null && countRange.size() > 1 && countRange.get(1) != null) {
            return clamp(countRange.get(1), 0, MAX_COUNT);
        }
        return countMax == null ? getCount() : clamp(countMax, 0, MAX_COUNT);
    }

    public int getLifeTime() {
        return clamp(lifeTime, 1, MAX_LIFE_TIME);
    }

    @Nullable
    public String getColor() {
        return color;
    }

    @Nullable
    public Float getAlpha() {
        return alpha;
    }

    @Nullable
    public Float getScale() {
        return scale;
    }

    public float getSurfaceOffset() {
        return clamp(surfaceOffset, 0.0f, MAX_OFFSET);
    }

    public float getRandomOffset() {
        return clamp(randomOffset, 0.0f, MAX_OFFSET);
    }

    public float getNormalSpeed() {
        return clamp(normalSpeed, 0.0f, MAX_SPEED);
    }

    public float getRandomSpeed() {
        return clamp(randomSpeed, 0.0f, MAX_SPEED);
    }

    public float getSpreadAngle() {
        return clamp(spreadAngle, 0.0f, MAX_SPREAD_ANGLE);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(value, max));
    }
}
