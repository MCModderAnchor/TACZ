package com.tacz.guns.api.client.animation;

import com.tacz.guns.client.sound.SoundPlayManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.Arrays;

public class ObjectAnimationSoundChannel {
    public AnimationSoundChannelContent content;

    public ObjectAnimationSoundChannel() {
    }

    public ObjectAnimationSoundChannel(AnimationSoundChannelContent content) {
        this.content = content;
    }

    /**
     * 播放区间内的所有声音。时间区间左开右闭
     */
    public void playSound(double fromTimeS, double toTimeS, Entity entity, int distance, float volume, float pitch) {
        if (content == null) {
            return;
        }
        if (fromTimeS == toTimeS) {
            return;
        }
        if (fromTimeS > toTimeS && fromTimeS <= getEndTimeS()) {
            playSound(0, toTimeS, entity, distance, volume, pitch);
            toTimeS = getEndTimeS();
        }
        int to = computeIndex(toTimeS, false);
        int from = computeIndex(fromTimeS, true);
        float mixVolume = volume;
        // 根据实体位置计算音量
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            mixVolume = mixVolume * (1.0F - Math.min(1.0F, (float) Math.sqrt(player.distanceToSqr(entity.getPosition(0))) / distance));
            mixVolume *= mixVolume;
        }
        for (int i = from + 1; i <= to; i++) {
            ResourceLocation name = content.keyframeSoundName[i];
            SoundPlayManager.playClientSound(entity, name, mixVolume, pitch, distance);
        }
    }

    public double getEndTimeS() {
        return content.keyframeTimeS[content.keyframeTimeS.length - 1];
    }

    private int computeIndex(double timeS, boolean open) {
        int index = Arrays.binarySearch(content.keyframeTimeS, timeS);
        if (index >= 0) {
            return open ? index - 1 : index;
        }
        return -index - 2;
    }
}
