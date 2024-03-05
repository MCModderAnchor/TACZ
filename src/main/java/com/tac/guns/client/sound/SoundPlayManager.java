package com.tac.guns.client.sound;

import com.tac.guns.client.resource.index.ClientGunIndex;
import com.tac.guns.init.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

public class SoundPlayManager {
    public static void playClientSound(LivingEntity entity, ResourceLocation name, float volume, float pitch) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getSoundManager().play(new GunSoundInstance(ModSounds.GUN.get(), SoundSource.PLAYERS, volume, pitch, entity, name));
    }

    public static void playShootSound(LivingEntity entity, ClientGunIndex gunIndex) {
        SoundPlayManager.playClientSound(entity, gunIndex.getSounds("shoot"), 1.0f, 0.8f);
    }
}