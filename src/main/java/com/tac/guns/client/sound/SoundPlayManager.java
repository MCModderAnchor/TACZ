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
        SoundPlayManager.playClientSound(entity, gunIndex.getSounds("shoot"), 0.8f, 0.9f + entity.getRandom().nextFloat() * 0.125f);
    }

    public static void playDryFireSound(LivingEntity entity, ClientGunIndex gunIndex) {
        SoundPlayManager.playClientSound(entity, gunIndex.getSounds("dry_fire"), 1.0f, 1.0f);
    }

    public static void playReloadSound(LivingEntity entity, ClientGunIndex gunIndex, boolean noAmmo) {
        if (noAmmo) {
            SoundPlayManager.playClientSound(entity, gunIndex.getSounds("reload_empty"), 1.0f, 1.0f);
        } else {
            SoundPlayManager.playClientSound(entity, gunIndex.getSounds("reload_tactical"), 1.0f, 1.0f);
        }
    }

    public static void playInspectSound(LivingEntity entity, ClientGunIndex gunIndex) {
        SoundPlayManager.playClientSound(entity, gunIndex.getSounds("inspect"), 1.0f, 1.0f);
    }

    public static void playDrawSound(LivingEntity entity, ClientGunIndex gunIndex) {
        SoundPlayManager.playClientSound(entity, gunIndex.getSounds("draw"), 1.0f, 1.0f);
    }
}