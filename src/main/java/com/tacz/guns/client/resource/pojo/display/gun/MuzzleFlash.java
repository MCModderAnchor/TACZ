package com.tacz.guns.client.resource.pojo.display.gun;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import com.tacz.guns.resource.pojo.data.SmokeDirectionData;
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
    public static final ResourceLocation DEFAULT_FIRST_PERSON_FLASH = ResourceLocation.fromNamespaceAndPath("tacz", "muzzle_flash");
    /** 默认 1p 烟雾粒子定义 id。 */
    public static final ResourceLocation DEFAULT_FIRST_PERSON_SMOKE = ResourceLocation.fromNamespaceAndPath("tacz", "muzzle_flash_smoke");
    /** 默认 3p 闪光粒子定义 id。 */
    public static final ResourceLocation DEFAULT_THIRD_PERSON_FLASH = ResourceLocation.fromNamespaceAndPath("tacz", "muzzle_flash");
    /** 默认 3p 烟雾粒子定义 id。 */
    public static final ResourceLocation DEFAULT_THIRD_PERSON_SMOKE = ResourceLocation.fromNamespaceAndPath("tacz", "muzzle_flash_smoke");

    /**
     * 枪口效果类型：default 走旧的四边形火光渲染；renew 使用第一人称 snowstorm 粒子系统。
     */
    @SerializedName("type")
    private Type type = Type.DEFAULT;

    @SerializedName("texture")
    protected ResourceLocation texture = null;

    @SerializedName("scale")
    private float scale = 1;

    /** Renew 开火时第一人称枪械的最低方块光，0 表示不额外照明。 */
    @SerializedName("flash_light_level")
    private int flashLightLevel = 15;

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

    /**
     * 枪烟的默认发射方向（裸枪状态）。装上枪口配件后由配件的同名字段接管——
     * 枪口配件本身就替换了枪口模型，排烟方式随之覆盖。
     * <p>
     * 写法与配件一致：单方向 {@code [x, y, z]} 或多方向 {@code [[x, y, z], ...]}，
     * {@code [0, 0, -1]} 为枪口正前方、{@code [0, 1, 0]} 为正上方。
     * 不填、格式非法或零向量时视为不干预，保持默认的均匀四散。
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
     * 枪烟方向的随机散布量，仅在配置了 {@link #smokeDirection} 时生效；
     * 不填则用 {@link SmokeDirectionData#DEFAULT_SPREAD}。装上枪口配件后由配件接管。
     */
    @SerializedName("smoke_spread")
    @Nullable
    private Float smokeSpread = null;

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

    public int getFlashLightLevel() {
        return Math.max(0, Math.min(15, flashLightLevel));
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
}
