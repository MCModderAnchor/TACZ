package com.tacz.guns.api;

import com.google.common.base.Suppliers;
import com.google.common.reflect.TypeToken;
import com.tacz.guns.api.modifier.ParameterizedCachePair;
import com.tacz.guns.resource.modifier.custom.InaccuracyModifier;
import com.tacz.guns.resource.pojo.data.gun.*;
import it.unimi.dsi.fastutil.Pair;
import org.jetbrains.annotations.ApiStatus;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 用于 AttachmentCacheProperty 的，类型安全的 key。
 */
public class GunProperties {
    /**
     * @since 1.1.7
     */
    static final Map<String, GunProperty<?>> ALL = new ConcurrentHashMap<>();

    /**
     * @since 1.1.7
     */
    static final Supplier<Map<String ,GunProperty<?>>> ALL_CACHE_MODIFIABLE_BY_SCRIPT = Suppliers.memoize(() -> List.<GunProperty<?>>of(
            GunProperties.AMMO_SPEED,
            GunProperties.ARMOR_IGNORE,
            GunProperties.EFFECTIVE_RANGE,
            GunProperties.HEADSHOT_MULTIPLIER,
            GunProperties.KNOCKBACK,
            GunProperties.PIERCE,
            GunProperties.WEIGHT
    ).stream().collect(Collectors.toMap(GunProperty::name, Function.identity())));

    /**
     * 返回所有属性
     *
     * @author ChloePrime
     * @since 1.1.7
     */
    public static Map<String, GunProperty<?>> all() {
        return Collections.unmodifiableMap(ALL);
    }

    /**
     * 返回所有可在配件缓存中被脚本更改的属性
     *
     * @author ChloePrime
     * @since 1.1.7
     */
    public static Map<String ,GunProperty<?>> allCacheModifiableByScript() {
        return ALL_CACHE_MODIFIABLE_BY_SCRIPT.get();
    }

    public static final GunProperty<Float>                                      ADS_TIME            = GunProperty.of("ads", Float.class);
    /**@deprecated
     * 此类是一个意外和设计失误，其功能和{@link InaccuracyModifier}完全重复<br/>
     * 已不再使用，内部的所有方法实际不会执行，请使用 {@link InaccuracyModifier} <br/>
     *
     * 同时，此Modifier的id也已经被重定向到 {@link InaccuracyModifier} <br/>
     * */
    @Deprecated
    public static final GunProperty<Map<InaccuracyType, Float>>                 AIM_INACCURACY      = GunProperty.of("inaccuracy", new TypeToken<>() {});

    @CacheModifiableByScript
    @ValueModifiableAtRuntime(Float.class)
    public static final GunProperty<Float>                                      AMMO_SPEED          = GunProperty.of("ammo_speed", Float.class);

    @CacheModifiableByScript
    @ValueModifiableAtRuntime(Float.class)
    public static final GunProperty<Float>                                      ARMOR_IGNORE        = GunProperty.of("armor_ignore", Float.class);

    /**
     * 枪械伤害。
     * 生效的值在命中时被脚本修改。
     */
    @ValueModifiableAtRuntime(Float.class)
    public static final GunProperty<LinkedList<ExtraDamage.DistanceDamagePair>> DAMAGE              = GunProperty.of("damage", new TypeToken<>() {});

    @CacheModifiableByScript
    @ValueModifiableAtRuntime(Float.class)
    public static final GunProperty<Float>                                      EFFECTIVE_RANGE     = GunProperty.of("effective_range", Float.class);

    /**
     * @see RuntimeOnly#EXPLODE_ENABLED
     * @see RuntimeOnly#EXPLOSION_DAMAGE
     * @see RuntimeOnly#EXPLOSION_RADIUS
     * @see RuntimeOnly#EXPLOSION_KNOCKBACK
     * @see RuntimeOnly#EXPLOSION_DESTROYS_BLOCK
     * @see RuntimeOnly#EXPLOSION_DELAY
     */
    public static final GunProperty<ExplosionData>                              EXPLOSION           = GunProperty.of("explosion", ExplosionData.class);

    public static final GunProperty<MoveSpeed>                                  MOVE_SPEED          = GunProperty.of("movement_speed", MoveSpeed.class);

    @CacheModifiableByScript
    @ValueModifiableAtRuntime(Float.class)
    public static final GunProperty<Float>                                      HEADSHOT_MULTIPLIER = GunProperty.of("head_shot", Float.class);

    /**
     * @see RuntimeOnly#IGNITE_ENTITY
     * @see RuntimeOnly#IGNITE_ENTITY_TIME
     * @see RuntimeOnly#IGNITE_BLOCK
     */
    public static final GunProperty<Ignite>                                     IGNITE              = GunProperty.of("ignite", Ignite.class);

    @ValueModifiableAtRuntime(Float.class)
    public static final GunProperty<Map<InaccuracyType, Float>>                 INACCURACY          = GunProperty.of("inaccuracy", new TypeToken<>() {});

    @CacheModifiableByScript
    @ValueModifiableAtRuntime(Float.class)
    public static final GunProperty<Float>                                      KNOCKBACK           = GunProperty.of("knockback", Float.class);

    @CacheModifiableByScript
    @ValueModifiableAtRuntime(Integer.class)
    public static final GunProperty<Integer>                                    PIERCE              = GunProperty.of("pierce", Integer.class);

    public static final GunProperty<GunRecoilPushData>                          RECOIL_PUSH         = GunProperty.of("recoil_push", GunRecoilPushData.class);

    public static final GunProperty<ParameterizedCachePair<Float, Float>>       RECOIL              = GunProperty.of("recoil", new TypeToken<>() {});
    public static final GunProperty<Integer>                                    ROUNDS_PER_MINUTE   = GunProperty.of("rpm", Integer.class);

    /**
     * @see RuntimeOnly#SOUND_DISTANCE
     */
    public static final GunProperty<Pair<Integer, Boolean>>                     SILENCE             = GunProperty.of("silence", new TypeToken<>() {});

    @CacheModifiableByScript
    public static final GunProperty<Float>                                      WEIGHT              = GunProperty.of("weight_modifier", Float.class);

    /**
     * 这个类是纯文档性质的，
     * 介绍了仅限脚本运行时修改，而不存在于配件缓存中的一些枪械属性。
     *
     * @author ChloePrime
     * @since 1.1.7
     */
    @ApiStatus.Experimental
    public static final class RuntimeOnly {
        /**
         * 热量上限
         */
        @ValueModifiableAtRuntime(Float.class)
        public static final String MAX_HEAT = "max_heat";

        /**
         * 弹丸数量
         */
        @ValueModifiableAtRuntime(Integer.class)
        public static final String BULLET_AMOUNT = "bullet_amount";

        /**
         * 连发数量，
         * 对于非 Burst 模式依然生效
         */
        @ValueModifiableAtRuntime(Integer.class)
        public static final String BURST_COUNT = "burst_count";

        /**
         * 连发间隔，单位为毫秒（ms）
         * 对于非 Burst 模式依然生效
         */
        @ValueModifiableAtRuntime(Long.class)
        public static final String BURST_SHOOT_INTERVAL = "burst_shoot_interval";

        /**
         * 子弹寿命，单位为秒（s）
         */
        @ValueModifiableAtRuntime(Float.class)
        public static final String BULLET_LIFE = "bullet_life";

        /**
         * 子弹重力
         */
        @ValueModifiableAtRuntime(Float.class)
        public static final String BULLET_GRAVITY = "bullet_gravity";

        /**
         * 子弹空气阻力
         */
        @ValueModifiableAtRuntime(Float.class)
        public static final String BULLET_FRICTION = "bullet_friction";

        /**
         * 子弹声音传播距离
         */
        @ValueModifiableAtRuntime(Integer.class)
        public static final String SOUND_DISTANCE = "sound_distance";

        /**
         * 是否点燃实体
         */
        @ValueModifiableAtRuntime(Boolean.class)
        public static final String IGNITE_ENTITY = "ignite_entity";

        /**
         * 点燃实体的时间，单位 tick
         */
        @ValueModifiableAtRuntime(Integer.class)
        public static final String IGNITE_ENTITY_TIME = "ignite_entity_time";

        /**
         * 是否点燃方块
         */
        @ValueModifiableAtRuntime(Boolean.class)
        public static final String IGNITE_BLOCK = "ignite_block";

        /**
         * 子弹是否爆炸，
         * 在子弹创建时被修改。
         */
        @ValueModifiableAtRuntime(Boolean.class)
        public static final String EXPLODE_ENABLED = "explode_enabled";

        /**
         * 子弹爆炸伤害，
         * 在子弹创建时被修改。
         */
        @ValueModifiableAtRuntime(Float.class)
        public static final String EXPLOSION_DAMAGE = "explosion_damage";

        /**
         * 子弹爆炸半径，
         * 在子弹创建时被修改。
         */
        @ValueModifiableAtRuntime(Float.class)
        public static final String EXPLOSION_RADIUS = "explosion_radius";

        /**
         * 子弹爆炸是否造成击退，
         * 在子弹创建时被修改。
         */
        @ValueModifiableAtRuntime(Boolean.class)
        public static final String EXPLOSION_KNOCKBACK = "explosion_knockback";

        /**
         * 子弹爆炸是否炸坏方块，
         * 在子弹创建时被修改。
         */
        @ValueModifiableAtRuntime(Boolean.class)
        public static final String EXPLOSION_DESTROYS_BLOCK = "explosion_destroys_block";

        /**
         * 子弹发射后到自动爆炸的延迟，单位为秒（s）
         * 在子弹创建时被修改。
         */
        @ValueModifiableAtRuntime(Float.class)
        public static final String EXPLOSION_DELAY = "explosion_delay";
    }

    private GunProperties() {
    }
}
