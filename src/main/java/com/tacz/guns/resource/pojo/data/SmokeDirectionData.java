package com.tacz.guns.resource.pojo.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import javax.annotation.Nullable;

/**
 * 枪口烟雾方向的共用解析。配件 data 与枪械 display 都用它读取
 * {@code smoke_direction} / {@code smoke_spread}，保证两边的写法与容错行为一致。
 */
public final class SmokeDirectionData {
    /**
     * {@code smoke_direction} 支持的最大方向数，超出部分会被截断。
     * <p>
     * molang 的变量通道是标量（{@code ParticleEmitterInstance#setVariable(String, float)}），
     * 整组向量没法直接交给粒子定义，多方向只能平铺成固定组数的标量变量、再由表达式挑选，
     * 所以需要一个硬上限。
     */
    public static final int MAX_DIRECTIONS = 4;

    /**
     * {@code smoke_direction} 缺省散布量。主方向为单位向量，故该值近似等于
     * 相对主方向的横向偏移量，约对应 ±19° 的锥角。
     */
    public static final float DEFAULT_SPREAD = 0.35f;
    public static final float DEFAULT_SPREAD_ANGLE = 20.0f;

    public record Config(float[][] directions, float spread, boolean angleMode) {
    }

    private SmokeDirectionData() {
    }

    @Nullable
    public static Config parse(@Nullable JsonElement angles, @Nullable Float spreadAngle,
                               @Nullable JsonElement vectors, @Nullable Float spread) {
        float[][] angleDirections = parseAngles(angles);
        if (angleDirections != null) {
            return new Config(angleDirections, parseSpreadAngle(spreadAngle), true);
        }
        float[][] legacyDirections = parseDirections(vectors);
        return legacyDirections == null ? null : new Config(legacyDirections, parseSpread(spread), false);
    }

    @Nullable
    private static float[][] parseAngles(@Nullable JsonElement element) {
        if (element == null || (!element.isJsonArray() && !element.isJsonObject())) {
            return null;
        }
        JsonArray array;
        if (element.isJsonArray()) {
            array = element.getAsJsonArray();
        } else {
            array = new JsonArray();
            array.add(element);
        }
        if (array.size() == 0) {
            return null;
        }
        int count = Math.min(array.size(), MAX_DIRECTIONS);
        float[][] result = new float[count][];
        for (int i = 0; i < count; i++) {
            JsonElement direction = array.get(i);
            if (!direction.isJsonObject()) {
                return null;
            }
            JsonElement plane = direction.getAsJsonObject().get("plane_angle");
            JsonElement out = direction.getAsJsonObject().get("out_angle");
            if (!isFiniteNumber(plane) || !isFiniteNumber(out)) {
                return null;
            }
            float planeDegrees = plane.getAsFloat();
            float outDegrees = out.getAsFloat();
            if (planeDegrees < -180 || planeDegrees > 180 || outDegrees < -90 || outDegrees > 90) {
                return null;
            }
            double planeRadians = Math.toRadians(planeDegrees);
            double outRadians = Math.toRadians(outDegrees);
            result[i] = new float[]{(float) (Math.cos(outRadians) * Math.cos(planeRadians)),
                    (float) (Math.cos(outRadians) * Math.sin(planeRadians)),
                    (float) -Math.sin(outRadians)};
        }
        return result;
    }

    private static boolean isFiniteNumber(@Nullable JsonElement value) {
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()
                && Float.isFinite(value.getAsFloat());
    }

    public static float parseSpreadAngle(@Nullable Float angle) {
        return angle != null && Float.isFinite(angle) && angle >= 0 && angle <= 180
                ? angle : DEFAULT_SPREAD_ANGLE;
    }

    /**
     * 解析归一化后的方向列表；未配置或配置非法时返回 {@code null}（不干预）。
     * <p>
     * 单方向写作 {@code [x, y, z]}，多方向写作 {@code [[x, y, z], ...]}，
     * 两者都归一化成列表返回（单方向即为长度为 1 的列表），长度不超过
     * {@link #MAX_DIRECTIONS}。
     * <p>
     * 归一化在这里完成，是为了让 {@code smoke_spread} 的语义与方向向量的书写长度无关——
     * 否则写成 {@code [0, 0.1, 0]} 时，缺省散布量会直接淹没主方向。
     * <p>
     * 只要有一个方向非法（不是三元数组、含非数值、零向量）就整体返回 {@code null}，
     * 而不是跳过那一个：半生效的配置比明确的失效更难排查。
     */
    @Nullable
    public static float[][] parseDirections(@Nullable JsonElement element) {
        if (element == null || !element.isJsonArray()) {
            return null;
        }
        JsonArray array = element.getAsJsonArray();
        if (array.size() == 0) {
            return null;
        }
        // 首元素是数组 → 多方向写法；否则按单方向处理
        boolean multiple = array.get(0) != null && array.get(0).isJsonArray();
        int count = multiple ? Math.min(array.size(), MAX_DIRECTIONS) : 1;
        float[][] result = new float[count][];
        for (int i = 0; i < count; i++) {
            float[] dir = parseVector(multiple ? array.get(i) : array);
            if (dir == null) {
                return null;
            }
            result[i] = dir;
        }
        return result;
    }

    /**
     * 解析散布量；未配置或非法时用 {@link #DEFAULT_SPREAD}。
     */
    public static float parseSpread(@Nullable Float spread) {
        return spread != null && Float.isFinite(spread) && spread >= 0.0f ? spread : DEFAULT_SPREAD;
    }

    /**
     * 解析并归一化单个方向向量；格式非法或为零向量时返回 {@code null}。
     */
    @Nullable
    private static float[] parseVector(@Nullable JsonElement element) {
        if (element == null || !element.isJsonArray()) {
            return null;
        }
        JsonArray array = element.getAsJsonArray();
        if (array.size() != 3) {
            return null;
        }
        float[] dir = new float[3];
        for (int i = 0; i < 3; i++) {
            JsonElement component = array.get(i);
            if (component == null || !component.isJsonPrimitive() || !component.getAsJsonPrimitive().isNumber()) {
                return null;
            }
            dir[i] = component.getAsFloat();
            if (!Float.isFinite(dir[i])) {
                return null;
            }
        }
        float length = (float) Math.sqrt(dir[0] * dir[0] + dir[1] * dir[1] + dir[2] * dir[2]);
        if (length <= 1.0e-6f) {
            // 零向量：交给粒子定义的均匀四散，等价于不配置
            return null;
        }
        return new float[]{dir[0] / length, dir[1] / length, dir[2] / length};
    }
}
