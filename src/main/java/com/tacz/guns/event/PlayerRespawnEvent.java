package com.tacz.guns.event;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.config.common.GunConfig;
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
        if (GunConfig.AUTO_RELOAD_WHEN_RESPAWN.get()) {
            Player player = event.getEntity();
            player.getInventory().items.forEach(item -> {
                if (item.getItem() instanceof AbstractGunItem gun) {
                    // TODO 实现正确的弹药消耗
                    TimelessAPI.getCommonGunIndex(gun.getGunId(item)).ifPresent(index -> {
                                int maxAmmCount = AttachmentDataUtils.getAmmoCountWithAttachment(item, index.getGunData());
                                gun.setCurrentAmmoCount(item, maxAmmCount);
                            }
                    );
                }
            });
        }
    }
}
