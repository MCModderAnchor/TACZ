package com.tacz.guns.client.resource.pojo.display.gun;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

public class MuzzleFlash {
    public enum Type {
        @SerializedName("default")
        DEFAULT,
        @SerializedName("renew")
        RENEW
    }

    /** 默认 1p 闪光粒子定义 id。 */
    public static final ResourceLocation DEFAULT_FIRST_PERSON_FLASH = new ResourceLocation("tacz", "muzzle_flash");
    /** 默认 1p 烟雾粒子定义 id。 */
    public static final ResourceLocation DEFAULT_FIRST_PERSON_SMOKE = new ResourceLocation("tacz", "muzzle_flash_smoke");
    /** 默认 3p 闪光粒子定义 id。 */
    public static final ResourceLocation DEFAULT_THIRD_PERSON_FLASH = new ResourceLocation("tacz", "muzzle_flash");
    /** 默认 3p 烟雾粒子定义 id。 */
    public static final ResourceLocation DEFAULT_THIRD_PERSON_SMOKE = new ResourceLocation("tacz", "muzzle_flash_smoke");

    /**
     * 枪口效果类型：default 走旧的四边形火光渲染；renew 使用第一人称 snowstorm 粒子系统。
     */
    @SerializedName("type")
    private Type type = Type.DEFAULT;

    @SerializedName("texture")
    protected ResourceLocation texture = null;

    @SerializedName("scale")
    private float scale = 1;

    /**
     * 第一人称火光粒子定义 id（对应 assets/&lt;namespace&gt;/particle_definitions/ 下的定义）。
     * 缺省为 {@link #DEFAULT_FIRST_PERSON_FLASH}；显式写 {@code null} 表示第一人称不生成火光。
     */
    @SerializedName("first_person_flash")
    private ResourceLocation firstPersonFlash = DEFAULT_FIRST_PERSON_FLASH;

    /**
     * 第一人称烟雾粒子定义 id。缺省为 {@link #DEFAULT_FIRST_PERSON_SMOKE}；
     * 显式写 {@code null} 表示第一人称不生成烟雾。
     */
    @SerializedName("first_person_smoke")
    private ResourceLocation firstPersonSmoke = DEFAULT_FIRST_PERSON_SMOKE;

    /**
     * 第三人称火光粒子定义 id。缺省为 {@link #DEFAULT_THIRD_PERSON_FLASH}；
     * 显式写 {@code null} 表示第三人称不生成火光。
     */
    @SerializedName("third_person_flash")
    private ResourceLocation thirdPersonFlash = DEFAULT_THIRD_PERSON_FLASH;

    /**
     * 第三人称烟雾粒子定义 id。缺省为 {@link #DEFAULT_THIRD_PERSON_SMOKE}；
     * 显式写 {@code null} 表示第三人称不生成烟雾。
     */
    @SerializedName("third_person_smoke")
    private ResourceLocation thirdPersonSmoke = DEFAULT_THIRD_PERSON_SMOKE;

    /**
     * 粒子初速度倍率，以 molang 变量 {@code v.muzzle_speed}（枪焰）/ {@code v.smoke_vel}（枪烟）
     * 传入粒子定义，缺省 1.0。
     */
    @SerializedName("initial_speed_scale")
    private float initialSpeedScale = 1.0f;

    /**
     * 烟雾粒子数量，以 molang 变量 {@code v.muzzle_particle_count}（枪焰）/
     * {@code v.smoke_count}（枪烟）传入粒子定义。缺省 0 表示使用粒子定义内置默认值（15）。
     */
    @SerializedName("smoke_particle_count")
    private float smokeParticleCount = 0f;

    /**
     * 枪烟尺寸倍率，以 molang 变量 {@code v.smoke_size} 传入枪烟粒子定义，与 {@link #scale} 完全解耦。
     * 缺省 0 表示使用粒子定义内置默认大小（等价于 1.0），不跟随枪焰的 {@code scale}。
     */
    @SerializedName("smoke_scale")
    private float smokeScale = 0f;

    public Type getType() {
        return type == null ? Type.DEFAULT : type;
    }

    public boolean isRenew() {
        return getType() == Type.RENEW;
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    public float getScale() {
        return scale;
    }

    /**
     * @return 第一人称火光粒子 id；{@code null} 表示显式配置为不生成
     */
    @Nullable
    public ResourceLocation getFirstPersonFlashId() {
        return firstPersonFlash;
    }

    /**
     * @return 第一人称烟雾粒子 id；{@code null} 表示显式配置为不生成
     */
    @Nullable
    public ResourceLocation getFirstPersonSmokeId() {
        return firstPersonSmoke;
    }

    /**
     * @return 第三人称火光粒子 id；{@code null} 表示显式配置为不生成
     */
    @Nullable
    public ResourceLocation getThirdPersonFlashId() {
        return thirdPersonFlash;
    }

    /**
     * @return 第三人称烟雾粒子 id；{@code null} 表示显式配置为不生成
     */
    @Nullable
    public ResourceLocation getThirdPersonSmokeId() {
        return thirdPersonSmoke;
    }

    public float getInitialSpeedScale() {
        return Float.isFinite(initialSpeedScale) ? initialSpeedScale : 1.0f;
    }

    /**
     * @return 烟雾粒子数量；0 表示使用粒子定义内置默认值
     */
    public float getSmokeParticleCount() {
        return Float.isFinite(smokeParticleCount) ? smokeParticleCount : 0f;
    }

    /**
     * @return 枪烟尺寸倍率；0 表示使用粒子定义内置默认大小
     */
    public float getSmokeScale() {
        return Float.isFinite(smokeScale) && smokeScale > 0f ? smokeScale : 0f;
    }
}
