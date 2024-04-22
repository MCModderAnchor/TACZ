package com.tac.guns.client.resource.pojo.display.ammo;

import com.google.gson.annotations.SerializedName;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.math.Vector3f;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.core.particles.ParticleOptions;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;

public class AmmoParticle {
    @SerializedName("name")
    private String name;

    @SerializedName("delta")
    private Vector3f delta;

    @SerializedName("speed")
    private float speed;

    @SerializedName("life_time")
    private int lifeTime;

    @SerializedName("count")
    private int count;

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

    public void decoParticleOptions() throws CommandSyntaxException {
        if (StringUtils.isNoneBlank(this.name)) {
            this.particleOptions = ParticleArgument.readParticle(new StringReader(this.name));
        }
    }

    @Nullable
    public ParticleOptions getParticleOptions() {
        return particleOptions;
    }
}
