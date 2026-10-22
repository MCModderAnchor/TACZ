package com.tacz.guns.client.resource.pojo.display.attachment;

import com.google.gson.annotations.SerializedName;
import com.tacz.guns.client.renderer.scope.ScopeVisionMode;
import com.tacz.guns.util.ColorHex;

/** Per-attachment lens effects. Missing fields preserve the default optical lens. */
public class ScopeLensConfig {
    @SerializedName("distortion")
    private float distortion = 0.05f;

    @SerializedName("aberration")
    private float aberration = 0.007f;

    @SerializedName("edge_width")
    private float edgeWidth = 0.35f;

    @SerializedName("tint")
    private String tint = "#70AEE8";

    @SerializedName("tint_strength")
    private float tintStrength = 0.2f;

    @SerializedName("vision_mode")
    private ScopeVisionMode visionMode = ScopeVisionMode.NORMAL;

    @SerializedName("vision_gain")
    private float visionGain = 3.0f;

    @SerializedName("vision_noise")
    private float visionNoise = 0.025f;

    private transient int tintColor = 0x70AEE8;

    public void init() {
        distortion = clamp(distortion, 0.05f, 0, 0.2f);
        aberration = clamp(aberration, 0.007f, 0, 0.03f);
        edgeWidth = clamp(edgeWidth, 0.35f, 0, 2);
        tintColor = ColorHex.colorTextToRbgInt(tint == null ? "#70AEE8" : tint);
        tintStrength = clamp(tintStrength, 0.2f, 0, 1);
        visionGain = clamp(visionGain, 3.0f, 1, 12);
        visionNoise = clamp(visionNoise, 0.025f, 0, 0.2f);
        if (visionMode == null) visionMode = ScopeVisionMode.NORMAL;
    }

    private static float clamp(float value, float fallback, float min, float max) {
        return Float.isFinite(value) ? Math.max(min, Math.min(max, value)) : fallback;
    }

    public float getDistortion() {
        return distortion;
    }

    public float getAberration() {
        return aberration;
    }

    public float getEdgeWidth() {
        return edgeWidth;
    }

    public int getTint() {
        return tintColor;
    }

    public float getTintStrength() {
        return tintStrength;
    }

    public ScopeVisionMode getVisionMode() {
        return visionMode;
    }

    public float getVisionGain() {
        return visionGain;
    }

    public float getVisionNoise() {
        return visionNoise;
    }
}
