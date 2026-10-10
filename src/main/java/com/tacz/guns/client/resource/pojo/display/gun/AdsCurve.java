package com.tacz.guns.client.resource.pojo.display.gun;

import com.google.gson.annotations.SerializedName;
import com.tacz.guns.util.math.Easing;
import net.minecraft.util.Mth;

/** 开镜进度中的一个运动窗口和缓动曲线。 */
public class AdsCurve {
    private float start = 0;
    private float end = 1;
    private Ease ease = Ease.LINEAR;
    private boolean clamp = true;

    public boolean isValid() {
        return Float.isFinite(start) && Float.isFinite(end)
                && start >= 0 && end <= 1 && start < end && ease != null;
    }

    public float evaluate(float progress) {
        float t = (progress - start) / (end - start);
        if (clamp) {
            t = Mth.clamp(t, 0, 1);
        }
        return switch (ease) {
            case LINEAR -> t;
            case EASE_IN_QUAD -> t * t;
            case EASE_OUT_QUAD -> 1 - (1 - t) * (1 - t);
            case EASE_IN_OUT_QUAD -> t < 0.5f ? 2 * t * t : 1 - (float) Math.pow(-2 * t + 2, 2) / 2;
            case EASE_IN_CUBIC -> t * t * t;
            case EASE_OUT_CUBIC -> 1 - (1 - t) * (1 - t) * (1 - t);
            case EASE_IN_OUT_CUBIC -> t < 0.5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
            case EASE_IN_QUART -> (float) Math.pow(t, 4);
            case EASE_OUT_QUART -> 1 - (float) Math.pow(1 - t, 4);
            case EASE_IN_OUT_QUART -> t < 0.5f ? 8 * (float) Math.pow(t, 4) : 1 - (float) Math.pow(-2 * t + 2, 4) / 2;
            case EASE_IN_QUINT -> (float) Math.pow(t, 5);
            case EASE_OUT_QUINT -> 1 - (float) Math.pow(1 - t, 5);
            case EASE_IN_OUT_QUINT -> t < 0.5f ? 16 * (float) Math.pow(t, 5) : 1 - (float) Math.pow(-2 * t + 2, 5) / 2;
            case EASE_IN_SINE -> (float) (1 - Math.cos(t * Math.PI / 2));
            case EASE_IN_EXPO -> (float) Easing.easeInExpo(t);
            case EASE_OUT_EXPO -> t <= 0 ? 0 : t >= 1 ? 1 : (float) (1 - Math.pow(2, -10.0 * t));
            case EASE_IN_OUT_EXPO -> t <= 0 ? 0 : t >= 1 ? 1 : t < 0.5f
                    ? (float) Math.pow(2, 20.0 * t - 10) / 2
                    : (float) (2 - Math.pow(2, -20.0 * t + 10)) / 2;
            case EASE_OUT_SINE -> (float) Math.sin(t * Math.PI / 2);
            case EASE_IN_OUT_SINE -> (float) ((1 - Math.cos(t * Math.PI)) / 2);
            // 圆弧曲线在单位区间外没有实数定义，始终限制其输入。
            case EASE_IN_CIRC -> 1 - (float) Math.sqrt(1 - Math.pow(Mth.clamp(t, 0, 1), 2));
            case EASE_OUT_CIRC -> (float) Math.sqrt(1 - Math.pow(Mth.clamp(t, 0, 1) - 1, 2));
            case EASE_IN_OUT_CIRC -> {
                float u = Mth.clamp(t, 0, 1);
                yield u < 0.5f ? (1 - (float) Math.sqrt(1 - Math.pow(2 * u, 2))) / 2
                        : ((float) Math.sqrt(1 - Math.pow(-2 * u + 2, 2)) + 1) / 2;
            }
            case EASE_IN_BACK -> (float) (2.70158 * t * t * t - 1.70158 * t * t);
            case EASE_OUT_BACK -> (float) (1 + 2.70158 * Math.pow(t - 1, 3) + 1.70158 * Math.pow(t - 1, 2));
            case EASE_IN_OUT_BACK -> t < 0.5f
                    ? (float) (Math.pow(2 * t, 2) * ((1.70158 * 1.525 + 1) * 2 * t - 1.70158 * 1.525) / 2)
                    : (float) (Math.pow(2 * t - 2, 2) * ((1.70158 * 1.525 + 1) * (2 * t - 2) + 1.70158 * 1.525) / 2 + 1);
            case SMOOTHSTEP -> t * t * (3 - 2 * t);
        };
    }

    private enum Ease {
        @SerializedName("linear") LINEAR,
        @SerializedName(value = "ease_in_sine", alternate = "easeInSine") EASE_IN_SINE,
        @SerializedName(value = "ease_out_sine", alternate = "easeOutSine") EASE_OUT_SINE,
        @SerializedName(value = "ease_in_out_sine", alternate = "easeInOutSine") EASE_IN_OUT_SINE,
        @SerializedName(value = "ease_in_quad", alternate = "easeInQuad") EASE_IN_QUAD,
        @SerializedName(value = "ease_out_quad", alternate = "easeOutQuad") EASE_OUT_QUAD,
        @SerializedName(value = "ease_in_out_quad", alternate = "easeInOutQuad") EASE_IN_OUT_QUAD,
        @SerializedName(value = "ease_in_cubic", alternate = "easeInCubic") EASE_IN_CUBIC,
        @SerializedName(value = "ease_out_cubic", alternate = "easeOutCubic") EASE_OUT_CUBIC,
        @SerializedName(value = "ease_in_out_cubic", alternate = "easeInOutCubic") EASE_IN_OUT_CUBIC,
        @SerializedName(value = "ease_in_quart", alternate = "easeInQuart") EASE_IN_QUART,
        @SerializedName(value = "ease_out_quart", alternate = "easeOutQuart") EASE_OUT_QUART,
        @SerializedName(value = "ease_in_out_quart", alternate = "easeInOutQuart") EASE_IN_OUT_QUART,
        @SerializedName(value = "ease_in_quint", alternate = "easeInQuint") EASE_IN_QUINT,
        @SerializedName(value = "ease_out_quint", alternate = "easeOutQuint") EASE_OUT_QUINT,
        @SerializedName(value = "ease_in_out_quint", alternate = "easeInOutQuint") EASE_IN_OUT_QUINT,
        @SerializedName(value = "ease_in_expo", alternate = "easeInExpo") EASE_IN_EXPO,
        @SerializedName(value = "ease_out_expo", alternate = "easeOutExpo") EASE_OUT_EXPO,
        @SerializedName(value = "ease_in_out_expo", alternate = "easeInOutExpo") EASE_IN_OUT_EXPO,
        @SerializedName(value = "ease_in_circ", alternate = "easeInCirc") EASE_IN_CIRC,
        @SerializedName(value = "ease_out_circ", alternate = "easeOutCirc") EASE_OUT_CIRC,
        @SerializedName(value = "ease_in_out_circ", alternate = "easeInOutCirc") EASE_IN_OUT_CIRC,
        @SerializedName(value = "ease_in_back", alternate = "easeInBack") EASE_IN_BACK,
        @SerializedName(value = "ease_out_back", alternate = "easeOutBack") EASE_OUT_BACK,
        @SerializedName(value = "ease_in_out_back", alternate = "easeInOutBack") EASE_IN_OUT_BACK,
        @SerializedName("smoothstep") SMOOTHSTEP
    }
}
