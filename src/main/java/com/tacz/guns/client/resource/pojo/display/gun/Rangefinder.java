package com.tacz.guns.client.resource.pojo.display.gun;

import com.google.gson.annotations.SerializedName;
import com.tacz.guns.util.ColorHex;
import org.apache.commons.lang3.StringUtils;

public class Rangefinder {
    @SerializedName("max_distance")
    private float maxDistance = 512.0f;

    @SerializedName("refresh_tick")
    private int refreshTick = 2;

    @SerializedName("show_impact_prediction")
    private boolean showImpactPrediction = false;

    @SerializedName("impact_prediction_color")
    private String impactPredictionColor = "#FFFFFF";

    @SerializedName("impact_prediction_scale")
    private float impactPredictionScale = 1.0f;

    public float getMaxDistance() {
        return maxDistance < 0 ? 512.0f : maxDistance;
    }

    public int getRefreshTick() {
        return refreshTick <= 0 ? 2 : refreshTick;
    }

    public boolean isShowImpactPrediction() {
        return showImpactPrediction;
    }

    public int getImpactPredictionColor() {
        String colorText = StringUtils.trimToEmpty(impactPredictionColor);
        if (colorText.length() == 6) {
            colorText = "#" + colorText;
        }
        return 0xFF000000 | ColorHex.colorTextToRbgInt(colorText);
    }

    public float getImpactPredictionScale() {
        return impactPredictionScale <= 0.0f ? 1.0f : impactPredictionScale;
    }
}
