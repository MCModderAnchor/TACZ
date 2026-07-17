package com.tacz.guns.entity;

import com.google.common.collect.Lists;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.GunProperties;
import com.tacz.guns.api.GunProperty;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ITargetEntity;
import com.tacz.guns.api.entity.KnockBackModifier;
import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.api.event.common.EntityKillByGunEvent;
import com.tacz.guns.api.event.server.AmmoHitBlockEvent;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.client.particle.AmmoParticleSpawner;
import com.tacz.guns.config.common.AmmoConfig;
import com.tacz.guns.config.sync.SyncConfig;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import com.tacz.guns.init.ModDamageTypes;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.event.ServerMessageGunHurt;
import com.tacz.guns.network.message.event.ServerMessageGunKill;
import com.tacz.guns.particles.BulletHoleOption;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.custom.DamageModifier;
import com.tacz.guns.resource.modifier.custom.ExplosionModifier;
import com.tacz.guns.resource.modifier.custom.IgniteModifier;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import com.tacz.guns.resource.pojo.data.gun.ExplosionData;
import com.tacz.guns.resource.pojo.data.gun.ExtraDamage.DistanceDamagePair;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.resource.pojo.data.gun.Ignite;
import com.tacz.guns.util.BulletDamageContext;
import com.tacz.guns.util.EntityUtil;
import com.tacz.guns.util.ExplodeUtil;
import com.tacz.guns.util.TacHitResult;
import com.tacz.guns.util.block.BlockRayTrace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkHooks;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.*;

import static com.tacz.guns.api.GunProperties.RuntimeOnly.*;
import static com.tacz.guns.api.event.common.GunDamageSourcePart.ARMOR_PIERCING;
import static com.tacz.guns.api.event.common.GunDamageSourcePart.NON_ARMOR_PIERCING;

/**
 * 动能武器打出的子弹实体。
 */
public class EntityKineticBullet extends Projectile implements IEntityAdditionalSpawnData {
    public static final EntityType<EntityKineticBullet> TYPE = EntityType.Builder.<EntityKineticBullet>of(EntityKineticBullet::new, MobCategory.MISC).noSummon().noSave().fireImmune().sized(0.0625F, 0.0625F).clientTrackingRange(5).updateInterval(5).setShouldReceiveVelocityUpdates(false).build("bullet");
    public static final TagKey<EntityType<?>> USE_MAGIC_DAMAGE_ON = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("tacz:use_magic_damage_on"));
    public static final TagKey<EntityType<?>> USE_VOID_DAMAGE_ON = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("tacz:use_void_damage_on"));
    public static final TagKey<EntityType<?>> PRETEND_MELEE_DAMAGE_ON = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("tacz:pretend_melee_damage_on"));

    /**
     * 允许其他 mod 使用 persistent data（永久数据） 控制曳光弹的颜色和粗细。<p>
     * 使用永久数据的好处是即使以后本类大改，使用了这个功能的其他 mod 也不会崩溃。<p>
     * 下面两个字段是 persistent data 的 key。<p>
     * 这个字段的值的类型是 int[4]。<p>
     * <p>
     * 使用例：
     * <pre>{@code
     *     bullet.getPersistentData().putIntArray(TRACER_COLOR_OVERRIDER_KEY, new int[]{255, 255, 255, 255});
     * }</pre>
     */
    public static final String TRACER_COLOR_OVERRIDER_KEY = GunMod.MOD_ID + ":tracer_override";

    /**
     * 这个字段的值的类型是 float。
     * 1 表示默认大小，0 表示 0 倍率粗细（不显示了）
     */
    public static final String TRACER_SIZE_OVERRIDER_KEY = GunMod.MOD_ID + ":tracer_size";

    private static final ExplosionData DEFAULT_EXPLOSION_DATA = new ExplosionData(false, 0, 0, false, 30, false);

    private ResourceLocation ammoId = DefaultAssets.EMPTY_AMMO_ID;
    private int life = 200;
    @Deprecated
    private float speed = 1;
    private float gravity = 0;
    private float friction = 0.01F;
    private LinkedList<DistanceDamagePair> damageAmount = Lists.newLinkedList();
    private float distanceAmount = 0;
    private float knockback = 0;
    private boolean explosion = false;
    private boolean igniteEntity = false;
    private boolean igniteBlock = false;
    private int igniteEntityTime = 2;
    private float explosionDamage = 3;
    private float explosionRadius = 3;
    private int explosionDelayCount = Integer.MAX_VALUE;
    private boolean explosionKnockback = false;
    private boolean explosionDestroyBlock = false;
    private float damageModifier = 1;
    // 穿透数
    private int pierce = 1;
    // 初始位置
    private Vec3 startPos;
    // 曳光弹
    private boolean isTracerAmmo;
    // 以下几个是只对客户端有用的曳光弹数据
    private float cameraXRot;
    private float cameraYRot;
    private Vector3f firstPersonRenderOffset;
    // 发射的枪械 ID
    private ResourceLocation gunId;
    // 枪械display ID
    private ResourceLocation gunDisplayId;
    private float armorIgnore;
    private float headShot;
    private float shotDamageMultiplier = 1f;

    public EntityKineticBullet(EntityType<? extends Projectile> type, Level worldIn) {
        super(type, worldIn);
    }

    public EntityKineticBullet(EntityType<? extends Projectile> type, double x, double y, double z, Level worldIn) {
        this(type, worldIn);
        this.setPos(x, y, z);
    }

    public EntityKineticBullet(Level worldIn, LivingEntity throwerIn, ItemStack gunItem, ResourceLocation ammoId, ResourceLocation gunId,
                               ResourceLocation gunDisplayId, boolean isTracerAmmo, GunData gunData, BulletData bulletData) {
        this(TYPE, worldIn, throwerIn, gunItem, ammoId, gunId, gunDisplayId, isTracerAmmo, gunData, bulletData);
    }

    public EntityKineticBullet(Level worldIn, LivingEntity throwerIn, ItemStack gunItem, ResourceLocation ammoId, ResourceLocation gunId, boolean isTracerAmmo, GunData gunData, BulletData bulletData) {
        this(TYPE, worldIn, throwerIn, gunItem, ammoId, gunId, DefaultAssets.DEFAULT_GUN_DISPLAY_ID, isTracerAmmo, gunData, bulletData);
    }

    protected EntityKineticBullet(EntityType<? extends Projectile> type, Level worldIn, LivingEntity throwerIn, ItemStack gunItem,
                                  ResourceLocation ammoId, ResourceLocation gunId, ResourceLocation gunDisplayId,
                                  boolean isTracerAmmo, GunData gunData, BulletData bulletData) {
        this(type, throwerIn.getX(), throwerIn.getEyeY() - (double) 0.1F, throwerIn.getZ(), worldIn);
        this.setOwner(throwerIn);
        // gunId 提前赋值，以让 modifyProperty 可以在构造函数中运行
        this.gunId = gunId;
        AttachmentCacheProperty cacheProperty = Objects.requireNonNull(IGunOperator.fromLivingEntity(throwerIn).getCacheProperty());
        float armorIgnore = modifyProperty(GunProperties.ARMOR_IGNORE, Float.class, cacheProperty.getCache(GunProperties.ARMOR_IGNORE));
        float headshot = modifyProperty(GunProperties.HEADSHOT_MULTIPLIER, Float.class, cacheProperty.getCache(GunProperties.HEADSHOT_MULTIPLIER));
        float knockback = modifyProperty(GunProperties.KNOCKBACK, Float.class, cacheProperty.getCache(GunProperties.KNOCKBACK));
        this.armorIgnore = Mth.clamp(armorIgnore, 0f, 1f);
        this.headShot = Math.max(headshot, 0f);
        this.knockback = Math.max(knockback, 0f);
        this.ammoId = ammoId;
        float lifeSecond = modifyProperty(BULLET_LIFE, Float.class, bulletData.getLifeSecond());
        this.life = Mth.clamp((int) (lifeSecond * 20), 1, Integer.MAX_VALUE);
        // speed 字段是无效的，实际生效的速度是 shootOnce 里传给 doBulletSpread 的速度
        this.gravity = Mth.clamp(modifyProperty(BULLET_GRAVITY, Float.class, bulletData.getGravity()), 0f, Float.MAX_VALUE);
        this.friction = Mth.clamp(modifyProperty(BULLET_FRICTION, Float.class, bulletData.getFriction()), 0f, Float.MAX_VALUE);
        // 点燃
        Ignite ignite = cacheProperty.getCache(IgniteModifier.ID);
        this.igniteEntity = modifyProperty(IGNITE_ENTITY, Boolean.class, bulletData.getIgnite().isIgniteEntity() || ignite.isIgniteEntity());
        this.igniteEntityTime = Math.max(modifyProperty(IGNITE_ENTITY_TIME, Integer.class, bulletData.getIgniteEntityTime()), 0);
        this.igniteBlock = modifyProperty(IGNITE_BLOCK, Boolean.class, bulletData.getIgnite().isIgniteBlock() || ignite.isIgniteBlock());
        this.damageAmount = cacheProperty.getCache(DamageModifier.ID);
        this.distanceAmount = modifyProperty(GunProperties.EFFECTIVE_RANGE, Float.class, cacheProperty.getCache(GunProperties.EFFECTIVE_RANGE));
        int pierce = modifyProperty(GunProperties.PIERCE, Integer.class, cacheProperty.getCache(GunProperties.PIERCE));
        this.pierce = Mth.clamp(pierce, 1, Integer.MAX_VALUE);
        ExplosionData explosionData = Objects.requireNonNullElse(cacheProperty.getCache(ExplosionModifier.ID), DEFAULT_EXPLOSION_DATA);
        this.explosion = modifyProperty(EXPLODE_ENABLED, Boolean.class, explosionData.isExplode());
        if (this.explosion) {
            var explosionDamage = modifyProperty(EXPLOSION_DAMAGE, Float.class, explosionData.getDamage());
            var explosionRadius = modifyProperty(EXPLOSION_RADIUS, Float.class, explosionData.getRadius());
            this.explosionDamage = (float) Mth.clamp(explosionDamage * SyncConfig.DAMAGE_BASE_MULTIPLIER.get(), 0, Float.MAX_VALUE);
            this.explosionRadius = Mth.clamp(explosionRadius, 0, Float.MAX_VALUE);
            this.explosionKnockback = modifyProperty(EXPLOSION_KNOCKBACK, Boolean.class, explosionData.isKnockback());
            // 防止越界，提前判定
            int delayTickCount = (int) (modifyProperty(EXPLOSION_DELAY, Float.class, explosionData.getDelay()) * 20);
            if (delayTickCount < 0) {
                delayTickCount = Integer.MAX_VALUE;
            }
            // 配置文件关闭爆炸后忽略脚本对爆炸是否破坏方块的修改
            this.explosionDestroyBlock = AmmoConfig.EXPLOSIVE_AMMO_DESTROYS_BLOCK.get() && modifyProperty(EXPLOSION_DESTROYS_BLOCK, Boolean.class, explosionData.isDestroyBlock());
            this.explosionDelayCount = Math.max(delayTickCount, 1);
        }
        // 子弹初始位置重置
        double posX = throwerIn.xOld + (throwerIn.getX() - throwerIn.xOld) / 2.0;
        double posY = throwerIn.yOld + (throwerIn.getY() - throwerIn.yOld) / 2.0 + throwerIn.getEyeHeight();
        double posZ = throwerIn.zOld + (throwerIn.getZ() - throwerIn.zOld) / 2.0;
        this.setPos(posX, posY, posZ);
        this.startPos = this.position();
        this.isTracerAmmo = isTracerAmmo;
        this.gunDisplayId = gunDisplayId;
    }

    @ApiStatus.Internal
    public void applyShotgunDamageSpread(int bulletCount) {
        // 霰弹情况，每个伤害要扣去
        if (bulletCount > 1) {
            this.damageModifier = 1f / bulletCount;
        }
    }

    @ApiStatus.Internal
    public void setShotDamageMultiplier(float multiplier) {
        this.shotDamageMultiplier = Math.max(multiplier, 0f);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();
        // 调用 TaC 子弹服务器事件
        this.onBulletTick();
        // 粒子效果
        if (this.level().isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> AmmoParticleSpawner.addParticle(this));
        }
        // 子弹模型的旋转与抛物线
        Vec3 movement = this.getDeltaMovement();
        double x = movement.x;
        double y = movement.y;
        double z = movement.z;
        double distance = movement.horizontalDistance();
        this.setYRot((float) Math.toDegrees(Mth.atan2(x, z)));
        this.setXRot((float) Math.toDegrees(Mth.atan2(y, distance)));
        // 子弹初始的朝向设置
        if (this.xRotO == 0.0F && this.yRotO == 0.0F) {
            this.yRotO = this.getYRot();
            this.xRotO = this.getXRot();
        }
        // 子弹运动时的旋转（不包含自转）
        this.setXRot(lerpRotation(this.xRotO, this.getXRot()));
        this.setYRot(lerpRotation(this.yRotO, this.getYRot()));
        // 子弹位置更新
        double nextPosX = this.getX() + x;
        double nextPosY = this.getY() + y;
        double nextPosZ = this.getZ() + z;
        this.setPos(nextPosX, nextPosY, nextPosZ);
        float friction = this.friction;
        float gravity = this.gravity;
        // 子弹入水后的调整
        if (this.isInWater()) {
            for (int i = 0; i < 4; i++) {
                this.level().addParticle(ParticleTypes.BUBBLE, nextPosX - x * 0.25F, nextPosY - y * 0.25F, nextPosZ - z * 0.25F, x, y, z);
            }
            // 在水中的阻力
            friction = 0.4F;
            gravity *= 0.6F;
        }
        // 重力与阻力更新速度状态
        this.setDeltaMovement(this.getDeltaMovement().scale(1 - friction));
        this.setDeltaMovement(this.getDeltaMovement().add(0, -gravity, 0));
        // 子弹生命结束
        if (this.tickCount >= this.life - 1) {
            this.discard();
        }
    }

    // 子弹的逻辑处理
    protected void onBulletTick() {
        // 服务器端子弹逻辑
        if (!this.level().isClientSide()) {
            // 延迟爆炸判定
            if (this.explosion) {
                if (this.explosionDelayCount > 0) {
                    this.explosionDelayCount--;
                } else {
                    ExplodeUtil.createExplosion(this.getOwner(), this, this.explosionDamage, this.explosionRadius, this.explosionKnockback, this.explosionDestroyBlock, this.position());
                    // 爆炸直接结束不留弹孔，不处理之后的逻辑
                    this.discard();
                    return;
                }
            }
            // 子弹在 tick 起始的位置
            Vec3 startVec = this.position();
            // 子弹在 tick 结束的位置
            Vec3 endVec = startVec.add(this.getDeltaMovement());
            // 子弹的碰撞检测
            HitResult result = BlockRayTrace.rayTraceBlocks(this.level(), new ClipContext(startVec, endVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            BlockHitResult resultB = (BlockHitResult) result;
            if (resultB.getType() != HitResult.Type.MISS) {
                // 子弹击中方块时，设置击中方块的位置为子弹的结束位置
                endVec = resultB.getLocation();
            }

            List<EntityResult> hitEntities = null;
            // 子弹的击中检测，穿透为 1 或者爆炸类弹药限制为一个实体穿透判定
            if (this.pierce <= 1 || this.explosion) {
                EntityResult entityResult = EntityUtil.findEntityOnPath(this, startVec, endVec);
                // 将单个命中是实体创建为单个内容的 list
                if (entityResult != null) {
                    hitEntities = Collections.singletonList(entityResult);
                }
            } else {
                hitEntities = EntityUtil.findEntitiesOnPath(this, startVec, endVec);
            }
            // 当子弹击中实体时，进行被命中的实体读取
            if (hitEntities != null && !hitEntities.isEmpty()) {
                EntityResult[] hitEntityResult = hitEntities.toArray(new EntityResult[0]);
                // 对被命中的实体进行排序，按照距离子弹发射位置的距离进行升序排序
                for (int i = 0; (i < this.pierce || i < 1) && i < (hitEntityResult.length - 1); i++) {
                    int k = i;
                    for (int j = i + 1; j < hitEntityResult.length; j++) {
                        if (hitEntityResult[j].hitVec.distanceTo(startVec) < hitEntityResult[k].hitVec.distanceTo(startVec)) {
                            k = j;
                        }
                    }
                    EntityResult t = hitEntityResult[i];
                    hitEntityResult[i] = hitEntityResult[k];
                    hitEntityResult[k] = t;
                }
                for (EntityResult entityResult : hitEntityResult) {
                    result = new TacHitResult(entityResult);
                    this.onHitEntity((TacHitResult) result, startVec, endVec);
                    this.pierce--;
                    if (this.pierce < 1 || this.explosion) {
                        // 子弹已经穿透所有实体，结束子弹的飞行
                        this.discard();
                        return;
                    }
                }
            }
            this.onHitBlock(resultB, startVec, endVec);
        }
    }

    public void shoot(double pitch, double yaw, float pVelocity, Vector2d vector2d) {
        Vector3d left = new Vector3d(vector2d.x, vector2d.y, 8);

        left.rotateX(pitch * Mth.DEG_TO_RAD);
        left.rotateY(-yaw * Mth.DEG_TO_RAD);

        Vec3 vec3 = new Vec3(left.x, left.y, left.z).normalize().scale(pVelocity);

        this.setDeltaMovement(vec3.x, vec3.y, vec3.z);
        double d0 = vec3.horizontalDistance();
        this.setYRot((float)(Mth.atan2(vec3.x, vec3.z) * (double)(180F / (float)Math.PI)));
        this.setXRot((float)(Mth.atan2(vec3.y, d0) * (double)(180F / (float)Math.PI)));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    public void shootFromRotation(Entity pShooter, float pX, float pY, float pZ, float pVelocity, Vector2d vector2d) {
        this.shoot(pX, pY, pVelocity, vector2d);
        Vec3 vec3 = pShooter.getDeltaMovement();
        this.setDeltaMovement(this.getDeltaMovement().add(vec3.x, pShooter.onGround() ? 0.0D : vec3.y, vec3.z));
    }

    public record MaybeMultipartEntity(
            Entity hitPart,
            Entity core
    ) {
        public static MaybeMultipartEntity of(Entity hitPart) {
            var core = (hitPart instanceof PartEntity<?> part)
                    ? part.getParent()
                    : hitPart;
            return new MaybeMultipartEntity(hitPart, core);
        }
    }

    protected void onHitEntity(TacHitResult result, Vec3 startVec, Vec3 endVec) {
        if (result.getEntity() instanceof ITargetEntity targetEntity) {
            DamageSource source = this.damageSources().thrown(this, this.getOwner());
            targetEntity.onProjectileHit(this, result, source, this.getDamage(result.getLocation()));
            // 打靶直接返回
            return;
        }
        // 获取Pre事件必要的信息
        Entity entity = result.getEntity();
        @Nullable Entity owner = this.getOwner();
        // 攻击者
        LivingEntity attacker = owner instanceof LivingEntity ? (LivingEntity) owner : null;
        var sources = createDamageSources(MaybeMultipartEntity.of(entity));
        boolean headshot = result.isHeadshot();
        float damage = this.getDamage(result.getLocation());
        float headShotMultiplier = Math.max(this.headShot, 0);
        // 发布Pre事件
        LivingEntity eventAttacker = attacker;
        Entity eventEntity = entity;
        float eventDamage = damage;
        Pair<DamageSource, DamageSource> eventSources = sources;
        boolean eventHeadshot = headshot;
        float eventHeadshotMultiplier = headShotMultiplier;
        var preEvent = BulletDamageContext.runWithoutShooter(() -> new EntityHurtByGunEvent.Pre(
                this, eventEntity, eventAttacker, this.gunId, this.gunDisplayId, eventDamage, eventSources,
                eventHeadshot, eventHeadshotMultiplier, LogicalSide.SERVER));
        var cancelled = BulletDamageContext.runWithoutShooter(() -> MinecraftForge.EVENT_BUS.post(preEvent));
        if (cancelled) {
            return;
        }
        // 刷新由Pre事件修改后的参数
        entity = preEvent.getHurtEntity();
        // 受击目标
        var parts = MaybeMultipartEntity.of(entity);
        attacker = preEvent.getAttacker();
        var newGunId = preEvent.getGunId();
        damage = preEvent.getBaseAmount();
        sources = Pair.of(preEvent.getDamageSource(NON_ARMOR_PIERCING), preEvent.getDamageSource(ARMOR_PIERCING));
        headshot = preEvent.isHeadShot();
        headShotMultiplier = preEvent.getHeadshotMultiplier();
        if (entity == null) {
            return;
        }
        // 点燃
        if (this.igniteEntity && AmmoConfig.IGNITE_ENTITY.get()) {
            entity.setSecondsOnFire(this.igniteEntityTime);
            // 给予粒子效果
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.LAVA, entity.getX(), entity.getY() + entity.getEyeHeight(), entity.getZ(), 1, 0, 0, 0, 0);
            }
        }
        // TODO 暴击判定（不是爆头）暴击判定内部逻辑，需要输出一个是否暴击的 flag
        if (headshot) {
            // 默认爆头伤害是 1x
            damage *= headShotMultiplier;
        }
        // 对 LivingEntity 进行击退强度的自定义
        if (parts.core() instanceof LivingEntity livingCore) {
            // 取消击退效果，设定自己的击退强度
            KnockBackModifier modifier = KnockBackModifier.fromLivingEntity(livingCore);
            modifier.setKnockBackStrength(this.knockback);
            // 创建伤害
            tacAttackEntity(parts, damage, sources);
            // 恢复原位
            modifier.resetKnockBackStrength();
        } else {
            // 创建伤害
            tacAttackEntity(parts, damage, sources);
        }
        // 爆炸逻辑
        if (this.explosion) {
            // 取消无敌时间
            parts.core().invulnerableTime = 0;
            ExplodeUtil.createExplosion(this.getOwner(), this, this.explosionDamage, this.explosionRadius, this.explosionKnockback, this.explosionDestroyBlock, result.getLocation());
        }
        // 只对 LivingEntity 执行击杀判定
        if (parts.core() instanceof LivingEntity livingCore) {
            // 事件同步，从服务端到客户端
            if (!level().isClientSide) {
                int attackerId = attacker == null ? 0 : attacker.getId();
                // 如果生物死了
                if (livingCore.isDeadOrDying()) {
                    MinecraftForge.EVENT_BUS.post(new EntityKillByGunEvent(this, livingCore, attacker, newGunId, gunDisplayId, damage, sources, headshot, headShotMultiplier, LogicalSide.SERVER));
                    NetworkHandler.sendToDimension(new ServerMessageGunKill(getId(), livingCore.getId(), attackerId, newGunId, gunDisplayId, damage, headshot, headShotMultiplier), livingCore);
                } else {
                    MinecraftForge.EVENT_BUS.post(new EntityHurtByGunEvent.Post(this, livingCore, attacker, newGunId, gunDisplayId, damage, sources, headshot, headShotMultiplier, LogicalSide.SERVER));
                    NetworkHandler.sendToDimension(new ServerMessageGunHurt(getId(), livingCore.getId(), attackerId, newGunId, gunDisplayId, damage, headshot, headShotMultiplier), livingCore);
                }
            }
        }
    }

    protected void onHitBlock(BlockHitResult result, Vec3 startVec, Vec3 endVec) {
        if (result.getType() == HitResult.Type.MISS) {
            return;
        }
        BlockPos pos = result.getBlockPos();
        Vec3 hitVec = result.getLocation();
        // 触发事件
        // 提前触发事件以让事件可以取消原版的命中行为（例如敲钟，打倒靶子等）
        if (MinecraftForge.EVENT_BUS.post(new AmmoHitBlockEvent(this.level(), result, this.level().getBlockState(pos), this))) {
            return;
        }
        super.onHitBlock(result);
        // 爆炸
        if (this.explosion) {
            ExplodeUtil.createExplosion(this.getOwner(), this, this.explosionDamage, this.explosionRadius, this.explosionKnockback, this.explosionDestroyBlock, hitVec);
            // 爆炸直接结束不留弹孔，不处理之后的逻辑
            this.discard();
            return;
        }
        // 弹孔与点燃特效
        if (this.level() instanceof ServerLevel serverLevel) {
            BulletHoleOption bulletHoleOption = new BulletHoleOption(result.getDirection(), result.getBlockPos(), this.ammoId.toString(), this.gunId.toString(), this.gunDisplayId.toString());
            serverLevel.sendParticles(bulletHoleOption, hitVec.x, hitVec.y, hitVec.z, 1, 0, 0, 0, 0);
            if (this.igniteBlock) {
                serverLevel.sendParticles(ParticleTypes.LAVA, hitVec.x, hitVec.y, hitVec.z, 1, 0, 0, 0, 0);
            }
        }
        if (this.igniteBlock && AmmoConfig.IGNITE_BLOCK.get()) {
            BlockPos offsetPos = pos.relative(result.getDirection());
            if (BaseFireBlock.canBePlacedAt(this.level(), offsetPos, result.getDirection())) {
                BlockState fireState = BaseFireBlock.getState(this.level(), offsetPos);
                this.level().setBlock(offsetPos, fireState, Block.UPDATE_ALL_IMMEDIATE);
                ((ServerLevel) this.level()).sendParticles(ParticleTypes.LAVA, hitVec.x - 1.0 + this.random.nextDouble() * 2.0, hitVec.y, hitVec.z - 1.0 + this.random.nextDouble() * 2.0, 4, 0, 0, 0, 0);
            }
        }
        this.discard();
    }

    // 根据距离进行伤害衰减设计
    public float getDamage(Vec3 hitVec) {
        // 如果忘记写最大值，那我就直接认为你伤害为 0
        float base = 0;
        // 遍历进行判断
        double playerDistance = hitVec.distanceTo(this.startPos);
        for (DistanceDamagePair pair : this.damageAmount) {
            float effectiveDistance = this.damageAmount.get(0).getDistance() == pair.getDistance() ? this.distanceAmount : pair.getDistance();
            if (playerDistance < effectiveDistance) {
                float damage = pair.getDamage();
                base = Math.max(damage * this.damageModifier, 0F);
                break;
            }
        }
        // 让脚本修改枪械伤害
        float modifiedDamage = modifyProperty(GunProperties.DAMAGE, Float.class, base);
        return Math.max(modifiedDamage * this.shotDamageMultiplier, 0F);
    }

    /**
     * @since 1.1.7
     */
    private <T> T modifyProperty(GunProperty<?> prop, Class<T> type, T original) {
        return modifyProperty(prop.name(), type, original);
    }

    /**
     * @since 1.1.7
     */
    private <T> T modifyProperty(String id, Class<T> type, T original) {
        if (getOwner() instanceof LivingEntity shooter) {
            ItemStack gun = shooter.getMainHandItem();
            if (gun.getItem() instanceof AbstractGunItem gunInterface && Objects.equals(this.gunId, gunInterface.getGunId(gun))) {
                ShooterDataHolder dataHolder = IGunOperator.fromLivingEntity(shooter).getDataHolder();
                return gunInterface.modifyProperty(dataHolder, gun, shooter, id, type, original);
            }
        }
        return original;
    }

    /**
     * @return Pair<非穿甲伤害源，穿甲伤害源>
     */
    private Pair<DamageSource, DamageSource> createDamageSources(MaybeMultipartEntity parts) {
        DamageSource source1, source2;
        var hitPartType = parts.hitPart().getType();
        var directCause = hitPartType.is(PRETEND_MELEE_DAMAGE_ON) ? this.getOwner() : this;
        // 给末影人造成伤害
        if (hitPartType.is(USE_MAGIC_DAMAGE_ON)) {
            source1 = source2 = this.damageSources().indirectMagic(this, getOwner());
        } else if (hitPartType.is(USE_VOID_DAMAGE_ON)) {
            source1 = ModDamageTypes.Sources.bulletVoid(this.level().registryAccess(), directCause, this.getOwner(), false);
            source2 = ModDamageTypes.Sources.bulletVoid(this.level().registryAccess(), directCause, this.getOwner(), true);
        } else {
            source1 = ModDamageTypes.Sources.bullet(this.level().registryAccess(), directCause, this.getOwner(), false);
            source2 = ModDamageTypes.Sources.bullet(this.level().registryAccess(), directCause, this.getOwner(), true);
        }
        return Pair.of(source1, source2);
    }

    private void tacAttackEntity(MaybeMultipartEntity parts, float damage, Pair<DamageSource, DamageSource> sources) {
        var source1 = sources.getLeft();
        var source2 = sources.getRight();
        // 穿甲伤害和普通伤害的比例计算
        float armorDamagePercent = Mth.clamp(this.armorIgnore, 0.0F, 1.0F);
        float normalDamagePercent = 1 - armorDamagePercent;
        // 取消无敌时间
        parts.core().invulnerableTime = 0;
        // 普通伤害
        parts.hitPart().hurt(source1, damage * normalDamagePercent);
        // 取消无敌时间
        parts.core().invulnerableTime = 0;
        // 穿甲伤害
        parts.hitPart().hurt(source2, damage * armorDamagePercent);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeFloat(getXRot());
        buffer.writeFloat(getYRot());
        buffer.writeDouble(getDeltaMovement().x);
        buffer.writeDouble(getDeltaMovement().y);
        buffer.writeDouble(getDeltaMovement().z);
        Entity entity = getOwner();
        buffer.writeInt(entity != null ? entity.getId() : 0);
        buffer.writeResourceLocation(ammoId);
        buffer.writeFloat(this.gravity);
        buffer.writeBoolean(this.explosion);
        buffer.writeBoolean(this.igniteEntity);
        buffer.writeBoolean(this.igniteBlock);
        buffer.writeFloat(this.explosionRadius);
        buffer.writeFloat(this.explosionDamage);
        buffer.writeInt(this.life);
        buffer.writeFloat(this.speed);
        buffer.writeFloat(this.friction);
        buffer.writeInt(this.pierce);
        buffer.writeBoolean(this.isTracerAmmo);
        buffer.writeResourceLocation(this.gunId);
        buffer.writeResourceLocation(this.gunDisplayId);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf additionalData) {
        setXRot(additionalData.readFloat());
        setYRot(additionalData.readFloat());
        setDeltaMovement(additionalData.readDouble(), additionalData.readDouble(), additionalData.readDouble());
        Entity entity = this.level().getEntity(additionalData.readInt());
        if (entity != null) {
            this.setOwner(entity);
        }
        this.ammoId = additionalData.readResourceLocation();
        this.gravity = additionalData.readFloat();
        this.explosion = additionalData.readBoolean();
        this.igniteEntity = additionalData.readBoolean();
        this.igniteBlock = additionalData.readBoolean();
        this.explosionRadius = additionalData.readFloat();
        this.explosionDamage = additionalData.readFloat();
        this.life = additionalData.readInt();
        this.speed = additionalData.readFloat();
        this.friction = additionalData.readFloat();
        this.pierce = additionalData.readInt();
        this.isTracerAmmo = additionalData.readBoolean();
        this.gunId = additionalData.readResourceLocation();
        this.gunDisplayId = additionalData.readResourceLocation();
    }

    public ResourceLocation getAmmoId() {
        return ammoId;
    }

    public ResourceLocation getGunId() {
        return gunId;
    }

    public ResourceLocation getGunDisplayId() {
        return gunDisplayId;
    }

    public boolean isTracerAmmo() {
        return isTracerAmmo;
    }

    public RandomSource getRandom() {
        return this.random;
    }

    public float getCameraYRot() {
        return cameraYRot;
    }

    public void setCameraYRot(float cameraYRot) {
        this.cameraYRot = cameraYRot;
    }

    public float getCameraXRot() {
        return cameraXRot;
    }

    public void setCameraXRot(float cameraXRot) {
        this.cameraXRot = cameraXRot;
    }

    public Vector3f getFirstPersonRenderOffset() {
        return firstPersonRenderOffset;
    }

    public void setFirstPersonRenderOffset(Vector3f originRenderOffset) {
        this.firstPersonRenderOffset = originRenderOffset;
    }

    public Optional<float[]> getTracerColorOverride() {
        var pd = getPersistentData();
        if (!pd.contains(TRACER_COLOR_OVERRIDER_KEY, Tag.TAG_INT_ARRAY)) {
            return Optional.empty();
        } else {
            var ints = pd.getIntArray(TRACER_COLOR_OVERRIDER_KEY);
            // 请避免使用 1 或者 2 个值的数组。
            // 此处 1~2 个值的分支仅为优雅地处理异常情况来代替崩溃所作的措施 :(
            switch (ints.length) {
                case 0:
                    return Optional.empty();
                case 1: {
                    var albedo = ints[0] / 255F;
                    return Optional.of(new float[]{albedo, albedo, albedo, 1});
                }
                case 2: {
                    var albedo = ints[0] / 255F;
                    var alpha = ints[1] / 255F;
                    return Optional.of(new float[]{albedo, albedo, albedo, alpha});
                }
                case 3: {
                    var r = ints[0] / 255F;
                    var g = ints[1] / 255F;
                    var b = ints[2] / 255F;
                    return Optional.of(new float[]{r, g, b, 1});
                }
                default: {
                    var r = ints[0] / 255F;
                    var g = ints[1] / 255F;
                    var b = ints[2] / 255F;
                    var a = ints[3] / 255F;
                    return Optional.of(new float[]{r, g, b, a});
                }
            }
        }
    }

    public float getTracerSizeOverride() {
        var pd = getPersistentData();
        return pd.contains(TRACER_SIZE_OVERRIDER_KEY, Tag.TAG_ANY_NUMERIC) ? pd.getFloat(TRACER_SIZE_OVERRIDER_KEY) : 1;
    }

    @Override
    public boolean ownedBy(@Nullable Entity entity) {
        if (entity == null) {
            return false;
        }
        return super.ownedBy(entity);
    }

    public static class EntityResult {
        private final Entity entity;
        private final Vec3 hitVec;
        private final boolean headshot;

        public EntityResult(Entity entity, Vec3 hitVec, boolean headshot) {
            this.entity = entity;
            this.hitVec = hitVec;
            this.headshot = headshot;
        }

        // 子弹命中的实体
        public Entity getEntity() {
            return this.entity;
        }

        // 子弹命中的位置
        public Vec3 getHitPos() {
            return this.hitVec;
        }

        // 是否为爆头
        public boolean isHeadshot() {
            return this.headshot;
        }
    }
}
