package com.tacz.guns.client.resource.pojo;

import com.google.gson.annotations.SerializedName;
import org.joml.Vector3f;

import javax.annotation.Nullable;

public class TransformScale {
    @SerializedName("thirdperson")
    @Nullable
    private Vector3f thirdPerson;
    @SerializedName("ground")
    @Nullable
    private Vector3f ground;
    @SerializedName("fixed")
    @Nullable
    private Vector3f fixed;

    public static TransformScale getAmmoDefault() {
        TransformScale transformScale = new TransformScale();
        transformScale.thirdPerson = new Vector3f(0.75f, 0.75f, 0.75f);
        transformScale.ground = new Vector3f(0.75f, 0.75f, 0.75f);
        transformScale.fixed = new Vector3f(1.5f, 1.5f, 1.5f);
        return transformScale;
    }

    public static TransformScale getGunDefault() {
        TransformScale transformScale = new TransformScale();
        transformScale.thirdPerson = new Vector3f(0.6f, 0.6f, 0.6f);
        transformScale.ground = new Vector3f(0.6f, 0.6f, 0.6f);
        transformScale.fixed = new Vector3f(1.2f, 1.2f, 1.2f);
        return transformScale;
    }

    @Nullable
    public Vector3f getThirdPerson() {
        return thirdPerson;
    }

    @Nullable
    public Vector3f getGround() {
        return ground;
    }

    @Nullable
    public Vector3f getFixed() {
        return fixed;
    }
}
