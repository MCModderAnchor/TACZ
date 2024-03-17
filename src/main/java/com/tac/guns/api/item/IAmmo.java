package com.tac.guns.api.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public interface IAmmo {
    /**
     * @return 如果物品类型为 IAttachment 则返回显式转换后的实例，否则返回 null。
     */
    @Nullable
    static IAmmo getIAmmoOrNull(@Nullable ItemStack stack) {
        if (stack == null) {
            return null;
        }
        if (stack.getItem() instanceof IAmmo iAmmo) {
            return iAmmo;
        }
        return null;
    }

    /**
     * 获取弹药 ID
     *
     * @param ammo 输入物品
     * @return 弹药 ID
     */
    ResourceLocation getAmmoId(ItemStack ammo);

    void setAmmoId(ItemStack gun, @Nullable ResourceLocation ammoId);

    boolean isAmmoOfGun(ItemStack gun, ItemStack ammo);
}
