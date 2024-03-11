package com.tac.guns.api.item;

import com.tac.guns.api.gun.FireMode;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public interface IGun {
    /**
     * 该物品是否为枪
     */
    static boolean isGun(@Nullable ItemStack stack) {
        if (stack == null) {
            return false;
        }
        return stack.getItem() instanceof IGun;
    }

    /**
     * 是否主手持枪
     */
    static boolean mainhandHoldGun(LivingEntity livingEntity) {
        return livingEntity.getMainHandItem().getItem() instanceof IGun;
    }

    /**
     * 获取主手枪械的开火模式
     */
    static FireMode getMainhandFireMode(LivingEntity livingEntity) {
        ItemStack mainhandItem = livingEntity.getMainHandItem();
        if (mainhandItem.getItem() instanceof IGun iGun) {
            return iGun.getFireMode(mainhandItem);
        }
        return FireMode.UNKNOWN;
    }

    /**
     * 是否副手持枪
     */
    static boolean offhandHoldGun(LivingEntity livingEntity) {
        return livingEntity.getOffhandItem().getItem() instanceof IGun;
    }

    /**
     * 获取枪械 ID
     *
     * @param gun 输入物品
     * @return 枪械 ID
     */
    ResourceLocation getGunId(ItemStack gun);

    void setGunId(ItemStack gun, @Nullable ResourceLocation gunId);

    /**
     * 获取开火模式
     *
     * @param gun 枪
     * @return 开火模式
     */
    FireMode getFireMode(ItemStack gun);

    void setFireMode(ItemStack gun, @Nullable FireMode fireMode);

    int getCurrentAmmoCount(ItemStack gun);

    void setCurrentAmmoCount(ItemStack gun, int ammoCount);

    void reduceCurrentAmmoCount(ItemStack gun);
}