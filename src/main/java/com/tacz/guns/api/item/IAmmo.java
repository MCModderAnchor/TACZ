package com.tacz.guns.api.item;

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

    /**
     * 设置弹药 ID
     */
    void setAmmoId(ItemStack ammo, @Nullable ResourceLocation ammoId);

    /**
     * 弹药是否属于这把枪
     *
     * @param gun  检查的枪械物品
     * @param ammo 检查的子弹物品
     * @return 是否属于这把枪
     */
    boolean isAmmoOfGun(ItemStack gun, ItemStack ammo);
}
