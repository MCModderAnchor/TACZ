package com.tacz.guns.client.sound;

import com.mojang.blaze3d.audio.SoundBuffer;
import com.tacz.guns.client.resource.ClientAssetsManager;
import com.tacz.guns.client.resource.manager.SoundAssetsManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import javax.sound.sampled.AudioFormat;

public class GunSoundInstance extends EntityBoundSoundInstance {
    private final ResourceLocation registryName;
    private final boolean mono;

    public GunSoundInstance(SoundEvent soundEvent, SoundSource source, float volume, float pitch, Entity entity, int soundDistance, ResourceLocation registryName, boolean mono) {
        super(soundEvent, source, volume, pitch, entity, 943);
        this.attenuation = Attenuation.NONE;
        this.registryName = registryName;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            this.volume = volume * (1.0F - Math.min(1.0F, (float) Math.sqrt(player.distanceToSqr(x, y, z)) / soundDistance));
            this.volume *= this.volume;
        }
        this.mono = mono;
    }

    public void setStop() {
        this.stop();
    }

    @Nullable
    public SoundBuffer getSoundBuffer() {
        SoundAssetsManager.SoundData soundData = ClientAssetsManager.INSTANCE.getSoundBuffers(this.registryName);
        if (soundData == null) {
            return null;
        }
        AudioFormat rawFormat = soundData.audioFormat();
        if (this.mono && rawFormat.getChannels() > 1) {
            AudioFormat monoFormat = new AudioFormat(rawFormat.getEncoding(), rawFormat.getSampleRate(), rawFormat.getSampleSizeInBits(), 1, rawFormat.getFrameSize(), rawFormat.getFrameRate(), rawFormat.isBigEndian(), rawFormat.properties());
            return new SoundBuffer(soundData.byteBuffer(), monoFormat);
        }
        return new SoundBuffer(soundData.byteBuffer(), soundData.audioFormat());
    }

    public ResourceLocation getRegistryName() {
        return registryName;
    }
}
