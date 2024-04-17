package com.tac.guns.resource.pojo.data.gun;

import com.google.gson.annotations.SerializedName;

public class ExtraDamage {
    @SerializedName("armor_ignore")
    private float armorIgnore = 0f;

    @SerializedName("decay")
    private Decay decay = new Decay();

    @SerializedName("close")
    private Close close = new Close();

    public float getArmorIgnore() {
        return armorIgnore;
    }

    public Decay getDecay() {
        return decay;
    }

    public Close getClose() {
        return close;
    }

    public static class Decay {
        @SerializedName("range_percent")
        private float[] rangePercent = new float[]{0, 1};

        @SerializedName("min_damage_multiplier")
        private float minDamageMultiplier = 1.0f;

        public float[] getRangePercent() {
            return rangePercent;
        }

        public float getMinDamageMultiplier() {
            return minDamageMultiplier;
        }
    }

    public static class Close {
        @SerializedName("range_meters")
        private float rangeMeters = 0f;

        @SerializedName("damage_multiplier")
        private float damageMultiplier = 1.0f;

        public float getRangeMeters() {
            return rangeMeters;
        }

        public float getDamageMultiplier() {
            return damageMultiplier;
        }
    }
}
