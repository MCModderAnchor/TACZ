package com.tacz.guns.client.gameplay;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.config.sync.SyncConfig;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ClientMessagePlayerCrawl;
import net.minecraft.client.player.LocalPlayer;
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
        // 退出不能被冷却、切枪或离地拦截，且只同步实际发生的状态变化。
        if (!isCrawl) {
            setCrawling(false);
            return;
        }
        ItemStack mainHandItem = player.getMainHandItem();
        if (isCrawling || crawCooldownTicks > 0 || !canCrawl()) {
            return;
        }
        if (mainHandItem.getItem() instanceof IGun gun
                && TimelessAPI.getClientGunIndex(gun.getGunId(mainHandItem)).isEmpty()) {
            return;
        }
        setCrawling(true);
        this.crawCooldownTicks = COOLDOWN_TICKS;
    }

    public void tickCrawl() {
        if (crawCooldownTicks > 0) {
            crawCooldownTicks--;
        }
        if (!isCrawling) {
            return;
        }
        boolean poseChanged = player.getForcedPose() != Pose.SWIMMING;
        if (poseChanged || !canCrawl()) {
            setCrawling(false);
        }
    }

    public boolean isCrawling() {
        return isCrawling;
    }

    private boolean canCrawl() {
        if (!SyncConfig.ENABLE_CRAWL.get()) {
            return false;
        }
        boolean invalidState = !player.isAlive() || player.isSpectator() || player.isPassenger();
        boolean invalidMovement = player.jumping || player.isSwimming() || !player.onGround();
        boolean incompatiblePose = player.isSleeping() || player.isFallFlying() || player.isAutoSpinAttack();
        if (invalidState || invalidMovement || incompatiblePose) {
            return false;
        }
        ItemStack mainHandItem = player.getMainHandItem();
        if (mainHandItem.getItem() instanceof IGun gun) {
            return gun.isCanCrawl(mainHandItem)
                    && TimelessAPI.getCommonGunIndex(gun.getGunId(mainHandItem)).isPresent();
        }
        return SyncConfig.ENABLE_CRAWL_WITHOUT_GUN.get();
    }

    private void setCrawling(boolean crawling) {
        if (isCrawling == crawling) {
            return;
        }
        isCrawling = crawling;
        // 趴下状态变化时设置或清除一次
        if (crawling || player.getForcedPose() == Pose.SWIMMING) {
            player.setForcedPose(crawling ? Pose.SWIMMING : null);
        }
        NetworkHandler.CHANNEL.sendToServer(new ClientMessagePlayerCrawl(crawling));
    }
}
