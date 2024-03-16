package com.tac.guns.entity;

import com.tac.guns.api.entity.IGunOperator;
import com.tac.guns.resource.DefaultAssets;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

public class EntityBullet extends ThrowableProjectile implements IEntityAdditionalSpawnData {
    public static final EntityType<EntityBullet> TYPE = EntityType.Builder.<EntityBullet>of(EntityBullet::new, MobCategory.MISC)
            .noSummon()
            .noSave()
            .fireImmune()
            .sized(0.0625F, 0.0625F)
            .clientTrackingRange(4)
            .updateInterval(5)
            .setShouldReceiveVelocityUpdates(false)
            .build("bullet");
    private ResourceLocation ammoId = DefaultAssets.EMPTY_AMMO_ID;

    public EntityBullet(EntityType<? extends ThrowableProjectile> type, Level worldIn) {
        super(type, worldIn);
    }

    public EntityBullet(Level worldIn, LivingEntity throwerIn, ResourceLocation ammoId) {
        super(TYPE, throwerIn, worldIn);
        this.ammoId = ammoId;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount > 200) {
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (result.getEntity() instanceof LivingEntity livingEntity) {
            // 取消无敌时间
            livingEntity.invulnerableTime = 0;
            // 取消击退效果
            if (livingEntity instanceof IGunOperator operator) {
                // TODO：可以自定义击退效果，伤害
                operator.setKnockbackStrength(0);
                livingEntity.hurt(DamageSource.thrown(this, this.getOwner()), 5);
                operator.resetKnockbackStrength();
            }
        }
        super.onHitEntity(result);
    }

    @Override
    protected float getGravity() {
        return 0;
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
    }

    public ResourceLocation getAmmoId() {
        return ammoId;
    }
}
