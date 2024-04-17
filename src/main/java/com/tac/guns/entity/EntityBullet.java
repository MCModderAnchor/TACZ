package com.tac.guns.entity;

import com.tac.guns.api.entity.KnockBackModifier;
import com.tac.guns.config.common.AmmoConfig;
import com.tac.guns.particles.BulletHoleOption;
import com.tac.guns.resource.DefaultAssets;
import com.tac.guns.resource.pojo.data.gun.BulletData;
import com.tac.guns.util.explosion.ProjectileExplosion;
import com.tac.guns.util.math.TacHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public class EntityBullet extends Projectile implements IEntityAdditionalSpawnData {
    private static final Predicate<Entity> PROJECTILE_TARGETS = input -> input != null && input.isPickable() && !input.isSpectator();

    // TODO 目前是写死的穿透数据（穿透树叶、两种栅栏、栅栏门），之后可以调整至配置中（json 文件或者是 config 控制）
    private static final Predicate<BlockState> IGNORES = input -> input != null &&
            ((input.getBlock() instanceof LeavesBlock) ||
                    (input.getBlock() instanceof FenceBlock) ||
                    (input.getBlock() instanceof IronBarsBlock) ||
                    (input.getBlock() instanceof FenceGateBlock));

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
        // TODO 考虑增加一个阻力和点燃目标的控制，在原版中阻力默认为 0.01
        this.damageAmount = Mth.clamp(data.getDamageAmount(), 0, Float.MAX_VALUE);
        this.knockback = Mth.clamp(data.getKnockback(), 0, Float.MAX_VALUE);
        if (data.getExplosionData() != null) {
            this.hasExplosion = true;
            this.explosionDamage = Mth.clamp(data.getExplosionData().getRadius(), 0, Float.MAX_VALUE);
            this.explosionRadius = Mth.clamp(data.getExplosionData().getDamage(), 0, Float.MAX_VALUE);
        }
        // 子弹初始位置重置
        this.tickCount = 0;
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
        this.setYRot((float) (Mth.atan2(x, z) * (double) (180F / (float) Math.PI)));
        this.setXRot((float) (Mth.atan2(y, distance) * (double) (180F / (float) Math.PI)));
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
            for (int j = 0; j < 4; ++j) {
                this.level.addParticle(ParticleTypes.BUBBLE, nextPosX - x * 0.25F, nextPosY - y * 0.25F, nextPosZ - z * 0.25F, x, y, z);
            }
            // 在水中的阻力
            friction = 0.4F;
            gravity *= 0.6F;
        }
        // 重力与阻力更新速度状态
        this.setDeltaMovement(this.getDeltaMovement().scale(1F - friction));
        this.setDeltaMovement(this.getDeltaMovement().add(0, -gravity, 0));
        // 子弹生命结束
        if (!this.level.isClientSide()) {
            if (this.tickCount >= this.life - 1) {
                this.discard();
            }
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
            HitResult result = rayTraceBlocks(this.level, new ClipContext(startVec, endVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this), IGNORES);
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
                        if (hitEntityResult[j].hitVec.distanceTo(startVec) < hitEntityResult[k].hitVec.distanceTo(startVec))
                            k = j;
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
        // Test
        AABB oldBox = boundingBox;
        Vec3 velocity = new Vec3(entity.getX() - entity.xOld, entity.getY() - entity.yOld, entity.getZ() - entity.zOld);
        if (entity.getVehicle() != null) {
            boundingBox = boundingBox.move(velocity.multiply(-2.5, -2.5, -2.5));
//            boundingBox = boundingBox.expandTowards(velocity.multiply(1,1,1));
        }
        boundingBox = boundingBox.move(velocity.multiply(-5, -5, -5));
//        boundingBox = boundingBox.expandTowards(velocity.multiply(-1,-1,-1));
        // 计算射线与实体 boundingBox 的交点
        Vec3 hitPos = boundingBox.clip(startVec, endVec).orElse(null);

        boolean headshot = false;
        /*

        爆头判定预留区域

        */
        if (hitPos == null) {
            return null;
        }

        return new EntityResult(entity, hitPos, headshot);
    }

    protected void onHitEntity(TacHitResult result, Vec3 startVec, Vec3 endVec) {
        if (result.getEntity() instanceof LivingEntity entity) {
            // 点燃
            if (this.hasIgnite) {
                entity.setSecondsOnFire(2);
            }
            // 取消无敌时间
            entity.invulnerableTime = 0;
            // 取消击退效果
            KnockBackModifier modifier = KnockBackModifier.fromLivingEntity(entity);
            modifier.setKnockBackStrength(this.knockback);
            modifier.resetKnockBackStrength();
            // 造成伤害
            float damage = this.getDamage(result.getLocation());
            // TODO 暴击判定（不是爆头）
            /*

            暴击判定内部逻辑，需要输出一个是否暴击的 flag

            */
            // 爆头伤害判定
            if (result.isHeadshot()) {
                // TODO 更细节的自定义爆头伤害计算
                damage *= 2;
            }
            // 创建伤害
            tac_attackEntity(DamageSource.thrown(this, this.getOwner()), entity, damage);
            entity.invulnerableTime = 0;
            // 爆炸逻辑
            if (this.hasExplosion) {
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
        BlockState state = this.level.getBlockState(pos);
        Block block = state.getBlock();
        // 打碎玻璃（可以给个 Config 开关）
        if (state.getMaterial() == Material.GLASS) {
            this.level.destroyBlock(result.getBlockPos(), false, this.getOwner());
        }
        // 敲钟
        if (block instanceof BellBlock bell) {
            bell.attemptToRing(this.level, pos, result.getDirection());
        }
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
        // 点燃 TODO 需要增加一个控制是否引燃方块的 Config
        if (this.hasIgnite) {
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
        float initialDamage = this.damageAmount;
        double maxDistance = this.speed * (1 - Math.pow(1 - this.friction, this.life)) / this.friction;
        double toDistance = hitVec.distanceTo(this.startPos);
        // 伤害衰减计算逻辑（包含贴脸加伤）
        float modifier = 1;
        /*
        if (toDistance <= Math.min(Math.min(this.speed / 10, maxDistance / 100), 2)) {
            modifier = this.getGunCloseDamage() > 1 ? this.getGunCloseDamage() : 1;
        } else {
            float decayStartDistance;
            float decayEndDistance;
            float minDecayMultiplier;
            if (this.projectile.getGunDecayStart() > this.projectile.getGunDecayEnd() && this.projectile.getGunMinDecayMultiplier() > 1f) {
                decayStartDistance = (float) (Mth.clamp(this.projectile.getGunDecayEnd(), 0f, 1f) * maxDistance);
                decayEndDistance = (float) (Mth.clamp(this.projectile.getGunDecayStart(), 0f, 1f) * maxDistance);
                minDecayMultiplier = this.projectile.getGunMinDecayMultiplier();
            } else {
                decayStartDistance = (float) (Mth.clamp(this.projectile.getGunDecayStart(), 0f, 1f) * maxDistance);
                decayEndDistance = (float) (Mth.clamp(this.projectile.getGunDecayEnd(), 0f, 1f) * maxDistance);
                minDecayMultiplier = Mth.clamp(this.projectile.getGunMinDecayMultiplier(), 0f, 1f);
            }

            if (decayStartDistance == decayEndDistance)
                modifier = toDistance > decayEndDistance ? minDecayMultiplier : 1f;
            else
                modifier = (float) Mth.clamp(
                        (toDistance - decayEndDistance) * (1 - minDecayMultiplier) / (decayStartDistance - decayEndDistance) + minDecayMultiplier,
                        Math.min(minDecayMultiplier, 1f),
                        Math.max(minDecayMultiplier, 1f));
        }
        */
        initialDamage *= modifier;
        // 伤害除去弹丸数（关于霰弹枪这种一次性射出多发弹丸的武器的情况设置）
        float damage = initialDamage /* / this.getProjectileAmount() */;
        return Math.max(0F, damage);
    }

    private void tac_attackEntity(DamageSource source, Entity entity, float damage) {
        float damageToMcArmor = 0;
        float armorIgnore = 0.5F;
        if (entity instanceof EnderMan) {
            source.bypassInvul();
        }
        if (armorIgnore <= 1.0) {
            damageToMcArmor = damage * (1 - armorIgnore);
            entity.hurt(source, damageToMcArmor);
        }
        entity.invulnerableTime = 0;
        source.bypassArmor();
        source.bypassMagic();
        if (armorIgnore > 0.0) {
            entity.hurt(source, (damage - damageToMcArmor));
        }
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
        buffer.writeInt(this.life);
        buffer.writeFloat(this.speed);
        buffer.writeFloat(this.friction);
        buffer.writeInt(this.pierce);
        buffer.writeInt(this.tickCount);
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
        this.tickCount = additionalData.readInt();
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

    // TODO 看看能不能和 ProjectileExplosion 下面的 rayTraceBlocks 合并功能，这个是通过代码强制控制可选项，以提供 config 进行穿透设置，另一个是通过 json 文件设置穿透方块
    private static BlockHitResult rayTraceBlocks(Level level, ClipContext context, Predicate<BlockState> ignorePredicate) {
        return performRayTrace(context, (rayTraceContext, blockPos) -> {
            BlockState blockState = level.getBlockState(blockPos);
            if (ignorePredicate.test(blockState)) {
                return null;
            }
            // TODO 这里添加判断方块是否可以穿透，如果可以穿透则返回 null
            return getBlockHitResult(level, rayTraceContext, blockPos, blockState);
        }, (rayTraceContext) -> {
            Vec3 vec3 = rayTraceContext.getFrom().subtract(rayTraceContext.getTo());
            return BlockHitResult.miss(rayTraceContext.getTo(), Direction.getNearest(vec3.x, vec3.y, vec3.z), new BlockPos(rayTraceContext.getTo()));
        });
    }

    @javax.annotation.Nullable
    private static BlockHitResult getBlockHitResult(Level level, ClipContext rayTraceContext, BlockPos blockPos, BlockState blockState) {
        FluidState fluidState = level.getFluidState(blockPos);
        Vec3 startVec = rayTraceContext.getFrom();
        Vec3 endVec = rayTraceContext.getTo();
        VoxelShape blockShape = rayTraceContext.getBlockShape(blockState, level, blockPos);
        BlockHitResult blockResult = level.clipWithInteractionOverride(startVec, endVec, blockPos, blockShape, blockState);
        VoxelShape fluidShape = rayTraceContext.getFluidShape(fluidState, level, blockPos);
        BlockHitResult fluidResult = fluidShape.clip(startVec, endVec, blockPos);
        double blockDistance = blockResult == null ? Double.MAX_VALUE : rayTraceContext.getFrom().distanceToSqr(blockResult.getLocation());
        double fluidDistance = fluidResult == null ? Double.MAX_VALUE : rayTraceContext.getFrom().distanceToSqr(fluidResult.getLocation());
        return blockDistance <= fluidDistance ? blockResult : fluidResult;
    }

    private static <T> T performRayTrace(ClipContext context, BiFunction<ClipContext, BlockPos, T> hitFunction, Function<ClipContext, T> missFactory) {
        Vec3 startVec = context.getFrom();
        Vec3 endVec = context.getTo();
        if (!startVec.equals(endVec)) {
            double startX = Mth.lerp(-0.0000001, endVec.x, startVec.x);
            double startY = Mth.lerp(-0.0000001, endVec.y, startVec.y);
            double startZ = Mth.lerp(-0.0000001, endVec.z, startVec.z);
            double endX = Mth.lerp(-0.0000001, startVec.x, endVec.x);
            double endY = Mth.lerp(-0.0000001, startVec.y, endVec.y);
            double endZ = Mth.lerp(-0.0000001, startVec.z, endVec.z);

            int blockX = Mth.floor(endX);
            int blockY = Mth.floor(endY);
            int blockZ = Mth.floor(endZ);

            BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos(blockX, blockY, blockZ);
            T t = hitFunction.apply(context, mutablePos);
            if (t != null) {
                return t;
            }

            double deltaX = startX - endX;
            double deltaY = startY - endY;
            double deltaZ = startZ - endZ;
            int signX = Mth.sign(deltaX);
            int signY = Mth.sign(deltaY);
            int signZ = Mth.sign(deltaZ);
            double d9 = signX == 0 ? Double.MAX_VALUE : (double) signX / deltaX;
            double d10 = signY == 0 ? Double.MAX_VALUE : (double) signY / deltaY;
            double d11 = signZ == 0 ? Double.MAX_VALUE : (double) signZ / deltaZ;
            double d12 = d9 * (signX > 0 ? 1.0D - Mth.frac(endX) : Mth.frac(endX));
            double d13 = d10 * (signY > 0 ? 1.0D - Mth.frac(endY) : Mth.frac(endY));
            double d14 = d11 * (signZ > 0 ? 1.0D - Mth.frac(endZ) : Mth.frac(endZ));

            while (d12 <= 1.0D || d13 <= 1.0D || d14 <= 1.0D) {
                if (d12 < d13) {
                    if (d12 < d14) {
                        blockX += signX;
                        d12 += d9;
                    } else {
                        blockZ += signZ;
                        d14 += d11;
                    }
                } else if (d13 < d14) {
                    blockY += signY;
                    d13 += d10;
                } else {
                    blockZ += signZ;
                    d14 += d11;
                }

                T t1 = hitFunction.apply(context, mutablePos.set(blockX, blockY, blockZ));
                if (t1 != null) {
                    return t1;
                }
            }
        }
        return missFactory.apply(context);
    }

    public static class EntityResult {
        private Entity entity;
        private Vec3 hitVec;
        private boolean headshot;

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
