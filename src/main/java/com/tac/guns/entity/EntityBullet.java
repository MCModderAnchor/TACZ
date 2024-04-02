package com.tac.guns.entity;

import com.tac.guns.api.entity.IGunOperator;
import com.tac.guns.config.common.AmmoConfig;
import com.tac.guns.particles.BulletHoleOption;
import com.tac.guns.resource.DefaultAssets;
import com.tac.guns.resource.pojo.data.gun.BulletData;
import com.tac.guns.util.explosion.ProjectileExplosion;
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
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EntityBullet extends ThrowableProjectile implements IEntityAdditionalSpawnData {
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
    private float gravity = 0;
    private float damageAmount = 5;
    private float knockback = 0;
    private boolean hasExplosion = false;
    private float explosionDamage = 3;
    private float explosionRadius = 3;

    public EntityBullet(EntityType<? extends ThrowableProjectile> type, Level worldIn) {
        super(type, worldIn);
    }

    public EntityBullet(Level worldIn, LivingEntity throwerIn, ResourceLocation ammoId, BulletData data) {
        super(TYPE, throwerIn, worldIn);
        this.ammoId = ammoId;
        this.life = Mth.clamp((int) (data.getLifeSecond() * 20), 1, Integer.MAX_VALUE);
        this.gravity = Mth.clamp(data.getGravity(), 0, Float.MAX_VALUE);
        this.damageAmount = Mth.clamp(data.getDamageAmount(), 0, Float.MAX_VALUE);
        this.knockback = Mth.clamp(data.getKnockback(), 0, Float.MAX_VALUE);
        if (data.getExplosionData() != null) {
            this.hasExplosion = true;
            this.explosionDamage = Mth.clamp(data.getExplosionData().getRadius(), 0, Float.MAX_VALUE);
            this.explosionRadius = Mth.clamp(data.getExplosionData().getDamage(), 0, Float.MAX_VALUE);
        }
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount > life) {
            this.discard();
        }
    }

    protected Vec3 hitEntityPos(Entity entity) {
        Vec3 startPos = this.position();
        Vec3 endPos = this.position().add(this.getDeltaMovement());
        return entity.getBoundingBox().clip(startPos, endPos).orElse(entity.position());
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (result.getEntity() instanceof LivingEntity livingEntity) {
            // 取消无敌时间
            livingEntity.invulnerableTime = 0;
            // 取消击退效果
            if (livingEntity instanceof IGunOperator operator) {
                operator.setKnockbackStrength(this.knockback);
                livingEntity.hurt(DamageSource.thrown(this, this.getOwner()), this.damageAmount);
                operator.resetKnockbackStrength();
                if (this.hasExplosion) {
                    createExplosion(this, this.explosionDamage, this.explosionRadius, hitEntityPos(livingEntity));
                }
            }
        }
        super.onHitEntity(result);
        this.discard();
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult hitResult) {
        Vec3 location = hitResult.getLocation();
        if (this.hasExplosion) {
            createExplosion(this, this.explosionDamage, this.explosionRadius, location);
        }
        super.onHitBlock(hitResult);

        if (this.level instanceof ServerLevel serverLevel) {
            BulletHoleOption bulletHoleOption = new BulletHoleOption(hitResult.getDirection(), hitResult.getBlockPos());
            serverLevel.sendParticles(bulletHoleOption, location.x, location.y, location.z, 1, 0, 0, 0, 0);
        }
        this.discard();
    }

    @Override
    protected float getGravity() {
        return this.gravity;
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
        buffer.writeFloat(this.explosionRadius);
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
        this.explosionRadius = additionalData.readFloat();
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
}
