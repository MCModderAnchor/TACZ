package com.tacz.guns.event;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.config.common.GunConfig;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.util.AttachmentDataUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandler;

import java.util.Optional;

@Mod.EventBusSubscriber
public class PlayerRespawnEvent {
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        // 重生后自动换弹
        if (GunConfig.AUTO_RELOAD_WHEN_RESPAWN.get()) {
            Player player = event.getEntity();
            player.getInventory().items.forEach(item -> {
                if (item.getItem() instanceof AbstractGunItem gun) {
                    Optional<CommonGunIndex> gunIndexOptional = TimelessAPI.getCommonGunIndex(gun.getGunId(item));
                    if (gunIndexOptional.isEmpty()) {
                        return;
                    }

                    GunData gunData = gunIndexOptional.get().getGunData();
                    int maxAmmCount = AttachmentDataUtils.getAmmoCountWithAttachment(item, gunData);

                    if (player.isCreative()) {
                        gun.setCurrentAmmoCount(item, maxAmmCount);
                    } else {
                        int maxAmmoCount = AttachmentDataUtils.getAmmoCountWithAttachment(item, gunData);
                        gun.reloadAmmo(item, getAndExtractNeedAmmoCount(player, gun, item, maxAmmoCount), false);
                    }
                }
            });
        }
    }

    private static int getAndExtractNeedAmmoCount(Player player, IGun iGun, ItemStack gunItem, int maxAmmoCount) {
        int currentAmmoCount = iGun.getCurrentAmmoCount(gunItem);
        if (IGunOperator.fromLivingEntity(player).needCheckAmmo()) {
            return player.getCapability(ForgeCapabilities.ITEM_HANDLER, null)
                    .map(cap -> getAndExtractInventoryAmmoCount(cap, maxAmmoCount, currentAmmoCount, gunItem))
                    .orElse(currentAmmoCount);
        }
        return maxAmmoCount;
    }

    private static int getAndExtractInventoryAmmoCount(IItemHandler itemHandler, int maxAmmoCount, int currentAmmoCount, ItemStack currentGunItem) {
        // 子弹数量检查
        int needAmmoCount = maxAmmoCount - currentAmmoCount;
        // 背包检查
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack checkAmmoStack = itemHandler.getStackInSlot(i);
            if (checkAmmoStack.getItem() instanceof IAmmo iAmmo && iAmmo.isAmmoOfGun(currentGunItem, checkAmmoStack)) {
                ItemStack extractItem = itemHandler.extractItem(i, needAmmoCount, false);
                needAmmoCount = needAmmoCount - extractItem.getCount();
                if (needAmmoCount <= 0) {
                    break;
                }
            }
            if (checkAmmoStack.getItem() instanceof IAmmoBox iAmmoBox && iAmmoBox.isAmmoBoxOfGun(currentGunItem, checkAmmoStack)) {
                int boxAmmoCount = iAmmoBox.getAmmoCount(checkAmmoStack);
                int extractCount = Math.min(boxAmmoCount, needAmmoCount);
                int remainCount = boxAmmoCount - extractCount;
                iAmmoBox.setAmmoCount(checkAmmoStack, remainCount);
                if (remainCount <= 0) {
                    iAmmoBox.setAmmoId(checkAmmoStack, DefaultAssets.EMPTY_AMMO_ID);
                }
                needAmmoCount = needAmmoCount - extractCount;
                if (needAmmoCount <= 0) {
                    break;
                }
            }
        }
        return maxAmmoCount - needAmmoCount;
    }
}
