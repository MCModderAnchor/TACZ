package com.tacz.guns.event;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.config.common.GunConfig;
import com.tacz.guns.entity.shooter.LivingEntityReload;
import com.tacz.guns.util.AttachmentDataUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class PlayerRespawnEvent {
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        // 重生后自动换弹
        if (!GunConfig.AUTO_RELOAD_WHEN_RESPAWN.get()) {
            return;
        }
        Player player = event.getPlayer();
        player.getInventory().items.forEach(currentGunItem -> {
            if (!(currentGunItem.getItem() instanceof AbstractGunItem iGun)) {
                return;
            }
            TimelessAPI.getCommonGunIndex(iGun.getGunId(currentGunItem)).ifPresent(gunIndex -> {
                int currentAmmoCount = iGun.getCurrentAmmoCount(currentGunItem);
                int maxAmmoCount = AttachmentDataUtils.getAmmoCountWithAttachment(currentGunItem, gunIndex.getGunData());
                if (IGunOperator.fromLivingEntity(player).needCheckAmmo() && !LivingEntityReload.inventoryHasAmmo(player, currentAmmoCount, maxAmmoCount, currentGunItem, iGun)) {
                    return;
                }
                iGun.reloadAmmo(currentGunItem, LivingEntityReload.getAndExtractNeedAmmoCount(player, currentGunItem, iGun, maxAmmoCount), false);
            });
        });
    }
}
