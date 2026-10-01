package com.tacz.guns.client.sound;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.pojo.display.SoundData;
import com.tacz.guns.config.client.SoundConfig;
import com.tacz.guns.config.common.GunConfig;
import com.tacz.guns.init.ModSounds;
import com.tacz.guns.network.message.ServerMessageSound;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.sound.SoundManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(value = Dist.CLIENT, modid = GunMod.MOD_ID)
public class SoundPlayManager {
    private static final FileToIdConverter TACZ_SOUND_LISTER = new FileToIdConverter("tacz_sounds", ".ogg");
    private static final SoundData DEFAULT_UNLOAD_SOUND = SoundData.of(ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID,
            "attachments/attachment_general_uninstall"));

    private static final Map<SoundKey, ArrayDeque<TrackedGunSound>> TRACKED_GUN_SOUNDS = new HashMap<>();
    private static final Map<ResourceLocation, Boolean> SOUND_RESOURCE_EXISTS_CACHE = new HashMap<>();
    private static final Set<ResourceLocation> MISSING_SOUND_WARNED = new HashSet<>();

    private static int soundCleanupTickCounter = 0;

    /**
     * 用于阻止连发时，反复播放 DryFire 音效
     */
    private static boolean DRY_SOUND_TRACK = true;

    /**
     * 临时缓存，用于停止播放的
     */
    private static GunSoundInstance tmpSoundInstance = null;

    @Nullable
    public static GunSoundInstance playClientSound(Entity entity, @Nullable ResourceLocation name, float volume, float pitch, int distance, boolean mono) {
        boolean trackFirstPerson = SoundConfig.FIRST_PERSON_ANIMATION_SOUND_TRACKING.get();
        return playClientSound(entity, name, volume, pitch, distance, mono, SoundConfig.DEFAULT_SOUND_CONCURRENCY_LIMIT.get(), trackFirstPerson, true);
    }

    @Nullable
    public static GunSoundInstance playClientSound(Entity entity, @Nullable SoundData sound, float volume, float pitch, int distance) {
        return playClientSound(entity, sound, volume, pitch, distance, false);
    }

    @Nullable
    public static GunSoundInstance playClientSound(Entity entity, @Nullable SoundData sound, float volume, float pitch, int distance, boolean mono) {
        boolean trackFirstPerson = SoundConfig.FIRST_PERSON_ANIMATION_SOUND_TRACKING.get();
        return playClientSound(entity, sound, volume, pitch, distance, mono, SoundConfig.DEFAULT_SOUND_CONCURRENCY_LIMIT.get(), trackFirstPerson, true);
    }

    @Nullable
    private static GunSoundInstance playClientSound(Entity entity, @Nullable SoundData sound, float volume, float pitch, int distance, boolean mono, int concurrencyLimit, boolean trackEntity, boolean relative) {
        if (sound == null) {
            return null;
        }
        RandomSource random = entity != null ? entity.level().random : RandomSource.create();
        float resolvedVolume = sound.resolveVolume(random, volume);
        float resolvedPitch = sound.resolvePitch(random, pitch);
        return playClientSound(entity, sound.getSound(), resolvedVolume, resolvedPitch, distance, mono, concurrencyLimit, trackEntity, relative);
    }

    @Nullable
    private static GunSoundInstance playClientSound(Entity entity, @Nullable ResourceLocation name, float volume, float pitch, int distance, boolean mono, int concurrencyLimit, boolean trackEntity, boolean relative) {
        Minecraft minecraft = Minecraft.getInstance();
        if (name == null) {
            return null;
        }
        boolean useTaczSound = hasTaczSoundResource(minecraft, name);
        if (!useTaczSound && minecraft.getSoundManager().getSoundEvent(name) == null) {
            warnMissingSound(name);
            return null;
        }
        if (concurrencyLimit > 0) {
            limitConcurrentGunSound(minecraft, entity.getId(), name, concurrencyLimit);
        }
        SoundEvent soundEvent = useTaczSound ? ModSounds.GUN.get() : SoundEvent.createVariableRangeEvent(name);
        GunSoundInstance instance = trackEntity
                ? new EntityTrackingGunSoundInstance(soundEvent, SoundSource.PLAYERS, volume, pitch, entity, distance, name, mono, useTaczSound)
                : new GunSoundInstance(soundEvent, SoundSource.PLAYERS, volume, pitch, entity, distance, name, mono, relative, useTaczSound);
        minecraft.getSoundManager().play(instance);
        if (concurrencyLimit > 0) {
            trackGunSound(entity.getId(), entity.getUUID(), name, instance);
        }
//        traceGunSoundPlay(minecraft, instance, entity, name, volume, pitch, distance, mono);
        return instance;
    }

    @Nullable
    public static GunSoundInstance playClientSound(Entity entity, @Nullable ResourceLocation name, float volume, float pitch, int distance) {
        return playClientSound(entity, name, volume, pitch, distance, false);
    }

    @Nullable
    public static GunSoundInstance playAnimationSound(Entity entity, @Nullable ResourceLocation name, float volume, float pitch, int distance) {
        if (isLocalPlayer(entity)) {
            boolean trackFirstPerson = SoundConfig.FIRST_PERSON_ANIMATION_SOUND_TRACKING.get();
            return playClientSound(entity, name, volume, pitch, distance, false, SoundConfig.HIGH_FREQUENCY_SOUND_CONCURRENCY_LIMIT.get(), trackFirstPerson, !trackFirstPerson);
        }
        return playClientSound(entity, name, volume, pitch, distance, false, SoundConfig.HIGH_FREQUENCY_SOUND_CONCURRENCY_LIMIT.get(), true, false);
    }


    public static void stopPlayGunSound() {
        if (tmpSoundInstance != null) {
            tmpSoundInstance.setStop();
        }
    }

    public static void stopPlayGunSound(GunDisplayInstance gunIndex, String animationName) {
        if (tmpSoundInstance != null) {
            SoundData sound = gunIndex.getSounds(animationName);
            if (sound != null && tmpSoundInstance.getRegistryName() != null && tmpSoundInstance.getRegistryName().equals(sound.getSound())) {
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
            Map<String, SoundData> sounds = index.getSounds();
            if (sounds.containsKey(soundName)) {
                SoundData sound = sounds.get(soundName);
                SoundPlayManager.playClientSound(player, sound, 1.0f, 1.0f, GunConfig.DEFAULT_GUN_OTHER_SOUND_DISTANCE.get());
            }
        });
    }

    public static void playShootSound(LivingEntity entity, GunDisplayInstance gunIndex, GunData gunData) {
        playClientSound(entity, gunIndex.getSounds(SoundManager.SHOOT_SOUND), 0.8f, 0.9f + entity.getRandom().nextFloat() * 0.125f, (int) (GunConfig.DEFAULT_GUN_FIRE_SOUND_DISTANCE.get() * gunData.getFireSound().getFireMultiplier()), false, SoundConfig.HIGH_FREQUENCY_SOUND_CONCURRENCY_LIMIT.get(), false, false);
    }

    public static void playSilenceSound(LivingEntity entity, GunDisplayInstance gunIndex, GunData gunData) {
        playClientSound(entity, gunIndex.getSounds(SoundManager.SILENCE_SOUND), 0.6f, 0.9f + entity.getRandom().nextFloat() * 0.125f, (int) (GunConfig.DEFAULT_GUN_SILENCE_SOUND_DISTANCE.get() * gunData.getFireSound().getSilenceMultiplier()), false, SoundConfig.HIGH_FREQUENCY_SOUND_CONCURRENCY_LIMIT.get(), false, false);
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

    public static void playUnloadSound(LivingEntity entity, ResourceLocation gunId, ResourceLocation gunDisplayId) {
        SoundData sound = TimelessAPI.getGunDisplay(gunDisplayId, gunId)
                .map(display -> display.getSounds(SoundManager.UNLOAD_SOUND))
                .orElse(DEFAULT_UNLOAD_SOUND);
        playClientSound(entity, sound, 1.0F, 1.0F, GunConfig.DEFAULT_GUN_OTHER_SOUND_DISTANCE.get());
    }

    public static void playHeadHitSound(LivingEntity entity, GunDisplayInstance display) {
        boolean relative = isLocalPlayer(entity);
        playClientSound(entity, display.getSounds(SoundManager.HEAD_HIT_SOUND), 1.0f, 1.0f, GunConfig.DEFAULT_GUN_OTHER_SOUND_DISTANCE.get(), false, SoundConfig.HIT_SOUND_CONCURRENCY_LIMIT.get(), !relative, relative);
    }

    public static void playFleshHitSound(LivingEntity entity, GunDisplayInstance display) {
        boolean relative = isLocalPlayer(entity);
        playClientSound(entity, display.getSounds(SoundManager.FLESH_HIT_SOUND), 1.0f, 1.0f, GunConfig.DEFAULT_GUN_OTHER_SOUND_DISTANCE.get(), false, SoundConfig.HIT_SOUND_CONCURRENCY_LIMIT.get(), !relative, relative);
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
            SoundData sound = index.getSounds(soundName);
            if (sound == null) {
                return;
            }
            if (SoundManager.SHOOT_3P_SOUND.equals(soundName) || SoundManager.SILENCE_3P_SOUND.equals(soundName)) {
                playClientSound(livingEntity, sound, message.getVolume(), message.getPitch(), message.getDistance(), true, SoundConfig.HIGH_FREQUENCY_SOUND_CONCURRENCY_LIMIT.get(), true, false);
            } else {
                playClientSound(livingEntity, sound, message.getVolume(), message.getPitch(), message.getDistance(), true, SoundConfig.DEFAULT_SOUND_CONCURRENCY_LIMIT.get(), true, false);
            }
        });
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            cleanupInvalidEntitySounds(minecraft);
            return;
        }
        soundCleanupTickCounter++;
        if (soundCleanupTickCounter % 5 == 0) {
            cleanupInvalidEntitySounds(minecraft);
        }
    }

    public static void clearSoundResourceCache() {
        SOUND_RESOURCE_EXISTS_CACHE.clear();
        MISSING_SOUND_WARNED.clear();
    }

    private static void limitConcurrentGunSound(Minecraft minecraft, int entityId, ResourceLocation soundId, int limit) {
        SoundKey key = new SoundKey(entityId, soundId);
        ArrayDeque<TrackedGunSound> sounds = TRACKED_GUN_SOUNDS.get(key);
        if (sounds == null) {
            return;
        }
        int activeForKey = 0;
        Iterator<TrackedGunSound> iterator = sounds.iterator();
        while (iterator.hasNext()) {
            TrackedGunSound tracked = iterator.next();
            if (minecraft.getSoundManager().isActive(tracked.instance())) {
                activeForKey++;
            } else {
                iterator.remove();
            }
        }
        if (sounds.isEmpty()) {
            TRACKED_GUN_SOUNDS.remove(key);
        }

        int toStop = activeForKey - limit + 1;
        if (toStop <= 0) {
            return;
        }

        int stopped = 0;
        iterator = sounds.iterator();
        while (iterator.hasNext() && stopped < toStop) {
            TrackedGunSound tracked = iterator.next();
            if (minecraft.getSoundManager().isActive(tracked.instance())) {
                tracked.instance().setStop();
                iterator.remove();
                stopped++;
            }
        }
        if (sounds.isEmpty()) {
            TRACKED_GUN_SOUNDS.remove(key);
        }
    }

    private static void trackGunSound(int entityId, UUID entityUuid, ResourceLocation soundId, GunSoundInstance instance) {
        SoundKey key = new SoundKey(entityId, soundId);
        TRACKED_GUN_SOUNDS.computeIfAbsent(key, ignored -> new ArrayDeque<>()).addLast(new TrackedGunSound(instance, entityUuid));
    }

    private static void cleanupInvalidEntitySounds(Minecraft minecraft) {
        if (minecraft.level == null) {
            stopAndClearTrackedSounds();
            return;
        }
        Iterator<Map.Entry<SoundKey, ArrayDeque<TrackedGunSound>>> entryIterator = TRACKED_GUN_SOUNDS.entrySet().iterator();
        while (entryIterator.hasNext()) {
            Map.Entry<SoundKey, ArrayDeque<TrackedGunSound>> entry = entryIterator.next();
            Iterator<TrackedGunSound> soundIterator = entry.getValue().iterator();
            while (soundIterator.hasNext()) {
                TrackedGunSound tracked = soundIterator.next();
                if (!minecraft.getSoundManager().isActive(tracked.instance())) {
                    soundIterator.remove();
                    continue;
                }
                Entity owner = minecraft.level.getEntity(entry.getKey().entityId());
                if (isInvalidSoundOwner(owner, tracked.entityUuid())) {
                    tracked.instance().setStop();
                    soundIterator.remove();
                }
            }
            if (entry.getValue().isEmpty()) {
                entryIterator.remove();
            }
        }
    }

    private static void stopAndClearTrackedSounds() {
        for (ArrayDeque<TrackedGunSound> sounds : TRACKED_GUN_SOUNDS.values()) {
            for (TrackedGunSound tracked : sounds) {
                tracked.instance().setStop();
            }
        }
        TRACKED_GUN_SOUNDS.clear();
    }

    private static boolean isInvalidSoundOwner(@Nullable Entity entity, UUID entityUuid) {
        return entity == null
                || !entity.getUUID().equals(entityUuid)
                || entity.isRemoved()
                || entity instanceof LivingEntity livingEntity && livingEntity.isDeadOrDying();
    }

    private static boolean isLocalPlayer(Entity entity) {
        return entity == Minecraft.getInstance().player;
    }

    private static boolean hasTaczSoundResource(Minecraft minecraft, ResourceLocation soundId) {
        return SOUND_RESOURCE_EXISTS_CACHE.computeIfAbsent(soundId, id -> {
            ResourceLocation soundPath = TACZ_SOUND_LISTER.idToFile(id);
            return minecraft.getResourceManager().getResource(soundPath).isPresent();
        });
    }

    private static void warnMissingSound(ResourceLocation soundId) {
        if (MISSING_SOUND_WARNED.add(soundId)) {
            ResourceLocation soundPath = TACZ_SOUND_LISTER.idToFile(soundId);
            GunMod.LOGGER.warn("[TACZ Sound] Missing tacz_sounds resource and fallback sound event, skipped. sound={}, path={}", soundId, soundPath);
        }
    }

    private record SoundKey(int entityId, ResourceLocation soundId) {}

    private record TrackedGunSound(GunSoundInstance instance, UUID entityUuid) {}
}
