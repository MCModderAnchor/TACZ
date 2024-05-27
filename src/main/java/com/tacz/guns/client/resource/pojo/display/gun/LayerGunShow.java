package com.tacz.guns.client.resource.pojo.display.gun;

import com.google.gson.annotations.SerializedName;
import org.joml.Vector3f;

public class LayerGunShow {
    @SerializedName("pos")
    private Vector3f pos = new Vector3f(-2, 20, 4);

    @SerializedName("rotate")
    private Vector3f rotate = new Vector3f(0, 0, -30);

    @SerializedName("scale")
    private Vector3f scale = new Vector3f(0.6f, 0.6f, 0.6f);

    public Vector3f getPos() {
        return pos;
    }

    public Vector3f getRotate() {
        return rotate;
    }

    public Vector3f getScale() {
        return scale;
    }
}
