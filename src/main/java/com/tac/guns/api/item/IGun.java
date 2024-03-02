package com.tac.guns.api.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

// todo 还是觉得由 GunItem 来实现 IGun 有点别扭。应该有更广泛意义的实现。
public interface IGun {
    /**
     * 该物品是不是符合要求的弹药
     *
     * @param gun  需要匹配的弹药的枪械
     * @param ammo 输入的待检测物品
     * @return 是否是该枪支的合法弹药
     */
    boolean isAmmo(ItemStack gun, ItemStack ammo);

    /**
     * 重载弹药时的逻辑
     *
     * @param gun 需要重载弹药的枪
     */
    void reload(ItemStack gun);

    /**
     * 枪械射击，该方法只在服务端调用
     *
     * @param shooter 射手
     * @param gun     枪
     * @return 射击结果如何
     */
    ShootResult shoot(LivingEntity shooter, ItemStack gun);

    /**
     * 该物品是否为枪
     */
    static boolean isGun(ItemStack stack) {
        return stack.getItem() instanceof IGun;
    }

    /**
     * 是否主手持枪
     */
    static boolean mainhandHoldGun(LivingEntity livingEntity) {
        return livingEntity.getMainHandItem().getItem() instanceof IGun;
    }

    /**
     * 是否副手持枪
     */
    static boolean offhandHoldGun(LivingEntity livingEntity) {
        return livingEntity.getOffhandItem().getItem() instanceof IGun;
    }
}