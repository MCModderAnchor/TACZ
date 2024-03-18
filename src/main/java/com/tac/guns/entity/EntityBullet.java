package com.tac.guns.entity;

import com.tac.guns.api.entity.IGunOperator;
import com.tac.guns.resource.DefaultAssets;
import com.tac.guns.resource.pojo.data.gun.BulletData;
import com.tac.guns.util.explosion.IExplosionDamageable;
import com.tac.guns.util.explosion.IExplosionProvider;
import com.tac.guns.util.explosion.ProjectileExplosion;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

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
            this.explosionRadius = Mth.clamp(data.getExplosionData().getRadius(), 0, Float.MAX_VALUE);
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

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (result.getEntity() instanceof LivingEntity livingEntity) {
            if (this.hasExplosion) {
                createExplosion(this, 10, 1, result.getLocation());
                this.discard();
                return;
            }
            // 取消无敌时间
            livingEntity.invulnerableTime = 0;
            // 取消击退效果
            if (livingEntity instanceof IGunOperator operator) {
                operator.setKnockbackStrength(this.knockback);
                livingEntity.hurt(DamageSource.thrown(this, this.getOwner()), this.damageAmount);
                if (this.hasExplosion) {
                    this.level.explode(this, this.getX(), this.getY(), this.getZ(), this.explosionRadius, Explosion.BlockInteraction.NONE);
                }
                operator.resetKnockbackStrength();
            }
        }
        super.onHitEntity(result);
        this.discard();
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult hitResult) {
        if (this.hasExplosion) {
            createExplosion(this, 10, 1, hitResult.getLocation());
            this.discard();
            return;
        }
        super.onHitBlock(hitResult);
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
    public boolean ownedBy(Entity entity) {
        return super.ownedBy(entity);
    }

    public static void createExplosion(Entity entity, float power, float radius, @Nullable Vec3 hitVec) {
        Level level = entity.level;
        if (level.isClientSide())
            return;

        Explosion.BlockInteraction mode = Explosion.BlockInteraction.NONE;
        DamageSource source = null;
        if (entity instanceof IExplosionProvider) {
            source = ((IExplosionProvider) entity).createDamageSource();
        }

        ProjectileExplosion explosion;
        if (hitVec == null)
            explosion = new ProjectileExplosion(level, entity, source, null, entity.getX(), entity.getY(), entity.getZ(), power, radius, mode);
        else
            explosion = new ProjectileExplosion(level, entity, source, null, hitVec.x(), hitVec.y(), hitVec.z(), power, radius, mode);

        if (net.minecraftforge.event.ForgeEventFactory.onExplosionStart(level, explosion))
            return;

        // Do explosion logic
        explosion.explode();
        explosion.finalizeExplosion(true);

        if (mode == Explosion.BlockInteraction.NONE) {
            explosion.clearToBlow();
        }

        // Send event to blocks that are exploded (none if mode is none)
        explosion.getToBlow().forEach(pos ->
        {
            if (level.getBlockState(pos).getBlock() instanceof IExplosionDamageable) {
                ((IExplosionDamageable) level.getBlockState(pos).getBlock()).onProjectileExploded(level, level.getBlockState(pos), pos, entity);
            }
        });

        for (ServerPlayer player : ((ServerLevel) level).players()) {
            if (hitVec == null) {
                if (player.distanceToSqr(entity.getX(), entity.getY(), entity.getZ()) < 4096)
                    player.connection.send(new ClientboundExplodePacket(entity.getX(), entity.getY(), entity.getZ(), radius, explosion.getToBlow(), explosion.getHitPlayers().get(player)));
            } else {
                if (player.distanceToSqr(hitVec.x(), hitVec.y(), hitVec.z()) < 4096)
                    player.connection.send(new ClientboundExplodePacket(hitVec.x(), hitVec.y(), hitVec.z(), radius, explosion.getToBlow(), explosion.getHitPlayers().get(player)));
            }
        }
    }
}
