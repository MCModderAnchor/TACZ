package com.tacz.guns.client.gameplay;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ClientMessagePlayerCrawl;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;

public class LocalPlayerCrawl {
    /**
     * 冷却时间为 10 tick
     */
    private static final int COOLDOWN_TICKS = 10;
    private final LocalPlayer player;
    private boolean isCrawling = false;
    private int crawCooldownTicks = 0;

    public LocalPlayerCrawl(LocalPlayer player) {
        this.player = player;
    }

    public void crawl(boolean isCrawl) {
        // 持枪才能按键趴下
        ItemStack mainhandItem = player.getMainHandItem();
        if (!(mainhandItem.getItem() instanceof IGun iGun)) {
            return;
        }
        // 冷却时间没到，不执行
        if (crawCooldownTicks > 0) {
            return;
        }
        if (player.isSpectator() || player.isPassenger() || !player.onGround()) {
            return;
        }
        ResourceLocation gunId = iGun.getGunId(mainhandItem);
        TimelessAPI.getClientGunIndex(gunId).ifPresent(gunIndex -> {
            this.isCrawling = isCrawl;
            this.crawCooldownTicks = COOLDOWN_TICKS;
            NetworkHandler.CHANNEL.sendToServer(new ClientMessagePlayerCrawl(isCrawl));
        });
    }

    public void tickCrawl() {
        if (crawCooldownTicks > 0) {
            crawCooldownTicks--;
        }
        // 持枪才能按键趴下
        ItemStack mainhandItem = player.getMainHandItem();
        if (!(mainhandItem.getItem() instanceof IGun iGun)) {
            isCrawling = false;
            this.setCrawlPose();
            return;
        }
        // 如果获取不到 gunIndex，则取消趴下状态
        ResourceLocation gunId = iGun.getGunId(mainhandItem);
        if (TimelessAPI.getCommonGunIndex(gunId).isEmpty()) {
            isCrawling = false;
            this.setCrawlPose();
            return;
        }
        // 如果玩家是观察者模型、骑乘、跳跃、在游泳、不在地上，取消
        if (player.isSpectator() || player.isPassenger() || player.jumping || player.isSwimming() || !player.onGround()) {
            isCrawling = false;
            this.setCrawlPose();
            return;
        }
        this.setCrawlPose();
    }

    public boolean isCrawling() {
        return isCrawling;
    }

    private void setCrawlPose() {
        if (isCrawling) {
            player.setForcedPose(Pose.SWIMMING);
        } else {
            player.setForcedPose(null);
        }
    }
}
