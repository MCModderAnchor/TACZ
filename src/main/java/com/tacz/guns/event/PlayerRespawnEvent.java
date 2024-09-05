package com.tacz.guns.event;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.config.common.GunConfig;
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
        Player player = event.getEntity();
        player.getInventory().items.forEach(currentGunItem -> {
            if (!(currentGunItem.getItem() instanceof AbstractGunItem iGun)) {
                return;
            }
            TimelessAPI.getCommonGunIndex(iGun.getGunId(currentGunItem)).ifPresent(gunIndex -> {
                if (IGunOperator.fromLivingEntity(player).needCheckAmmo() && !iGun.canReload(player, currentGunItem)) {
                    return;
                }
                iGun.doReload(player, currentGunItem, false);
            });
        });
    }
}
