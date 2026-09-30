package com.tacz.guns.resource.pojo.data.attachment;

import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.tacz.guns.api.modifier.JsonProperty;
import com.tacz.guns.resource.pojo.data.SmokeDirectionData;

import javax.annotation.Nullable;
import java.util.Map;

public class AttachmentData {
    @Expose(serialize = false, deserialize = false)
    private Map<String, JsonProperty<?>> modifier = Maps.newHashMap();

    @SerializedName("weight")
    private float weight = 0;

    @SerializedName("extended_mag_level")
    private int extendedMagLevel = 0;

    /**
     * 枪口火光倍率（renew 粒子模式）：1.0 不变，0.5 缩小 50%，0 完全隐藏火光层。
     * 未填写时保留旧消音器行为：隐藏整组枪口效果。
     */
    @SerializedName("flash_hider")
    @Nullable
    private Float flashHider = null;

    /**
     * 枪口烟雾倍率（renew 粒子模式）：1.0 不变，0 完全隐藏烟雾层。
     */
    @SerializedName("smoke_hider")
    @Nullable
    private Float smokeHider = null;

    /**
     * 枪口烟雾扩散速度倍率（renew 粒子模式）：1.0 不变，1.5 加快 50%。
     * 与枪械 display 的 {@code initial_speed_scale} 相乘——配件是通用件，
     * 只做相对调整，避免覆盖后与其他枪械不兼容。
     */
    @SerializedName("smoke_speed_scale")
    @Nullable
    private Float smokeSpeedScale = null;

    /**
     * 枪口烟雾粒子数量倍率（renew 粒子模式）：1.0 不变，2.0 翻倍。
     * 与枪械 display 的 {@code smoke_particle_count} 相乘，理由同上。
     */
    @SerializedName("smoke_count_scale")
    @Nullable
    private Float smokeCountScale = null;

    /**
     * 枪烟发射方向。两种写法：
     * <ul>
     *   <li>单方向 {@code [x, y, z]}：所有粒子朝同一方向喷</li>
     *   <li>多方向 {@code [[x, y, z], [x, y, z], ...]}：每个粒子随机挑其中一个，
     *       用于表达"制退器朝两侧排烟"这类离散分布</li>
     * </ul>
     * 坐标是直觉空间——{@code [0, 0, -1]} 为枪口正前方、{@code [0, 1, 0]} 为正上方
     * （渲染空间里那个 Z 轴 180° 翻转已在应用时抵消）。不填、格式非法或为零向量时
     * 视为不干预，保持默认的均匀四散。
     * <p>
     * 这里刻意用 {@link JsonElement} 而非 {@code float[]} 接收：字段类型不匹配时
     * Gson 会抛出异常，整个配件 data 解析失败；用 JsonElement 则能在 getter 里
     * 静默降级为"未配置"，写错方向不至于让配件整个失效。
     */
    @SerializedName("smoke_direction")
    @Nullable
    private JsonElement smokeDirection = null;

    /** 角度方向：平面角 0=右、90=上；外倾角为正时朝枪口前方。最多四路。 */
    @SerializedName("smoke_directions")
    @Nullable
    private JsonElement smokeDirections = null;

    /** 每路烟雾的圆锥半角，单位为度。仅与角度方向配合使用。 */
    @SerializedName("smoke_spread_angle")
    @Nullable
    private Float smokeSpreadAngle = null;

    /**
     * 枪烟发射方向的随机散布量：粒子方向在归一化主方向的基础上按 ± 该值随机偏移，
     * 形成锥形喷射。仅在配置了 {@link #smokeDirection} 时生效，不填则用
     * {@link SmokeDirectionData#DEFAULT_SPREAD}。
     */
    @SerializedName("smoke_spread")
    @Nullable
    private Float smokeSpread = null;

    @SerializedName("melee")
    @Nullable
    private MeleeData meleeData = null;

    public float getWeight() {
        return weight;
    }

    public int getExtendedMagLevel() {
        return extendedMagLevel;
    }

    public boolean hasFlashHider() {
        return flashHider != null && Float.isFinite(flashHider) && flashHider >= 0.0f;
    }

    public float getFlashHider() {
        return hasFlashHider() ? flashHider : 1.0f;
    }

    public boolean hasSmokeHider() {
        return smokeHider != null && Float.isFinite(smokeHider) && smokeHider >= 0.0f;
    }

    public float getSmokeHider() {
        return hasSmokeHider() ? smokeHider : 1.0f;
    }

    public boolean hasSmokeSpeedScale() {
        return smokeSpeedScale != null && Float.isFinite(smokeSpeedScale) && smokeSpeedScale >= 0.0f;
    }

    public float getSmokeSpeedScale() {
        return hasSmokeSpeedScale() ? smokeSpeedScale : 1.0f;
    }

    public boolean hasSmokeCountScale() {
        return smokeCountScale != null && Float.isFinite(smokeCountScale) && smokeCountScale >= 0.0f;
    }

    public float getSmokeCountScale() {
        return hasSmokeCountScale() ? smokeCountScale : 1.0f;
    }

    /**
     * @return 归一化后的枪烟发射方向列表；未配置或配置非法时为 {@code null}（不干预）。
     *         写法与容错规则见 {@link SmokeDirectionData#parseDirections}。
     */
    @Nullable
    public float[][] getSmokeDirections() {
        return SmokeDirectionData.parseDirections(smokeDirection);
    }

    /**
     * @return 枪烟发射方向的随机散布量；未配置或非法时用
     *         {@link SmokeDirectionData#DEFAULT_SPREAD}。
     */
    public float getSmokeSpread() {
        return SmokeDirectionData.parseSpread(smokeSpread);
    }

    @Nullable
    public SmokeDirectionData.Config getSmokeDirectionConfig() {
        return SmokeDirectionData.parse(smokeDirections, smokeSpreadAngle, smokeDirection, smokeSpread);
    }

    @Nullable
    public MeleeData getMeleeData() {
        return meleeData;
    }

    public void addModifier(String id, JsonProperty<?> jsonProperty) {
        modifier.put(id, jsonProperty);
    }

    public Map<String, JsonProperty<?>> getModifier() {
        return modifier;
    }
}
