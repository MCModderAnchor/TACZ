package com.tac.guns.client.resource.pojo.display.ammo;

import com.google.gson.annotations.SerializedName;
import com.mojang.math.Vector3f;
import net.minecraft.core.particles.ParticleOptions;

import javax.annotation.Nullable;

public class AmmoParticle {
    @SerializedName("name")
    private String name;

    @SerializedName("delta")
    private Vector3f delta = Vector3f.ZERO;

    @SerializedName("speed")
    private float speed = 0f;

    @SerializedName("life_time")
    private int lifeTime = 20;

    @SerializedName("count")
    private int count = 1;

    // 不进行序列化，而是需要 deco 的
    private transient ParticleOptions particleOptions;

    public String getName() {
        return name;
    }

    public Vector3f getDelta() {
        return delta;
    }

    public float getSpeed() {
        return speed;
    }

    public int getCount() {
        return count;
    }

    public int getLifeTime() {
        return lifeTime;
    }

    public void setParticleOptions(ParticleOptions particleOptions) {
        this.particleOptions = particleOptions;
    }

    @Nullable
    public ParticleOptions getParticleOptions() {
        return particleOptions;
    }
}
