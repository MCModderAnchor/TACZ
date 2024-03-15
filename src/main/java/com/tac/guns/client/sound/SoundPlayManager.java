package com.tac.guns.client.sound;

import com.tac.guns.api.TimelessAPI;
import com.tac.guns.client.resource.index.ClientGunIndex;
import com.tac.guns.init.ModSounds;
import com.tac.guns.network.message.ServerMessageSound;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import static com.tac.guns.resource.DefaultAssets.*;

@OnlyIn(Dist.CLIENT)
public class SoundPlayManager {
    /**
     * 用于阻止连发时，反复播放 DryFire 音效
     */
    private static boolean DRY_SOUND_TRACK = true;

    /**
     * 临时缓存，用于停止播放的
     */
    private static GunSoundInstance tmpSoundInstance = null;

    public static GunSoundInstance playClientSound(LivingEntity entity, ResourceLocation name, float volume, float pitch) {
        Minecraft minecraft = Minecraft.getInstance();
        GunSoundInstance instance = new GunSoundInstance(ModSounds.GUN.get(), SoundSource.PLAYERS, volume, pitch, entity, name);
        minecraft.getSoundManager().play(instance);
        return instance;
    }

    public static void stopPlayGunSound() {
        if (tmpSoundInstance != null) {
            tmpSoundInstance.setStop();
        }
    }

    public static void playShootSound(LivingEntity entity, ClientGunIndex gunIndex) {
        playClientSound(entity, gunIndex.getSounds(SHOOT_SOUND), 0.8f, 0.9f + entity.getRandom().nextFloat() * 0.125f);
    }

    public static void playDryFireSound(LivingEntity entity, ClientGunIndex gunIndex) {
        if (DRY_SOUND_TRACK) {
            playClientSound(entity, gunIndex.getSounds(DRY_FIRE_SOUND), 1.0f, 1.0f);
            DRY_SOUND_TRACK = false;
        }
    }

    /**
     * 只有松开鼠标时，才会重置
     */
    public static void resetDryFireSound() {
        DRY_SOUND_TRACK = true;
    }

    public static void playReloadSound(LivingEntity entity, ClientGunIndex gunIndex, boolean noAmmo) {
        if (noAmmo) {
            tmpSoundInstance = playClientSound(entity, gunIndex.getSounds(RELOAD_EMPTY_SOUND), 1.0f, 1.0f);
        } else {
            tmpSoundInstance = playClientSound(entity, gunIndex.getSounds(RELOAD_TACTICAL_SOUND), 1.0f, 1.0f);
        }
    }

    public static void playInspectSound(LivingEntity entity, ClientGunIndex gunIndex, boolean noAmmo) {
        if (noAmmo) {
            tmpSoundInstance = playClientSound(entity, gunIndex.getSounds(INSPECT_EMPTY_SOUND), 1.0f, 1.0f);
        } else {
            tmpSoundInstance = playClientSound(entity, gunIndex.getSounds(INSPECT_SOUND), 1.0f, 1.0f);
        }
    }

    public static void playDrawSound(LivingEntity entity, ClientGunIndex gunIndex) {
        tmpSoundInstance = playClientSound(entity, gunIndex.getSounds(DRAW_SOUND), 1.0f, 1.0f);
    }

    public static void playMessageSound(ServerMessageSound message) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !(level.getEntity(message.getEntityId()) instanceof LivingEntity livingEntity)) {
            return;
        }
        ResourceLocation gunId = message.getGunId();
        TimelessAPI.getClientGunIndex(gunId).ifPresent(index -> {
            ResourceLocation soundId = index.getSounds(message.getSoundName());
            if (soundId == null) {
                return;
            }
            playClientSound(livingEntity, soundId, message.getVolume(), message.getPitch());
        });
    }
}