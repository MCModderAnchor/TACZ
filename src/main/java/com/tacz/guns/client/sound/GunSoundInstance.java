package com.tacz.guns.client.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;

public class GunSoundInstance extends AbstractSoundInstance {
    private static final FileToIdConverter TACZ_SOUND_LISTER = new FileToIdConverter("tacz_sounds", ".ogg");

    @Nullable
    private final ResourceLocation registryName;
    private final boolean canPlay;
    @Nullable
    private Sound redirectedSound;
    private final boolean useTaczSound;

    public GunSoundInstance(SoundEvent soundEvent, SoundSource source, float volume, float pitch, Entity entity, int soundDistance,
                            ResourceLocation registryName, boolean mono, boolean relative, boolean useTaczSound) {
        super(soundEvent, source, RandomSource.create(943));
        this.attenuation = Attenuation.NONE;
        this.relative = relative;
        this.registryName = registryName;
        this.useTaczSound = useTaczSound;
        this.canPlay = !entity.isSilent();
        this.volume = volume;
        this.pitch = pitch;
        this.x = relative ? 0 : entity.getX();
        this.y = relative ? 0 : entity.getY();
        this.z = relative ? 0 : entity.getZ();
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && !relative) {
            this.volume = volume * (1.0F - Math.min(1.0F, (float) Math.sqrt(player.distanceToSqr(x, y, z)) / soundDistance));
            this.volume *= this.volume;
        }

    }

    public void setStop() {
        Minecraft.getInstance().getSoundManager().stop(this);
    }

    @Override
    public boolean canPlaySound() {
        return this.canPlay;
    }

    @Nullable
    public ResourceLocation getRegistryName() {
        return registryName;
    }

    @Override
    public WeighedSoundEvents resolve(SoundManager manager) {
        WeighedSoundEvents events = super.resolve(manager);
        if (this.useTaczSound && this.registryName != null) {
            this.redirectedSound = new TaczSound(this.registryName, TACZ_SOUND_LISTER.idToFile(this.registryName), super.getSound());
            this.sound = this.redirectedSound;
        } else {
            this.redirectedSound = null;
        }
        return events;
    }

    @Override
    public Sound getSound() {
        return redirectedSound == null ? super.getSound() : redirectedSound;
    }

    private static class TaczSound extends Sound {
        private final ResourceLocation location;
        private final ResourceLocation path;

        private TaczSound(ResourceLocation location, ResourceLocation path, Sound template) {
            super(location, template.getVolume(), template.getPitch(), template.getWeight(), Type.FILE,
                    template.shouldStream(), false, template.getAttenuationDistance());
            this.location = location;
            this.path = path;
        }

        @Override
        public ResourceLocation getLocation() {
            return location;
        }

        @Override
        public ResourceLocation getPath() {
            return path;
        }
    }
}
