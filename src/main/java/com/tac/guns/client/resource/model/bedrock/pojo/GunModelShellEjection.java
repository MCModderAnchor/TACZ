package com.tac.guns.client.resource.model.bedrock.pojo;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class GunModelShellEjection {
    @SerializedName("type")
    private ResourceLocation type;

    @SerializedName("velocity")
    private List<Float> velocity;

    @SerializedName("random_velocity")
    private List<Float> randomVelocity;

    @SerializedName("angular_velocity")
    private List<Float> angularVelocity;

    @SerializedName("living_time")
    private Float livingTimeS;

    public ResourceLocation getType() {
        return type;
    }

    public List<Float> getVelocity() {
        return velocity;
    }

    public List<Float> getRandomVelocity() {
        return randomVelocity;
    }

    public List<Float> getAngularVelocity() {
        return angularVelocity;
    }

    public Float getLivingTimeS() {
        return livingTimeS;
    }
}
