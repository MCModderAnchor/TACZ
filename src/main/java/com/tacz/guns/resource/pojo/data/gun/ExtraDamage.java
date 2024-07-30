package com.tacz.guns.resource.pojo.data.gun;

import com.google.common.collect.Lists;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;

public class ExtraDamage {
    @SerializedName("armor_ignore")
    private float armorIgnore = 0f;

    @SerializedName("head_shot_multiplier")
    private float headShotMultiplier = 1f;

    @SerializedName("damage_adjust")
    private LinkedList<DistanceDamagePair> damageAdjust = Lists.newLinkedList();

    public float getArmorIgnore() {
        return armorIgnore;
    }

    public float getHeadShotMultiplier() {
        return headShotMultiplier;
    }

    @Nullable
    public LinkedList<DistanceDamagePair> getDamageAdjust() {
        return damageAdjust.isEmpty() ? null : damageAdjust;
    }

    public static class DistanceDamagePair {
        @SerializedName("distance")
        private float distance;

        @SerializedName("damage")
        private float damage;

        public DistanceDamagePair(float distance, float damage) {
            this.distance = distance;
            this.damage = damage;
        }

        public float getDistance() {
            return distance;
        }

        public float getDamage() {
            return damage;
        }
    }
}
