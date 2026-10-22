package com.tacz.guns.client.renderer.scope;

/** Experimental, client-wide presets for the existing scope window. */
public enum ScopeVisionMode {
    NORMAL,
    NIGHT_VISION_GREEN,
    NIGHT_VISION_WHITE,
    THERMAL_WHITE_HOT,
    THERMAL_BLACK_HOT,
    THERMAL_IRONBOW;

    public boolean isThermal() {
        return this == THERMAL_WHITE_HOT || this == THERMAL_BLACK_HOT || this == THERMAL_IRONBOW;
    }

    public String translationKey() {
        return "config.tacz.client.render.scope_vision_mode." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
