package com.tacz.guns.entity;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.entity.ITargetEntity;
import com.tacz.guns.api.entity.KnockBackModifier;
import com.tacz.guns.api.event.common.LivingHurtByGunEvent;
import com.tacz.guns.api.event.common.LivingKillByGunEvent;
import com.tacz.guns.api.event.server.AmmoHitBlockEvent;
import com.tacz.guns.client.particle.AmmoParticleSpawner;
import com.tacz.guns.config.common.AmmoConfig;
import com.tacz.guns.config.sync.SyncConfig;
import com.tacz.guns.config.util.HeadShotAABBConfigRead;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ServerMessageGunHurt;
import com.tacz.guns.network.message.ServerMessageGunKill;
import com.tacz.guns.particles.BulletHoleOption;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import com.tacz.guns.resource.pojo.data.gun.ExtraDamage;
import com.tacz.guns.util.HitboxHelper;
import com.tacz.guns.util.TacHitResult;
import com.tacz.guns.util.block.BlockRayTrace;
import com.tacz.guns.util.block.ProjectileExplosion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

/**
 * 动能武器打出的子弹实体。
 */
public class EntityKineticBullet extends Projectile implements IEntityAdditionalSpawnData {
    public static final EntityType<EntityKineticBullet> TYPE = EntityType.Builder.<EntityKineticBullet>of(EntityKineticBullet::new, MobCategory.MISC).noSummon().noSave().fireImmune().sized(0.0625F, 0.0625F).clientTrackingRange(5).updateInterval(5).setShouldReceiveVelocityUpdates(false).build("bullet");
    private static final Predicate<Entity> PROJECTILE_TARGETS = input -> input != null && input.isPickable() && !input.isSpectator();
    private ResourceLocation ammoId = DefaultAssets.EMPTY_AMMO_ID;
    private int life = 200;
    private float speed = 1;
    private float gravity = 0;
    private float friction = 0.01F;
    private float damageAmount = 5;
    private float knockback = 0;
    private boolean hasExplosion = false;
    private boolean hasIgnite = false;
    private int igniteEntityTime = 2;
    private float explosionDamage = 3;
    private float explosionRadius = 3;
    private boolean explosionKnockback = false;
    private ExtraDamage extraDamage = null;
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

    public EntityKineticBullet(EntityType<? extends Projectile> type, Level worldIn) {
        super(type, worldIn);
    }

    public EntityKineticBullet(EntityType<? extends Projectile> type, double x, double y, double z, Level worldIn) {
        this(type, worldIn);
        this.setPos(x, y, z);
    }

    public EntityKineticBullet(Level worldIn, LivingEntity throwerIn, ResourceLocation ammoId, ResourceLocation gunId, boolean isTracerAmmo, BulletData data) {
        this(TYPE, throwerIn.getX(), throwerIn.getEyeY() - (double) 0.1F, throwerIn.getZ(), worldIn);
        this.setOwner(throwerIn);
        this.ammoId = ammoId;
        this.life = Mth.clamp((int) (data.getLifeSecond() * 20), 1, Integer.MAX_VALUE);
        // 限制最大弹速为 600 m / s，以减轻计算负担
        this.speed = Mth.clamp(data.getSpeed() / 20, 0, 30);
        this.gravity = Mth.clamp(data.getGravity(), 0, Float.MAX_VALUE);
        this.friction = Mth.clamp(data.getFriction(), 0, Float.MAX_VALUE);
        this.hasIgnite = data.isHasIgnite();
        this.igniteEntityTime = Math.max(data.getIgniteEntityTime(), 0);
        this.damageAmount = (float) Mth.clamp(data.getDamageAmount() * SyncConfig.DAMAGE_BASE_MULTIPLIER.get(), 0, Double.MAX_VALUE);
        // 霰弹情况，每个伤害要扣去
        if (data.getBulletAmount() > 1) {
            this.damageModifier = 1f / data.getBulletAmount();
        }
        this.knockback = Mth.clamp(data.getKnockback(), 0, Float.MAX_VALUE);
        this.pierce = Mth.clamp(data.getPierce(), 1, Integer.MAX_VALUE);
        this.extraDamage = data.getExtraDamage();
        if (data.getExplosionData() != null) {
            this.hasExplosion = true;
            this.explosionDamage = (float) Mth.clamp(data.getExplosionData().getDamage() * SyncConfig.DAMAGE_BASE_MULTIPLIER.get(), 0, Float.MAX_VALUE);
            this.explosionRadius = Mth.clamp(data.getExplosionData().getRadius(), 0, Float.MAX_VALUE);
            this.explosionKnockback = data.getExplosionData().isKnockback();
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

    public static void createExplosion(Entity owner, Entity exploder, float damage, float radius, boolean knockback, Vec3 hitPos) {
        // 客户端不执行
        if (!(exploder.level instanceof ServerLevel level)) {
            return;
        }
        // 依据配置文件读取方块破坏方式
        Explosion.BlockInteraction mode = Explosion.BlockInteraction.NONE;
        if (AmmoConfig.EXPLOSIVE_AMMO_DESTROYS_BLOCKS.get()) {
            mode = Explosion.BlockInteraction.BREAK;
        }
        // 创建爆炸
        ProjectileExplosion explosion = new ProjectileExplosion(level, owner, exploder, null, null, hitPos.x(), hitPos.y(), hitPos.z(), damage, radius, knockback, mode);
        // 监听 forge 事件
        if (ForgeEventFactory.onExplosionStart(level, explosion)) {
            return;
        }
        // 执行爆炸逻辑
        explosion.explode();
        explosion.finalizeExplosion(true);
        if (mode == Explosion.BlockInteraction.NONE) {
            explosion.clearToBlow();
        }
        // 客户端发包，发送爆炸相关信息
        level.players().stream().filter(player -> Mth.sqrt((float) player.distanceToSqr(hitPos)) < AmmoConfig.EXPLOSIVE_AMMO_VISIBLE_DISTANCE.get()).forEach(player -> {
            ClientboundExplodePacket packet = new ClientboundExplodePacket(hitPos.x(), hitPos.y(), hitPos.z(), radius, explosion.getToBlow(), explosion.getHitPlayers().get(player));
            player.connection.send(packet);
        });
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
        if (this.level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> AmmoParticleSpawner.addParticle(level, this));
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
                this.level.addParticle(ParticleTypes.BUBBLE, nextPosX - x * 0.25F, nextPosY - y * 0.25F, nextPosZ - z * 0.25F, x, y, z);
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
        if (!this.level.isClientSide()) {
            // 子弹在 tick 起始的位置
            Vec3 startVec = this.position();
            // 子弹在 tick 结束的位置
            Vec3 endVec = startVec.add(this.getDeltaMovement());
            // 子弹的碰撞检测
            HitResult result = BlockRayTrace.rayTraceBlocks(this.level, new ClipContext(startVec, endVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            BlockHitResult resultB = (BlockHitResult) result;
            if (resultB.getType() != HitResult.Type.MISS) {
                // 子弹击中方块时，设置击中方块的位置为子弹的结束位置
                endVec = resultB.getLocation();
            }

            List<EntityResult> hitEntities = null;
            // 子弹的击中检测，穿透为 1 或者爆炸类弹药限制为一个实体穿透判定
            if (this.pierce <= 1 || this.hasExplosion) {
                EntityResult entityResult = this.findEntityOnPath(startVec, endVec);
                // 将单个命中是实体创建为单个内容的 list
                if (entityResult != null) {
                    hitEntities = Collections.singletonList(entityResult);
                }
            } else {
                hitEntities = this.findEntitiesOnPath(startVec, endVec);
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
                    if (this.pierce < 1 || this.hasExplosion) {
                        // 子弹已经穿透所有实体，结束子弹的飞行
                        this.discard();
                        return;
                    }
                }
            }
            this.onHitBlock(resultB, startVec, endVec);
        }
    }

    @Nullable
    protected EntityResult findEntityOnPath(Vec3 startVec, Vec3 endVec) {
        Vec3 hitVec = null;
        Entity hitEntity = null;
        boolean headshot = false;
        // 获取子弹 tick 路径上所有的实体
        List<Entity> entities = this.level.getEntities(this, this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(1.0), PROJECTILE_TARGETS);
        double closestDistance = Double.MAX_VALUE;
        Entity owner = this.getOwner();
        for (Entity entity : entities) {
            // 禁止对自己造成伤害（如有需要可以增加 Config 开启对自己的伤害）
            if (!entity.equals(owner)) {
                // 射击无视自己的载具
                if (owner != null && entity.equals(owner.getVehicle())) {
                    continue;
                }
                EntityResult result = this.getHitResult(entity, startVec, endVec);
                if (result == null) {
                    continue;
                }
                Vec3 hitPos = result.getHitPos();
                double distanceToHit = startVec.distanceTo(hitPos);
                if (entity.isAlive()) {
                    if (distanceToHit < closestDistance) {
                        hitVec = hitPos;
                        hitEntity = entity;
                        closestDistance = distanceToHit;
                        headshot = result.isHeadshot();
                    }
                }
            }
        }
        return hitEntity != null ? new EntityResult(hitEntity, hitVec, headshot) : null;
    }

    @Nullable
    protected List<EntityResult> findEntitiesOnPath(Vec3 startVec, Vec3 endVec) {
        List<EntityResult> hitEntities = new ArrayList<>();
        List<Entity> entities = this.level.getEntities(this, this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(1.0), PROJECTILE_TARGETS);
        Entity owner = this.getOwner();
        for (Entity entity : entities) {
            if (!entity.equals(owner)) {
                if (owner != null && entity.equals(owner.getVehicle())) {
                    continue;
                }
                EntityResult result = this.getHitResult(entity, startVec, endVec);
                if (result == null) {
                    continue;
                }
                if (entity.isAlive()) {
                    hitEntities.add(result);
                }
            }
        }
        return hitEntities;
    }

    @Nullable
    protected EntityResult getHitResult(Entity entity, Vec3 startVec, Vec3 endVec) {
        AABB boundingBox = HitboxHelper.getFixedBoundingBox(entity, this.getOwner());
        // 计算射线与实体 boundingBox 的交点
        Vec3 hitPos = boundingBox.clip(startVec, endVec).orElse(null);
        // 爆头判定
        if (hitPos == null) {
            return null;
        }
        Vec3 hitBoxPos = hitPos.subtract(entity.position());
        ResourceLocation entityId = ForgeRegistries.ENTITIES.getKey(entity.getType());
        // 有配置的调用配置
        if (entityId != null) {
            AABB aabb = HeadShotAABBConfigRead.getAABB(entityId);
            if (aabb != null) {
                return new EntityResult(entity, hitPos, aabb.contains(hitBoxPos));
            }
        }
        // 没有配置的默认给一个
        boolean headshot = false;
        float eyeHeight = entity.getEyeHeight();
        if ((eyeHeight - 0.25) < hitBoxPos.y && hitBoxPos.y < (eyeHeight + 0.25)) {
            headshot = true;
        }
        return new EntityResult(entity, hitPos, headshot);
    }

    protected void onHitEntity(TacHitResult result, Vec3 startVec, Vec3 endVec) {
        if (result.getEntity() instanceof ITargetEntity targetEntity) {
            targetEntity.onProjectileHit(this, result, DamageSource.thrown(this, this.getOwner()), this.getDamage(result.getLocation()));
            // 打靶直接返回
            return;
        }
        Entity entity = result.getEntity();
        // 点燃
        if (this.hasIgnite && AmmoConfig.IGNITE_ENTITY.get()) {
            entity.setSecondsOnFire(this.igniteEntityTime);
        }
        // 获取伤害
        float damage = this.getDamage(result.getLocation());
        // TODO 暴击判定（不是爆头）暴击判定内部逻辑，需要输出一个是否暴击的 flag
        boolean headshot = result.isHeadshot();
        if (headshot) {
            // 默认爆头伤害是 1x
            float headShotMultiplier = 1f;
            if (this.extraDamage != null && this.extraDamage.getHeadShotMultiplier() > 0) {
                headShotMultiplier = (float) (this.extraDamage.getHeadShotMultiplier() * SyncConfig.HEAD_SHOT_BASE_MULTIPLIER.get());
            }
            damage *= headShotMultiplier;
        }
        @Nullable Entity owner = this.getOwner();
        // 对 LivingEntity 进行击退强度的自定义
        if (entity instanceof LivingEntity livingEntity) {
            // 取消击退效果，设定自己的击退强度
            KnockBackModifier modifier = KnockBackModifier.fromLivingEntity(livingEntity);
            modifier.setKnockBackStrength(this.knockback);
            // 创建伤害
            tacAttackEntity(DamageSource.thrown(this, owner), entity, damage);
            // 恢复原位
            modifier.resetKnockBackStrength();
        } else {
            // 创建伤害
            tacAttackEntity(DamageSource.thrown(this, owner), entity, damage);
        }
        // 爆炸逻辑
        if (this.hasExplosion) {
            // 取消无敌时间
            entity.invulnerableTime = 0;
            createExplosion(this.getOwner(), this, this.explosionDamage, this.explosionRadius, this.explosionKnockback, result.getLocation());
        }
        LivingEntity livingEntity = entity instanceof LivingEntity ? (LivingEntity) entity : null;
        if (entity instanceof PartEntity<?> partEntity) {
            livingEntity = partEntity.getParent() instanceof LivingEntity ? (LivingEntity) partEntity.getParent() : null;
        }
        // 只对 LivingEntity 执行击杀判定
        if (livingEntity != null) {
            // 事件同步，从服务端到客户端
            if (!level.isClientSide) {
                LivingEntity attacker = owner instanceof LivingEntity ? (LivingEntity) owner : null;
                int attackerId = attacker == null ? 0 : attacker.getId();
                // 如果生物死了
                if (livingEntity.isDeadOrDying()) {
                    MinecraftForge.EVENT_BUS.post(new LivingKillByGunEvent(livingEntity, attacker, this.gunId, headshot, LogicalSide.SERVER));
                    NetworkHandler.sendToNearby(entity, new ServerMessageGunKill(entity.getId(), attackerId, this.gunId, headshot));
                } else {
                    MinecraftForge.EVENT_BUS.post(new LivingHurtByGunEvent(livingEntity, attacker, this.gunId, damage, headshot, LogicalSide.SERVER));
                    NetworkHandler.sendToNearby(entity, new ServerMessageGunHurt(entity.getId(), attackerId, this.gunId, damage, headshot));
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
        MinecraftForge.EVENT_BUS.post(new AmmoHitBlockEvent(level, result, this.level.getBlockState(pos), this));
        // 爆炸
        if (this.hasExplosion) {
            createExplosion(this.getOwner(), this, this.explosionDamage, this.explosionRadius, this.explosionKnockback, hitVec);
            // 爆炸直接结束不留弹孔，不处理之后的逻辑
            this.discard();
            return;
        }
        // 弹孔与点燃特效
        if (this.level instanceof ServerLevel serverLevel) {
            BulletHoleOption bulletHoleOption = new BulletHoleOption(result.getDirection(), result.getBlockPos());
            serverLevel.sendParticles(bulletHoleOption, hitVec.x, hitVec.y, hitVec.z, 1, 0, 0, 0, 0);
            if (this.hasIgnite) {
                serverLevel.sendParticles(ParticleTypes.LAVA, hitVec.x, hitVec.y, hitVec.z, 1, 0, 0, 0, 0);
            }
        }
        if (this.hasIgnite && AmmoConfig.IGNITE_BLOCK.get()) {
            BlockPos offsetPos = pos.relative(result.getDirection());
            if (BaseFireBlock.canBePlacedAt(this.level, offsetPos, result.getDirection())) {
                BlockState fireState = BaseFireBlock.getState(this.level, offsetPos);
                this.level.setBlock(offsetPos, fireState, Block.UPDATE_ALL_IMMEDIATE);
                ((ServerLevel) this.level).sendParticles(ParticleTypes.LAVA, hitVec.x - 1.0 + this.random.nextDouble() * 2.0, hitVec.y, hitVec.z - 1.0 + this.random.nextDouble() * 2.0, 4, 0, 0, 0, 0);
            }
        }
        this.discard();
    }

    // 根据距离进行伤害衰减设计
    public float getDamage(Vec3 hitVec) {
        // 如果没有额外伤害，直接原样返回
        if (this.extraDamage == null) {
            return Math.max(0F, this.damageAmount * this.damageModifier);
        }
        // 调用距离伤害函数进行具体伤害计算
        var damageDecay = extraDamage.getDamageAdjust();
        // 距离伤害函数为空，直接全程默认伤害
        if (damageDecay == null || damageDecay.isEmpty()) {
            return Math.max(0F, this.damageAmount * this.damageModifier);
        }
        // 遍历进行判断
        double playerDistance = hitVec.distanceTo(this.startPos);
        for (ExtraDamage.DistanceDamagePair pair : damageDecay) {
            if (playerDistance < pair.getDistance()) {
                return (float) (Math.max(0F, pair.getDamage() * SyncConfig.DAMAGE_BASE_MULTIPLIER.get()) * this.damageModifier);
            }
        }
        // 如果忘记写最大值，那我就直接认为你伤害为 0
        return 0;
    }

    private void tacAttackEntity(DamageSource source, Entity entity, float damage) {
        float armorIgnore = 0;
        if (this.extraDamage != null && this.extraDamage.getArmorIgnore() > 0) {
            armorIgnore = (float) (this.extraDamage.getArmorIgnore() * SyncConfig.ARMOR_IGNORE_BASE_MULTIPLIER.get());
        }
        // 给末影人造成伤害
        if (entity instanceof EnderMan) {
            source.bypassInvul();
        }
        // 穿甲伤害和普通伤害的比例计算
        float armorDamagePercent = Mth.clamp(armorIgnore, 0.0F, 1.0F);
        float normalDamagePercent = 1 - armorDamagePercent;
        // 取消无敌时间
        entity.invulnerableTime = 0;
        // 普通伤害
        entity.hurt(source, damage * normalDamagePercent);
        // 穿甲伤害
        source.bypassArmor();
        source.bypassMagic();
        // 取消无敌时间
        entity.invulnerableTime = 0;
        entity.hurt(source, damage * armorDamagePercent);
    }

    @Override
    public Packet<?> getAddEntityPacket() {
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
        buffer.writeBoolean(this.hasExplosion);
        buffer.writeBoolean(this.hasIgnite);
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
        Entity entity = this.level.getEntity(additionalData.readInt());
        if (entity != null) {
            this.setOwner(entity);
        }
        this.ammoId = additionalData.readResourceLocation();
        this.gravity = additionalData.readFloat();
        this.hasExplosion = additionalData.readBoolean();
        this.hasIgnite = additionalData.readBoolean();
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

    public boolean isTracerAmmo() {
        return isTracerAmmo;
    }

    public Random getRandom() {
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
