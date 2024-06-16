package com.tacz.guns.resource.pojo.data.attachment;

import com.google.gson.annotations.SerializedName;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.compress.utils.Lists;

import java.util.List;

public class MeleeData {
    @SerializedName("range")
    private Vec3 range = Vec3.ZERO;

    @SerializedName("damage")
    private float damage = 0f;

    @SerializedName("knockback")
    private float knockback = 0f;

    @SerializedName("delay_damage_time")
    private float delayDamageTime = 0.1f;

    @SerializedName("effects")
    private List<EffectData> effects = Lists.newArrayList();

    public Vec3 getRange() {
        return range;
    }

    public float getDamage() {
        return damage;
    }

    public float getKnockback() {
        return knockback;
    }

    public float getDelayDamageTime() {
        return delayDamageTime;
    }

    public List<EffectData> getEffects() {
        return effects;
    }
}
