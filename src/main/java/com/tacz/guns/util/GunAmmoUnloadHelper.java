package com.tacz.guns.util;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.config.sync.SyncConfig;
import com.tacz.guns.resource.index.CommonAmmoIndex;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import com.tacz.guns.resource.pojo.data.gun.FeedType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * 枪械物品的基础卸弹结算，不改变拆卸弹匣配件时的既有退弹规则。
 */
public final class GunAmmoUnloadHelper {
    private GunAmmoUnloadHelper() {
    }

    public static int unloadAmmo(LivingEntity shooter, ItemStack stack, AbstractGunItem gun, CommonGunIndex index,
                                 boolean unloadBarrel, int amount) {
        if (amount < -1 || shooter.level().isClientSide() || gun.useInventoryAmmo(stack)) {
            return 0;
        }
        int currentAmmo = Math.max(0, gun.getCurrentAmmoCount(stack));
        int magazineAmmo = amount == -1 ? currentAmmo : Math.min(currentAmmo, amount);
        boolean chambered = unloadBarrel && index.getGunData().getBolt() != Bolt.OPEN_BOLT && gun.hasBulletInBarrel(stack);
        int unloaded = magazineAmmo + (chambered ? 1 : 0);
        if (unloaded == 0) {
            return 0;
        }

        boolean returnAmmo = IGunOperator.fromLivingEntity(shooter).needCheckAmmo()
                && !index.getGunData().getReloadData().isInfinite()
                && index.getGunData().getReloadData().getType() != FeedType.FUEL;
        boolean dummyAmmo = gun.useDummyAmmo(stack);
        ResourceLocation ammoId = index.getGunData().getAmmoId();
        CommonAmmoIndex ammoIndex = TimelessAPI.getCommonAmmoIndex(ammoId).orElse(null);
        // 普通弹药缺少定义时不执行卸弹，避免清空后无法生成返还物品。
        if (returnAmmo && !dummyAmmo && (ammoIndex == null || ammoIndex.getStackSize() <= 0)) {
            return 0;
        }

        if (magazineAmmo > 0) {
            gun.setCurrentAmmoCount(stack, currentAmmo - magazineAmmo);
        }
        if (chambered) {
            gun.setBulletInBarrel(stack, false);
        }
        if (!returnAmmo) {
            return unloaded;
        }
        if (dummyAmmo) {
            gun.addDummyAmmoAmount(stack, unloaded);
            return unloaded;
        }

        IItemHandler inventory = shooter.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.ENTITY);
        int remaining = inventory == null ? unloaded : returnToAmmoBoxes(inventory, ammoId, ammoIndex.getStackSize(), unloaded);
        while (remaining > 0) {
            int count = Math.min(remaining, ammoIndex.getStackSize());
            ItemStack ammoStack = AmmoItemBuilder.create().setId(ammoId).setCount(count).build();
            if (shooter instanceof Player player) {
                ItemHandlerHelper.giveItemToPlayer(player, ammoStack);
            } else {
                ItemStack overflow = inventory == null ? ammoStack : ItemHandlerHelper.insertItemStacked(inventory, ammoStack, false);
                if (!overflow.isEmpty()) {
                    shooter.spawnAtLocation(overflow);
                }
            }
            remaining -= count;
        }
        return unloaded;
    }

    private static int returnToAmmoBoxes(IItemHandler inventory, ResourceLocation ammoId, int stackSize, int amount) {
        int remaining = amount;
        for (int slot = 0; slot < inventory.getSlots() && remaining > 0; slot++) {
            ItemStack boxStack = inventory.getStackInSlot(slot);
            if (!(boxStack.getItem() instanceof IAmmoBox box) || box.isCreative(boxStack)
                    || box.isAllTypeCreative(boxStack) || !ammoId.equals(box.getAmmoId(boxStack))) {
                continue;
            }
            int current = box.getAmmoCount(boxStack);
            // 异常负数计数不能作为返还目标，剩余弹药继续交给背包或掉落。
            if (current < 0) {
                continue;
            }
            long capacity = (long) stackSize * SyncConfig.AMMO_BOX_STACK_SIZE.get() * ((long) box.getAmmoLevel(boxStack) + 1);
            long space = Math.max(0, Math.min(capacity, Integer.MAX_VALUE) - current);
            int accepted = (int) Math.min(space, remaining);
            if (accepted > 0) {
                box.setAmmoCount(boxStack, current + accepted);
                remaining -= accepted;
            }
        }
        return remaining;
    }
}
