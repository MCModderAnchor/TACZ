package com.tacz.guns.client.sound;

import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;

public class EntityTrackingGunSoundInstance extends GunSoundInstance implements TickableSoundInstance {
    private final WeakReference<Entity> entityRef;
    private boolean stopped;

    public EntityTrackingGunSoundInstance(SoundEvent soundEvent, SoundSource source, float volume, float pitch, Entity entity, int soundDistance, @Nullable ResourceLocation registryName, boolean mono) {
        super(soundEvent, source, volume, pitch, entity, soundDistance, registryName, mono, false);
        this.entityRef = new WeakReference<>(entity);
    }

    @Override
    public boolean canPlaySound() {
        Entity entity = this.entityRef.get();
        return super.canPlaySound() && entity != null && !entity.isSilent();
    }

    @Override
    public boolean isStopped() {
        return this.stopped;
    }

    @Override
    public void tick() {
        Entity entity = this.entityRef.get();
        if (entity == null || entity.isRemoved() || entity instanceof LivingEntity livingEntity && livingEntity.isDeadOrDying()) {
            this.setStop();
            return;
        }
        this.x = entity.getX();
        this.y = entity.getEyeY();
        this.z = entity.getZ();
    }

    @Override
    public void setStop() {
        this.stopped = true;
        super.setStop();
    }
}
