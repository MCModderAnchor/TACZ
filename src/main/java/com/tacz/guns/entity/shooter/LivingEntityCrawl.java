package com.tacz.guns.entity.shooter;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.config.sync.SyncConfig;
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
        setCrawling(isCrawl && canCrawl());
    }

    public void tickCrawling() {
        if (!data.isCrawling) {
            return;
        }
        boolean poseChanged = shooter instanceof Player player && player.getForcedPose() != Pose.SWIMMING;
        if (poseChanged || !canCrawl()) {
            setCrawling(false);
        }
    }

    private boolean canCrawl() {
        if (!SyncConfig.ENABLE_CRAWL.get()) {
            return false;
        }
        boolean invalidState = !shooter.isAlive() || shooter.isSpectator() || shooter.isPassenger();
        boolean invalidMovement = shooter.jumping || shooter.isSwimming() || !shooter.onGround();
        boolean incompatiblePose = shooter.isSleeping() || shooter.isFallFlying() || shooter.isAutoSpinAttack();
        if (invalidState || invalidMovement || incompatiblePose) {
            return false;
        }
        // 玩家可以从未持过枪，也可能刚切换物品，以实际主手物品为准。
        ItemStack gunItem = shooter instanceof Player ? shooter.getMainHandItem()
                : data.currentGunItem == null ? ItemStack.EMPTY : data.currentGunItem.get();
        if (gunItem.getItem() instanceof IGun gun) {
            return gun.isCanCrawl(gunItem)
                    && TimelessAPI.getCommonGunIndex(gun.getGunId(gunItem)).isPresent();
        }
        return shooter instanceof Player && SyncConfig.ENABLE_CRAWL_WITHOUT_GUN.get();
    }

    private void setCrawling(boolean crawling) {
        if (data.isCrawling == crawling) {
            return;
        }
        data.isCrawling = crawling;
        // 只在 TACZ 的趴下状态变化时设置或清除一次，不持续写入姿态。
        if (shooter instanceof Player player) {
            if (crawling || player.getForcedPose() == Pose.SWIMMING) {
                player.setForcedPose(crawling ? Pose.SWIMMING : null);
            }
        } else {
            shooter.setPose(crawling ? Pose.SWIMMING : Pose.STANDING);
        }
    }
}
