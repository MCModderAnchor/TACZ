package com.tacz.guns.client.sound;

import com.mojang.blaze3d.audio.SoundBuffer;
import com.tacz.guns.client.resource.pojo.animation.bedrock.SoundEffectKeyframes;
import com.tacz.guns.init.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class BedrockGunSoundInstance extends GunSoundInstance{
    private final double[] keyframeTimeS;
    private final ResourceLocation[] keyframeSoundName;
    private long timestamp;
    private double timeS;
    private final Entity entity;
    private final int distance;
    private final List<GunSoundInstance> tempSoundInstance = new ArrayList<>();

    public BedrockGunSoundInstance(SoundEvent soundEvent, SoundSource source, float volume, float pitch, Entity entity, int distance, SoundEffectKeyframes keyframes) {
        super(soundEvent, source, volume, pitch, entity, distance, null);
        int keyframeNum = keyframes.getKeyframes().size();
        keyframeTimeS = new double[keyframeNum];
        keyframeSoundName = new ResourceLocation[keyframeNum];
        int i = 0;
        for (Map.Entry<Double, ResourceLocation> entry : keyframes.getKeyframes().double2ObjectEntrySet()) {
            keyframeTimeS[i] = entry.getKey();
            keyframeSoundName[i] = entry.getValue();
            i++;
        }
        this.timestamp = System.currentTimeMillis();
        this.entity = entity;
        this.timeS = 0;
        this.distance = distance;
    }

    @Override
    public void tick(){
        super.tick();
        if (!isStopped()) {
            double newTimeS = timeS + (System.currentTimeMillis() - timestamp) / 1000.0;
            int to = computeIndex(newTimeS, false);
            int from = computeIndex(timeS, true);
            // 根据实体位置计算音量
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                this.volume = volume * (1.0F - Math.min(1.0F, (float) Math.sqrt(player.distanceToSqr(x, y, z)) / distance));
                this.volume *= this.volume;
            }
            for (int i = from + 1; i <= to; i++) {
                ResourceLocation name = keyframeSoundName[i];
                Minecraft minecraft = Minecraft.getInstance();
                GunSoundInstance instance = new GunSoundInstance(ModSounds.GUN.get(), SoundSource.PLAYERS, volume, pitch, entity, distance, name);
                minecraft.getSoundManager().play(instance);
            }
            timeS = newTimeS;
            timestamp = System.currentTimeMillis();
        }
    }

    @Override
    @Nullable
    public SoundBuffer getSoundBuffer() {
        // 不直接播放音效，因此返回 null 。
        return null;
    }

    private int computeIndex(double timeS, boolean open) {
        int index = Arrays.binarySearch(keyframeTimeS, timeS);
        if (index >= 0) {
            return open ? index - 1 : index;
        }
        return -index - 2;
    }

}
