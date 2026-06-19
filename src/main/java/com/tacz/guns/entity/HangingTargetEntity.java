package com.tacz.guns.entity;

import com.tacz.guns.api.entity.ITargetEntity;
import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.config.common.OtherConfig;
import com.tacz.guns.init.ModSounds;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.event.ServerMessageGunHurt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkHooks;
import org.apache.commons.lang3.tuple.Pair;

public class HangingTargetEntity extends Entity implements ITargetEntity {

    public static final EntityType<HangingTargetEntity> TYPE = EntityType.Builder.<HangingTargetEntity>of(HangingTargetEntity::new, MobCategory.MISC)
            .sized(0.9F, 0.9F)
            .clientTrackingRange(8)
            .build("hanging_target");

    private net.minecraft.world.phys.Vec3 pivotPos = null;
    private final double ROPE_LENGTH = 1.0;//吊绳长度？

    public HangingTargetEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    @Override protected void defineSynchedData() {}
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}

    @Override
    public void onProjectileHit(Entity entity, EntityHitResult result, DamageSource source, float damage) {
        if (this.level().isClientSide() || this.isRemoved()) return;
        if (!(source.isIndirect())) return;

        Entity sourceEntity = source.getEntity();
        if (sourceEntity instanceof Player player) {
            // 子弹击退力：由于锁了单轴，只有前后的力（Z轴分量）会真正起作用
            net.minecraft.world.phys.Vec3 knockback = entity.getDeltaMovement().normalize().scale(0.5);
            this.setDeltaMovement(this.getDeltaMovement().add(knockback));
            this.hasImpulse = true;

            double dis = this.position().distanceTo(sourceEntity.position());
            player.displayClientMessage(Component.translatable("message.tacz.target_minecart.hit", String.format("%.1f", damage), String.format("%.2f", dis)), true);

            float volume = OtherConfig.TARGET_SOUND_DISTANCE.get() / 16.0f;
            volume = Math.max(volume, 0);
            level().playSound(null, this, ModSounds.TARGET_HIT.get(), SoundSource.BLOCKS, volume, this.level().random.nextFloat() * 0.1F + 0.9F);

            if (entity instanceof EntityKineticBullet projectile) {
                boolean isHeadshot = false;
                float headshotMultiplier = 1;
                MinecraftForge.EVENT_BUS.post(new EntityHurtByGunEvent.Post(projectile, this, player, projectile.getGunId(), projectile.getGunDisplayId(), damage, Pair.of(source, source), isHeadshot, headshotMultiplier, LogicalSide.SERVER));
                NetworkHandler.sendToDimension(new ServerMessageGunHurt(projectile.getId(), this.getId(), player.getId(), projectile.getGunId(), projectile.getGunDisplayId(), damage, isHeadshot, headshotMultiplier), this);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (pivotPos == null) {
            pivotPos = this.position().add(0, ROPE_LENGTH, 0);
        }

        net.minecraft.world.phys.Vec3 velocity = this.getDeltaMovement().add(0, -0.04, 0);

        // ✨ 核心修正 1：强行将预测位置的 X 轴锁死在轴心上，实现绝对的单轴前后摆动
        double nextX = pivotPos.x;
        double nextY = this.getY() + velocity.y;
        double nextZ = this.getZ() + velocity.z;
        net.minecraft.world.phys.Vec3 nextPos = new net.minecraft.world.phys.Vec3(nextX, nextY, nextZ);

        net.minecraft.world.phys.Vec3 fromPivot = nextPos.subtract(pivotPos);
        fromPivot = fromPivot.normalize().scale(ROPE_LENGTH);
        net.minecraft.world.phys.Vec3 constrainedPos = pivotPos.add(fromPivot);

        net.minecraft.world.phys.Vec3 finalVelocity = constrainedPos.subtract(this.position());
        finalVelocity = finalVelocity.scale(0.95); // 阻尼

        // 防止无限微幅抽搐的静止休眠逻辑
        if (finalVelocity.lengthSqr() < 0.0001 && Math.abs(this.getZ() - pivotPos.z) < 0.01) {
            finalVelocity = net.minecraft.world.phys.Vec3.ZERO;
            this.setPos(pivotPos.x, pivotPos.y - ROPE_LENGTH, pivotPos.z);
        }

        this.setDeltaMovement(finalVelocity);
        this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());
    }

    public net.minecraft.world.phys.Vec3 getPivotPos() {
        return this.pivotPos;
    }

    @Override public boolean isInvulnerableTo(DamageSource source) { return source.is(DamageTypeTags.IS_EXPLOSION) || super.isInvulnerableTo(source); }
    @Override public boolean isPushable() { return false; }
    @Override public boolean isPickable() { return true; }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double size = this.getBoundingBox().getSize();
        if (Double.isNaN(size)) size = 1.0;
        size *= RenderConfig.TARGET_RENDER_DISTANCE.get() * getViewScale();
        return distance < size * size;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}