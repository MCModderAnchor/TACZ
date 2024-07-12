package com.tacz.guns.api.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * 子弹盒接口
 */
public interface IAmmoBox {
    /**
     * 获取子弹盒中的子弹 ID
     *
     * @param ammoBox 子弹盒
     * @return 子弹盒中的子弹 ID
     */
    ResourceLocation getAmmoId(ItemStack ammoBox);

    /**
     * 获取子弹盒中的子弹数量
     *
     * @param ammoBox 子弹盒
     * @return 子弹数量
     */
    int getAmmoCount(ItemStack ammoBox);

    /**
     * 设置子弹盒中子弹的 ID
     */
    void setAmmoId(ItemStack ammoBox, ResourceLocation ammoId);

    /**
     * 设置子弹盒中子弹数量
     */
    void setAmmoCount(ItemStack ammoBox, int count);

    /**
     * 子弹盒中的子弹是否属于这把枪
     *
     * @param gun     枪
     * @param ammoBox 子弹盒
     * @return 是否属于这把枪
     */
    boolean isAmmoBoxOfGun(ItemStack gun, ItemStack ammoBox);

    /**
     * 设置子弹盒的等级，
     *
     * @param ammoBox   子弹盒
     * @param ammoLevel 子弹盒等级
     * @return 修改后的子弹盒
     */
    ItemStack setAmmoLevel(ItemStack ammoBox, int ammoLevel);

    /**
     * 获取子弹盒的等级，
     *
     * @param ammoBox 子弹盒
     * @return 等级，从 0 开始
     */
    int getAmmoLevel(ItemStack ammoBox);

    /**
     * 是否是无限子弹盒
     *
     * @param ammoBox 子弹盒
     * @return 是否是无限子弹盒
     */
    boolean isCreative(ItemStack ammoBox);

    /**
     * 是否是全种类无限子弹盒
     *
     * @param ammoBox 子弹盒
     * @return 是否是全种类无限子弹盒
     */
    boolean isAllTypeCreative(ItemStack ammoBox);

    /**
     * 将该弹药箱设置为无限种类
     *
     * @param ammoBox   子弹盒
     * @param isAllType 是否是全弹种类型
     * @return 修改后的子弹盒
     */
    ItemStack setCreative(ItemStack ammoBox, boolean isAllType);
}
