package com.tacz.guns.entity;

import com.google.common.collect.Lists;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ITargetEntity;
import com.tacz.guns.api.entity.KnockBackModifier;
import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.api.event.common.EntityKillByGunEvent;
import com.tacz.guns.api.event.server.AmmoHitBlockEvent;
import com.tacz.guns.client.particle.AmmoParticleSpawner;
import com.tacz.guns.config.common.AmmoConfig;
import com.tacz.guns.config.sync.SyncConfig;
import com.tacz.guns.init.ModAttributes;
import com.tacz.guns.init.ModDamageTypes;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.event.ServerMessageGunHurt;
import com.tacz.guns.network.message.event.ServerMessageGunKill;
import com.tacz.guns.particles.BulletHoleOption;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.custom.*;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import com.tacz.guns.resource.pojo.data.gun.ExplosionData;
import com.tacz.guns.resource.pojo.data.gun.ExtraDamage.DistanceDamagePair;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.resource.pojo.data.gun.Ignite;
import com.tacz.guns.util.EntityUtil;
import com.tacz.guns.util.ExplodeUtil;
import com.tacz.guns.util.TacHitResult;
import com.tacz.guns.util.block.BlockRayTrace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
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
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
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
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

/**
 * 动能武器打出的子弹实体。
 */
public class EntityKineticBullet extends Projectile implements IEntityAdditionalSpawnData {
    public static final EntityType<EntityKineticBullet> TYPE = EntityType.Builder.<EntityKineticBullet>of(EntityKineticBullet::new, MobCategory.MISC).noSummon().noSave().fireImmune().sized(0.0625F, 0.0625F).clientTrackingRange(5).updateInterval(5).setShouldReceiveVelocityUpdates(false).build("bullet");
    public static final TagKey<EntityType<?>> USE_MAGIC_DAMAGE_ON = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("tacz:use_magic_damage_on"));
    public static final TagKey<EntityType<?>> USE_VOID_DAMAGE_ON = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("tacz:use_void_damage_on"));
    public static final TagKey<EntityType<?>> PRETEND_MELEE_DAMAGE_ON = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("tacz:pretend_melee_damage_on"));
    private ResourceLocation ammoId = DefaultAssets.EMPTY_AMMO_ID;
    private int life = 200;
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
    // 只对客户端有用的曳光弹数据
    private Vec3 originCameraPosition;
    private Vec3 originRenderOffset;
    // 发射的枪械 ID
    private ResourceLocation gunId;
    private float armorIgnore;
    private float headShot;

    public EntityKineticBullet(EntityType<? extends Projectile> type, Level worldIn) {
        super(type, worldIn);
    }

    public EntityKineticBullet(EntityType<? extends Projectile> type, double x, double y, double z, Level worldIn) {
        this(type, worldIn);
        this.setPos(x, y, z);
    }

    public EntityKineticBullet(Level worldIn, LivingEntity throwerIn, ItemStack gunItem, ResourceLocation ammoId, ResourceLocation gunId, boolean isTracerAmmo, GunData gunData, BulletData bulletData) {
        this(TYPE, worldIn, throwerIn, gunItem, ammoId, gunId, isTracerAmmo, gunData, bulletData);
    }

    protected EntityKineticBullet(EntityType<? extends Projectile> type, Level worldIn, LivingEntity throwerIn, ItemStack gunItem, ResourceLocation ammoId, ResourceLocation gunId, boolean isTracerAmmo, GunData gunData, BulletData bulletData) {
        this(type, throwerIn.getX(), throwerIn.getEyeY() - (double) 0.1F, throwerIn.getZ(), worldIn);
        this.setOwner(throwerIn);
        AttachmentCacheProperty cacheProperty = Objects.requireNonNull(IGunOperator.fromLivingEntity(throwerIn).getCacheProperty());
        this.armorIgnore = Mth.clamp(cacheProperty.getCache(ArmorIgnoreModifier.ID), 0f, 1f);
        this.headShot = Math.max(cacheProperty.getCache(HeadShotModifier.ID), 0f);
        this.knockback = Math.max(cacheProperty.getCache(KnockbackModifier.ID), 0f);
        this.ammoId = ammoId;
        this.life = Mth.clamp((int) (bulletData.getLifeSecond() * 20), 1, Integer.MAX_VALUE);
        // 限制最大弹速为 600 m / s，以减轻计算负担
        this.speed = Mth.clamp(cacheProperty.<Float>getCache(AmmoSpeedModifier.ID) / 20f, 0f, 30f);
        this.gravity = Mth.clamp(bulletData.getGravity(), 0f, Float.MAX_VALUE);
        this.friction = Mth.clamp(bulletData.getFriction(), 0f, Float.MAX_VALUE);
        Ignite ignite = cacheProperty.getCache(IgniteModifier.ID);
        this.igniteEntity = bulletData.getIgnite().isIgniteEntity() || ignite.isIgniteEntity();
        this.igniteEntityTime = Math.max(bulletData.getIgniteEntityTime(), 0);
        this.igniteBlock = bulletData.getIgnite().isIgniteBlock() || ignite.isIgniteBlock();
        this.damageAmount = cacheProperty.getCache(DamageModifier.ID);
        this.distanceAmount = cacheProperty.getCache(EffectiveRangeModifier.ID);
        // 霰弹情况，每个伤害要扣去
        if (bulletData.getBulletAmount() > 1) {
            this.damageModifier = 1f / bulletData.getBulletAmount();
        }
        this.pierce = Mth.clamp(cacheProperty.getCache(PierceModifier.ID), 1, Integer.MAX_VALUE);
        ExplosionData explosionData = cacheProperty.getCache(ExplosionModifier.ID);
        if (explosionData != null) {
            this.explosion = explosionData.isExplode();
            this.explosionDamage = (float) Mth.clamp(explosionData.getDamage() * SyncConfig.DAMAGE_BASE_MULTIPLIER.get(), 0, Float.MAX_VALUE);
            this.explosionRadius = Mth.clamp(explosionData.getRadius(), 0, Float.MAX_VALUE);
            this.explosionKnockback = explosionData.isKnockback();
            // 防止越界，提前判定
            int delayTickCount = explosionData.getDelay() * 20;
            if (delayTickCount < 0) {
                delayTickCount = Integer.MAX_VALUE;
            }
            this.explosionDestroyBlock = explosionData.isDestroyBlock() && AmmoConfig.EXPLOSIVE_AMMO_DESTROYS_BLOCK.get();
            this.explosionDelayCount = Math.max(delayTickCount, 1);
        }
        // 子弹初始位置重置
        double posX = throwerIn.xOld + (throwerIn.getX() - throwerIn.xOld) / 2.0;
        double posY = throwerIn.yOld + (throwerIn.getY() - throwerIn.yOld) / 2.0 + throwerIn.getEyeHeight();
        double posZ = throwerIn.zOld + (throwerIn.getZ() - throwerIn.zOld) / 2.0;
        this.setPos(posX, posY, posZ);
        this.startPos = this.position();
        this.isTracerAmmo = isTracerAmmo;
        this.gunId = gunId;
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
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> AmmoParticleSpawner.addParticle(this, gunId));
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
        boolean headshot = result.isHeadshot();
        float damage = this.getDamage(result.getLocation());
        float headShotMultiplier = Math.max(this.headShot, 0);
        // 发布Pre事件
        var preEvent = new EntityHurtByGunEvent.Pre(entity, attacker, this.gunId, damage, headshot, headShotMultiplier, LogicalSide.SERVER);
        var cancelled = MinecraftForge.EVENT_BUS.post(preEvent);
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
            tacAttackEntity(parts, damage);
            // 恢复原位
            modifier.resetKnockBackStrength();
        } else {
            // 创建伤害
            tacAttackEntity(parts, damage);
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
                    MinecraftForge.EVENT_BUS.post(new EntityKillByGunEvent(livingCore, attacker, newGunId, headshot, LogicalSide.SERVER));
                    NetworkHandler.sendToDimension(new ServerMessageGunKill(livingCore.getId(), attackerId, newGunId, headshot), livingCore);
                } else {
                    MinecraftForge.EVENT_BUS.post(new EntityHurtByGunEvent.Post(livingCore, attacker, newGunId, damage, headshot, headShotMultiplier, LogicalSide.SERVER));
                    NetworkHandler.sendToDimension(new ServerMessageGunHurt(livingCore.getId(), attackerId, newGunId, damage, headshot, headShotMultiplier), livingCore);
                }
            }
        }
    }

    protected void onHitBlock(BlockHitResult result, Vec3 startVec, Vec3 endVec) {
        super.onHitBlock(result);
        if (result.getType() == HitResult.Type.MISS) {
            return;
        }
        Vec3 hitVec = result.getLocation();
        BlockPos pos = result.getBlockPos();
        // 触发事件
        if (MinecraftForge.EVENT_BUS.post(new AmmoHitBlockEvent(this.level(), result, this.level().getBlockState(pos), this))) {
            return;
        }
        // 爆炸
        if (this.explosion) {
            ExplodeUtil.createExplosion(this.getOwner(), this, this.explosionDamage, this.explosionRadius, this.explosionKnockback, this.explosionDestroyBlock, hitVec);
            // 爆炸直接结束不留弹孔，不处理之后的逻辑
            this.discard();
            return;
        }
        // 弹孔与点燃特效
        if (this.level() instanceof ServerLevel serverLevel) {
            BulletHoleOption bulletHoleOption = new BulletHoleOption(result.getDirection(), result.getBlockPos(), this.ammoId.toString(), this.gunId.toString());
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
        // 遍历进行判断
        double playerDistance = hitVec.distanceTo(this.startPos);
        for (DistanceDamagePair pair : this.damageAmount) {
            float effectiveDistance = this.damageAmount.get(0).getDistance() == pair.getDistance() ? this.distanceAmount : pair.getDistance();
            if (playerDistance < effectiveDistance) {
                float damage = pair.getDamage();
                return Math.max(damage * this.damageModifier, 0F);
            }
        }
        // 如果忘记写最大值，那我就直接认为你伤害为 0
        return 0;
    }

    private void tacAttackEntity(MaybeMultipartEntity parts, float damage) {
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
    }

    public ResourceLocation getAmmoId() {
        return ammoId;
    }

    public ResourceLocation getGunId() {
        return gunId;
    }

    public boolean isTracerAmmo() {
        return isTracerAmmo;
    }

    public RandomSource getRandom() {
        return this.random;
    }

    public Vec3 getOriginCameraPosition() {
        return originCameraPosition;
    }

    public void setOriginCameraPosition(Vec3 originCameraPosition) {
        this.originCameraPosition = originCameraPosition;
    }

    public Vec3 getOriginRenderOffset() {
        return originRenderOffset;
    }

    public void setOriginRenderOffset(Vec3 originRenderOffset) {
        this.originRenderOffset = originRenderOffset;
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
