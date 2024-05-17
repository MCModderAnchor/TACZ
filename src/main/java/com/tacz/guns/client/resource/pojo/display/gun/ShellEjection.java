package com.tacz.guns.client.resource.pojo.display.gun;

import com.google.gson.annotations.SerializedName;
import org.joml.Vector3f;

public class ShellEjection {
    @SerializedName("initial_velocity")
    private Vector3f initialVelocity = new Vector3f(8.0f, 5.0f, -0.5f);

    @SerializedName("random_velocity")
    private Vector3f randomVelocity = new Vector3f(2.5f, 1.5f, 0.25f);

    @SerializedName("acceleration")
    private Vector3f acceleration = new Vector3f(0, -20f, 0);

    @SerializedName("angular_velocity")
    private Vector3f angularVelocity = new Vector3f(-720, -720, 90);

    @SerializedName("living_time")
    private float livingTime = 1.0f;

    public Vector3f getInitialVelocity() {
        return initialVelocity;
    }

    public Vector3f getRandomVelocity() {
        return randomVelocity;
    }

    public Vector3f getAcceleration() {
        return acceleration;
    }

    public Vector3f getAngularVelocity() {
        return angularVelocity;
    }

    public float getLivingTime() {
        return livingTime;
    }
}
