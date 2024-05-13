package com.tacz.guns.client.resource.pojo.display.gun;

import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.StringUtils;

public class TextShow {
    @SerializedName("scale")
    private float scale = 1.0f;

    @SerializedName("align")
    private Align align = Align.CENTER;

    @SerializedName("shadow")
    private boolean shadow = false;

    @SerializedName("color")
    private String colorText = "#FFFFFF";

    @SerializedName("light")
    private int textLight = 15;

    @SerializedName("text")
    private String textKey = StringUtils.EMPTY;

    private volatile int colorInt = 0xFFFFFF;

    public float getScale() {
        return scale;
    }

    public Align getAlign() {
        return align;
    }

    public boolean isShadow() {
        return shadow;
    }

    public String getTextKey() {
        return textKey;
    }

    public String getColorText() {
        return colorText;
    }

    public int getTextLight() {
        return textLight;
    }

    public int getColorInt() {
        return colorInt;
    }

    public void setColorInt(int colorInt) {
        this.colorInt = colorInt;
    }
}
