package com.tac.guns.entity;

import com.tac.guns.api.entity.KnockBackModifier;
import com.tac.guns.api.event.AmmoHitBlockEvent;
import com.tac.guns.config.common.AmmoConfig;
import com.tac.guns.network.NetworkHandler;
import com.tac.guns.network.message.ServerMessageHeadShot;
import com.tac.guns.particles.BulletHoleOption;
import com.tac.guns.resource.DefaultAssets;
import com.tac.guns.resource.pojo.data.gun.BulletData;
import com.tac.guns.resource.pojo.data.gun.ExtraDamage;
import com.tac.guns.util.block.BlockRayTrace;
import com.tac.guns.util.block.ProjectileExplosion;
import com.tac.guns.util.math.TacHitResult;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class EntityBullet extends Projectile implements IEntityAdditionalSpawnData {
    private static final Predicate<Entity> PROJECTILE_TARGETS = input -> input != null && input.isPickable() && !input.isSpectator();
    public static final EntityType<EntityBullet> TYPE = EntityType.Builder.<EntityBullet>of(EntityBullet::new, MobCategory.MISC)
            .noSummon()
            .noSave()
            .fireImmune()
            .sized(0.0625F, 0.0625F)
            .clientTrackingRange(5)
            .updateInterval(5)
            .setShouldReceiveVelocityUpdates(false)
            .build("bullet");

    private ResourceLocation ammoId = DefaultAssets.EMPTY_AMMO_ID;
    private int life = 200;
    private float speed = 1;
    private float gravity = 0;
    private float friction = 0.01F;
    private float damageAmount = 5;
    private float knockback = 0;
    private boolean hasExplosion = false;
    private boolean hasIgnite = false;
    private float explosionDamage = 3;
    private float explosionRadius = 3;
    private ExtraDamage extraDamage = null;
    // 穿透数
    private int pierce = 1;
    // 初始位置
    private Vec3 startPos;

    public EntityBullet(EntityType<? extends Projectile> type, Level worldIn) {
        super(type, worldIn);
    }

    public EntityBullet(EntityType<? extends Projectile> type, double x, double y, double z, Level worldIn) {
        this(type, worldIn);
        this.setPos(x, y, z);
    }

    public EntityBullet(Level worldIn, LivingEntity throwerIn, ResourceLocation ammoId, BulletData data) {
        this(TYPE, throwerIn.getX(), throwerIn.getEyeY() - (double) 0.1F, throwerIn.getZ(), worldIn);
        this.setOwner(throwerIn);
        this.ammoId = ammoId;
        this.life = Mth.clamp((int) (data.getLifeSecond() * 20), 1, Integer.MAX_VALUE);
        this.speed = Mth.clamp(data.getSpeed() / 20, 0, Float.MAX_VALUE);
        this.gravity = Mth.clamp(data.getGravity(), 0, Float.MAX_VALUE);
        this.friction = Mth.clamp(data.getFriction(), 0, Float.MAX_VALUE);
        this.hasIgnite = data.isHasIgnite();
        this.damageAmount = Mth.clamp(data.getDamageAmount(), 0, Float.MAX_VALUE);
        this.knockback = Mth.clamp(data.getKnockback(), 0, Float.MAX_VALUE);
        this.extraDamage = data.getExtraDamage();
        if (data.getExplosionData() != null) {
            this.hasExplosion = true;
            this.explosionDamage = Mth.clamp(data.getExplosionData().getRadius(), 0, Float.MAX_VALUE);
            this.explosionRadius = Mth.clamp(data.getExplosionData().getDamage(), 0, Float.MAX_VALUE);
        }
        // 子弹初始位置重置
        double posX = throwerIn.xOld + (throwerIn.getX() - throwerIn.xOld) / 2.0;
        double posY = throwerIn.yOld + (throwerIn.getY() - throwerIn.yOld) / 2.0 + throwerIn.getEyeHeight();
        double posZ = throwerIn.zOld + (throwerIn.getZ() - throwerIn.zOld) / 2.0;
        this.setPos(posX, posY, posZ);
        this.startPos = this.position();
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();
        // 调用 TaC 子弹服务器事件
        this.onBulletTick();
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
            if (hitEntities != null) {
                EntityResult[] hitEntityResult = hitEntities.toArray(new EntityResult[0]);
                // 对被命中的实体进行排序，按照距离子弹发射位置的距离进行升序排序
                for (int i = 0; i < this.pierce || i < 1; i++) {
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
                }
            } else {
                this.onHitBlock(resultB, startVec, endVec);
            }
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
        for (Entity entity : entities) {
            // 禁止对自己造成伤害（如有需要可以增加 Config 开启对自己的伤害）
            if (!entity.equals(this.getOwner())) {
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
        for (Entity entity : entities) {
            if (!entity.equals(this.getOwner())) {
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
        double expandHeight = entity instanceof Player && !entity.isCrouching() ? 0.0625 : 0.0;
        AABB boundingBox = entity.getBoundingBox();
        boundingBox = boundingBox.expandTowards(0, expandHeight, 0);
        Vec3 velocity = new Vec3(entity.getX() - entity.xOld, entity.getY() - entity.yOld, entity.getZ() - entity.zOld);
        if (entity.getVehicle() != null) {
            boundingBox = boundingBox.move(velocity.multiply(-2.5, -2.5, -2.5));
        }
        boundingBox = boundingBox.move(velocity.multiply(-5, -5, -5));
        // 计算射线与实体 boundingBox 的交点
        Vec3 hitPos = boundingBox.clip(startVec, endVec).orElse(null);
        // 爆头判定
        boolean headshot = false;
        if (hitPos == null) {
            return null;
        }
        // TODO 默认爆头判定
        Vec3 hitBoxPos = hitPos.subtract(entity.position());
        float eyeHeight = entity.getEyeHeight();
        if ((eyeHeight - 0.25) < hitBoxPos.y && hitBoxPos.y < (eyeHeight + 0.25)) {
            headshot = true;
        }
        return new EntityResult(entity, hitPos, headshot);
    }

    protected void onHitEntity(TacHitResult result, Vec3 startVec, Vec3 endVec) {
        if (result.getEntity() instanceof LivingEntity entity) {
            // 点燃
            if (this.hasIgnite && AmmoConfig.IGNITE_BLOCK.get()) {
                entity.setSecondsOnFire(2);
            }
            // 取消无敌时间
            entity.invulnerableTime = 0;
            // 造成伤害
            float damage = this.getDamage(result.getLocation());
            // TODO 暴击判定（不是爆头）暴击判定内部逻辑，需要输出一个是否暴击的 flag
            Entity owner = this.getOwner();
            if (result.isHeadshot()) {
                // 发送爆头提示
                if (owner instanceof Player player) {
                    NetworkHandler.sendToClientPlayer(new ServerMessageHeadShot(), player);
                }
                float headShotMultiplier = 2f;
                if (this.extraDamage != null && this.extraDamage.getHeadShotMultiplier() > 0) {
                    headShotMultiplier = (float) (this.extraDamage.getHeadShotMultiplier() * AmmoConfig.HEAD_SHOT_BASE_MULTIPLIER.get());
                }
                damage *= headShotMultiplier;
            }
            // 取消击退效果，设定自己的击退强度
            KnockBackModifier modifier = KnockBackModifier.fromLivingEntity(entity);
            modifier.setKnockBackStrength(this.knockback);
            // 创建伤害
            tacAttackEntity(DamageSource.thrown(this, owner), entity, damage);
            // 恢复原位
            modifier.resetKnockBackStrength();
            // 爆炸逻辑
            if (this.hasExplosion) {
                // 取消无敌时间
                entity.invulnerableTime = 0;
                createExplosion(this, this.explosionDamage, this.explosionRadius, result.getLocation());
                // 爆炸直接结束子弹生命周期，不计算穿透
                this.discard();
            }
        }
        this.discard();
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
            createExplosion(this, this.explosionDamage, this.explosionRadius, hitVec);
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
                this.level.setBlock(offsetPos, fireState, 11);
                ((ServerLevel) this.level).sendParticles(ParticleTypes.LAVA, hitVec.x - 1.0 + this.random.nextDouble() * 2.0, hitVec.y, hitVec.z - 1.0 + this.random.nextDouble() * 2.0, 4, 0, 0, 0, 0);
            }
        }
        this.discard();
    }

    // 根据距离进行伤害衰减设计
    public float getDamage(Vec3 hitVec) {
        // 如果没有额外伤害，直接原样返回
        if (this.extraDamage == null) {
            return Math.max(0F, this.damageAmount);
        }
        // 有？那就正常计算
        float initialDamage = this.damageAmount;
        double maxDistance = this.speed * (1 - Math.pow(1 - this.friction, this.life)) / this.friction;
        double playerDistance = hitVec.distanceTo(this.startPos);
        // 伤害衰减计算逻辑（包含贴脸加伤）
        float modifier;
        ExtraDamage.Close damageClose = extraDamage.getClose();
        ExtraDamage.Decay damageDecay = extraDamage.getDecay();
        // 以防万一检查一下
        if (damageClose == null || damageDecay == null) {
            return Math.max(0F, this.damageAmount);
        }
        // 抵近伤害的判断
        if (playerDistance <= damageClose.getRangeMeters()) {
            modifier = Math.max(damageClose.getDamageMultiplier(), 1);
        }
        // 远距伤害的判断
        else {
            float[] rangePercent = damageDecay.getRangePercent();
            // 以防万一检查一下
            if (rangePercent.length < 2) {
                return Math.max(0F, this.damageAmount);
            }
            // 数据检查
            float decayStartDistance;
            float decayEndDistance;
            float minDecayMultiplier;
            if (rangePercent[0] > rangePercent[1] && damageDecay.getMinDamageMultiplier() > 1f) {
                decayStartDistance = (float) (Mth.clamp(rangePercent[1], 0f, 1f) * maxDistance);
                decayEndDistance = (float) (Mth.clamp(rangePercent[0], 0f, 1f) * maxDistance);
                minDecayMultiplier = damageDecay.getMinDamageMultiplier();
            } else {
                decayStartDistance = (float) (Mth.clamp(rangePercent[0], 0f, 1f) * maxDistance);
                decayEndDistance = (float) (Mth.clamp(rangePercent[1], 0f, 1f) * maxDistance);
                minDecayMultiplier = Mth.clamp(damageDecay.getMinDamageMultiplier(), 0f, 1f);
            }
            // 判断伤害
            if (decayStartDistance == decayEndDistance) {
                modifier = playerDistance > decayEndDistance ? minDecayMultiplier : 1f;
            } else {
                double value = (playerDistance - decayEndDistance) * (1 - minDecayMultiplier) / (decayStartDistance - decayEndDistance) + minDecayMultiplier;
                modifier = (float) Mth.clamp(value, Math.min(minDecayMultiplier, 1f), Math.max(minDecayMultiplier, 1f));
            }
        }
        initialDamage *= modifier;
        // 伤害除去弹丸数（关于霰弹枪这种一次性射出多发弹丸的武器的情况设置）
        // this.getProjectileAmount()
        float damage = initialDamage;
        return Math.max(0F, damage);
    }

    private void tacAttackEntity(DamageSource source, Entity entity, float damage) {
        float armorIgnore = 0;
        if (this.extraDamage != null && this.extraDamage.getArmorIgnore() > 0) {
            armorIgnore = (float) (this.extraDamage.getArmorIgnore() * AmmoConfig.ARMOR_IGNORE_BASE_MULTIPLIER.get());
        }
        // FIXME 阻止末影人传送，并不起作用
        if (entity instanceof EnderMan) {
            source.bypassInvul();
        }
        // 穿甲伤害和普通伤害的比例计算
        float armorDamagePercent = Mth.clamp(armorIgnore, 0.0F, 1.0F);
        float normalDamagePercent = 1 - armorDamagePercent;
        // 普通伤害
        entity.hurt(source, damage * normalDamagePercent);
        // 穿甲伤害
        source.bypassArmor();
        source.bypassMagic();
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
    }

    public ResourceLocation getAmmoId() {
        return ammoId;
    }

    @Override
    public boolean ownedBy(@Nullable Entity entity) {
        if (entity == null) {
            return false;
        }
        return super.ownedBy(entity);
    }

    public static void createExplosion(Entity exploder, float damage, float radius, Vec3 hitPos) {
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
        ProjectileExplosion explosion = new ProjectileExplosion(level, exploder, null, null,
                hitPos.x(), hitPos.y(), hitPos.z(), damage, radius, mode);
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
            ClientboundExplodePacket packet = new ClientboundExplodePacket(hitPos.x(), hitPos.y(), hitPos.z(),
                    radius, explosion.getToBlow(), explosion.getHitPlayers().get(player));
            player.connection.send(packet);
        });
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
