package com.tacz.guns.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ColorHex {
    private static final Pattern COLOR_HEX = Pattern.compile("^#([0-9A-Fa-f]{6})$");

    public static float[] colorTextToRbgFloatArray(String colorText) {
        int colorHex = colorTextToRbgInt(colorText);
        float r = (colorHex >> 16 & 0xff) / 255.0F;
        float g = (colorHex >> 8 & 0xff) / 255.0F;
        float b = (colorHex & 0xff) / 255.0F;
        return new float[]{r, g, b};
    }

    public static int colorTextToRbgInt(String colorText) {
        Matcher matcher = COLOR_HEX.matcher(colorText);
        if (!matcher.find()) {
            return 0xFFFFFF;
        }
        return Integer.parseInt(matcher.group(1), 16);
    }
}
