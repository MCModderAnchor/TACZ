package com.tacz.guns.entity.shooter;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.resource.index.CommonGunIndex;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

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
            return;
        }
        ItemStack currentGunItem = data.currentGunItem.get();
        // 如果获取不到 gunIndex，则取消趴下状态
        ResourceLocation gunId = iGun.getGunId(currentGunItem);
        Optional<CommonGunIndex> gunIndexOptional = TimelessAPI.getCommonGunIndex(gunId);
        if (gunIndexOptional.isEmpty()) {
            data.isCrawling = false;
        }
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
