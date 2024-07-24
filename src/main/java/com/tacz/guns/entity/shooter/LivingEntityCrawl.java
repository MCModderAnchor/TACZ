package com.tacz.guns.entity.shooter;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class LivingEntityCrawl {
    private final LivingEntity shooter;
    private final ShooterDataHolder data;

    public LivingEntityCrawl(LivingEntity shooter, ShooterDataHolder data) {
        this.shooter = shooter;
        this.data = data;
    }

    public void crawl(boolean isCrawl) {
        data.isCrawling = isCrawl;
    }

    public void tickCrawling() {
        // currentGunItem 如果为 null，则取消趴下状态
        if (data.currentGunItem == null || !(data.currentGunItem.get().getItem() instanceof IGun iGun)) {
            data.isCrawling = false;
            this.setCrawlPose();
            return;
        }
        ItemStack currentGunItem = data.currentGunItem.get();
        // 如果获取不到 gunIndex，则取消趴下状态
        ResourceLocation gunId = iGun.getGunId(currentGunItem);
        if (TimelessAPI.getCommonGunIndex(gunId).isEmpty()) {
            data.isCrawling = false;
            this.setCrawlPose();
            return;
        }
        // 如果是观察者模型、骑乘、跳跃、在游泳、不在地上，取消
        if (shooter.isSpectator() || shooter.isPassenger() || shooter.jumping || shooter.isSwimming() || !shooter.onGround()) {
            data.isCrawling = false;
            this.setCrawlPose();
            return;
        }
        this.setCrawlPose();
    }

    private void setCrawlPose() {
        if (data.isCrawling) {
            if (shooter instanceof Player player) {
                player.setForcedPose(Pose.SWIMMING);
            } else {
                this.shooter.setPose(Pose.SWIMMING);
            }
        } else {
            if (shooter instanceof Player player) {
                player.setForcedPose(null);
            }
        }
    }
}
