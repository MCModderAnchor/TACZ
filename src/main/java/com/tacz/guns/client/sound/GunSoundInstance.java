package com.tacz.guns.client.sound;

import com.mojang.blaze3d.audio.SoundBuffer;
import com.tacz.guns.client.resource.ClientAssetManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;

public class GunSoundInstance extends EntityBoundSoundInstance {
    private final ResourceLocation registryName;

    public GunSoundInstance(SoundEvent soundEvent, SoundSource source, float volume, float pitch, Entity entity, int soundDistance, ResourceLocation registryName) {
        super(soundEvent, source, volume, pitch, entity, 943);
        this.attenuation = Attenuation.NONE;
        this.registryName = registryName;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            this.volume = volume * (1.0F - Math.min(1.0F, (float) Math.sqrt(player.distanceToSqr(x, y, z)) / soundDistance));
            this.volume *= this.volume;
        }
    }

    public void setStop() {
        this.stop();
    }

    @Nullable
    public SoundBuffer getSoundBuffer() {
        return ClientAssetManager.INSTANCE.getSoundBuffers(this.registryName);
    }

    public ResourceLocation getRegistryName() {
        return registryName;
    }
}
