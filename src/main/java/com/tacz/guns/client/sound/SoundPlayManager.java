package com.tacz.guns.client.sound;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.config.common.GunConfig;
import com.tacz.guns.init.ModSounds;
import com.tacz.guns.network.message.ServerMessageSound;
import com.tacz.guns.sound.SoundManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

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

    public static GunSoundInstance playClientSound(Entity entity, @Nullable ResourceLocation name, float volume, float pitch, int distance, boolean mono) {
        Minecraft minecraft = Minecraft.getInstance();
        GunSoundInstance instance = new GunSoundInstance(ModSounds.GUN.get(), SoundSource.PLAYERS, volume, pitch, entity, distance, name, mono);
        minecraft.getSoundManager().play(instance);
        return instance;
    }

    public static GunSoundInstance playClientSound(Entity entity, @Nullable ResourceLocation name, float volume, float pitch, int distance) {
        return playClientSound(entity, name, volume, pitch, distance, false);
    }

    public static void stopPlayGunSound() {
        if (tmpSoundInstance != null) {
            tmpSoundInstance.setStop();
        }
    }

    public static void stopPlayGunSound(GunDisplayInstance gunIndex, String animationName) {
        if (tmpSoundInstance != null) {
            if (tmpSoundInstance.getRegistryName() != null && tmpSoundInstance.getRegistryName().equals(gunIndex.getSounds(animationName))) {
                tmpSoundInstance.setStop();
            }
        }
    }

    public static void playerRefitSound(ItemStack attachmentItem, LocalPlayer player, String soundName) {
        IAttachment iAttachment = IAttachment.getIAttachmentOrNull(attachmentItem);
        if (iAttachment == null) {
            return;
        }
        ResourceLocation attachmentId = iAttachment.getAttachmentId(attachmentItem);
        TimelessAPI.getClientAttachmentIndex(attachmentId).ifPresent(index -> {
            Map<String, ResourceLocation> sounds = index.getSounds();
            if (sounds.containsKey(soundName)) {
                ResourceLocation resourceLocation = sounds.get(soundName);
                SoundPlayManager.playClientSound(player, resourceLocation, 1.0f, 1.0f, GunConfig.DEFAULT_GUN_OTHER_SOUND_DISTANCE.get());
            }
        });
    }

    public static void playShootSound(LivingEntity entity, GunDisplayInstance gunIndex) {
        playClientSound(entity, gunIndex.getSounds(SoundManager.SHOOT_SOUND), 0.8f, 0.9f + entity.getRandom().nextFloat() * 0.125f, GunConfig.DEFAULT_GUN_FIRE_SOUND_DISTANCE.get());
    }

    public static void playSilenceSound(LivingEntity entity, GunDisplayInstance gunIndex) {
        playClientSound(entity, gunIndex.getSounds(SoundManager.SILENCE_SOUND), 0.6f, 0.9f + entity.getRandom().nextFloat() * 0.125f, GunConfig.DEFAULT_GUN_OTHER_SOUND_DISTANCE.get());
    }

    public static void playDryFireSound(LivingEntity entity, GunDisplayInstance gunIndex) {
        if (DRY_SOUND_TRACK) {
            playClientSound(entity, gunIndex.getSounds(SoundManager.DRY_FIRE_SOUND), 1.0f, 0.9f + entity.getRandom().nextFloat() * 0.125f, GunConfig.DEFAULT_GUN_OTHER_SOUND_DISTANCE.get());
            DRY_SOUND_TRACK = false;
        }
    }

    /**
     * 只有松开鼠标时，才会重置
     */
    public static void resetDryFireSound() {
        DRY_SOUND_TRACK = true;
    }

    public static void playReloadSound(LivingEntity entity, GunDisplayInstance display, boolean noAmmo) {
        if (noAmmo) {
            tmpSoundInstance = playClientSound(entity, display.getSounds(SoundManager.RELOAD_EMPTY_SOUND), 1.0f, 1.0f, GunConfig.DEFAULT_GUN_OTHER_SOUND_DISTANCE.get());
        } else {
            tmpSoundInstance = playClientSound(entity, display.getSounds(SoundManager.RELOAD_TACTICAL_SOUND), 1.0f, 1.0f, GunConfig.DEFAULT_GUN_OTHER_SOUND_DISTANCE.get());
        }
    }

    public static void playInspectSound(LivingEntity entity, GunDisplayInstance display, boolean noAmmo) {
        if (noAmmo) {
            tmpSoundInstance = playClientSound(entity, display.getSounds(SoundManager.INSPECT_EMPTY_SOUND), 1.0f, 1.0f, GunConfig.DEFAULT_GUN_OTHER_SOUND_DISTANCE.get());
        } else {
            tmpSoundInstance = playClientSound(entity, display.getSounds(SoundManager.INSPECT_SOUND), 1.0f, 1.0f, GunConfig.DEFAULT_GUN_OTHER_SOUND_DISTANCE.get());
        }
    }

    public static void playBoltSound(LivingEntity entity, GunDisplayInstance display) {
        tmpSoundInstance = playClientSound(entity, display.getSounds(SoundManager.BOLT_SOUND), 1.0f, 1.0f, GunConfig.DEFAULT_GUN_OTHER_SOUND_DISTANCE.get());
    }

    public static void playDrawSound(LivingEntity entity, GunDisplayInstance display) {
        tmpSoundInstance = playClientSound(entity, display.getSounds(SoundManager.DRAW_SOUND), 1.0f, 1.0f, GunConfig.DEFAULT_GUN_OTHER_SOUND_DISTANCE.get());
    }

    public static void playPutAwaySound(LivingEntity entity, GunDisplayInstance display) {
        tmpSoundInstance = playClientSound(entity, display.getSounds(SoundManager.PUT_AWAY_SOUND), 1.0f, 1.0f, GunConfig.DEFAULT_GUN_OTHER_SOUND_DISTANCE.get());
    }

    public static void playFireSelectSound(LivingEntity entity, GunDisplayInstance display) {
        playClientSound(entity, display.getSounds(SoundManager.FIRE_SELECT), 1.0f, 1.0f, GunConfig.DEFAULT_GUN_OTHER_SOUND_DISTANCE.get());
    }

    public static void playMeleeBayonetSound(LivingEntity entity, GunDisplayInstance display) {
        playClientSound(entity, display.getSounds(SoundManager.MELEE_BAYONET), 1.0f, 0.9f + entity.getRandom().nextFloat() * 0.125f, GunConfig.DEFAULT_GUN_OTHER_SOUND_DISTANCE.get());
    }

    public static void playMeleePushSound(LivingEntity entity, GunDisplayInstance display) {
        playClientSound(entity, display.getSounds(SoundManager.MELEE_PUSH), 1.0f, 0.9f + entity.getRandom().nextFloat() * 0.125f, GunConfig.DEFAULT_GUN_OTHER_SOUND_DISTANCE.get());
    }

    public static void playMeleeStockSound(LivingEntity entity, GunDisplayInstance display) {
        playClientSound(entity, display.getSounds(SoundManager.MELEE_STOCK), 1.0f, 0.9f + entity.getRandom().nextFloat() * 0.125f, GunConfig.DEFAULT_GUN_OTHER_SOUND_DISTANCE.get());
    }

    public static void playHeadHitSound(LivingEntity entity, GunDisplayInstance display) {
        playClientSound(entity, display.getSounds(SoundManager.HEAD_HIT_SOUND), 1.0f, 1.0f, GunConfig.DEFAULT_GUN_OTHER_SOUND_DISTANCE.get());
    }

    public static void playFleshHitSound(LivingEntity entity, GunDisplayInstance display) {
        playClientSound(entity, display.getSounds(SoundManager.FLESH_HIT_SOUND), 1.0f, 1.0f, GunConfig.DEFAULT_GUN_OTHER_SOUND_DISTANCE.get());
    }

    public static void playKillSound(LivingEntity entity, GunDisplayInstance display) {
        playClientSound(entity, display.getSounds(SoundManager.KILL_SOUND), 1.0f, 1.0f, GunConfig.DEFAULT_GUN_OTHER_SOUND_DISTANCE.get());
    }

    public static void playMessageSound(ServerMessageSound message) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !(level.getEntity(message.getEntityId()) instanceof LivingEntity livingEntity)) {
            return;
        }
        ResourceLocation gunId = message.getGunId();
        ResourceLocation gunDisplayId = message.getGunDisplayId();
        TimelessAPI.getGunDisplay(gunDisplayId, gunId).ifPresent(index -> {
            String soundName = message.getSoundName();
            ResourceLocation soundId = index.getSounds(soundName);
            if (soundId == null) {
                return;
            }
            if (SoundManager.SHOOT_3P_SOUND.equals(soundName) || SoundManager.SILENCE_3P_SOUND.equals(soundName)) {
                playClientSound(livingEntity, soundId, message.getVolume(), message.getPitch(), message.getDistance(), true);
            } else {
                playClientSound(livingEntity, soundId, message.getVolume(), message.getPitch(), message.getDistance());
            }
        });
    }
}