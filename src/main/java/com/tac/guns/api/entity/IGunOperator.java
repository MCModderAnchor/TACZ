package com.tac.guns.api.entity;

import com.tac.guns.api.gun.FireMode;
import com.tac.guns.api.gun.ReloadState;
import com.tac.guns.api.gun.ShootResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public interface IGunOperator {
    /**
     * 获取从服务端同步的射击的冷却
     */
    long getSynShootCoolDown();

    /**
     * 获取从服务端同步的换弹状态
     */
    ReloadState getSynReloadState();

    /**
     * 获取从服务端同步的瞄准进度
     */
    float getSynAimingProgress();

    void draw(ItemStack gunItemStack);

    void reload(ItemStack gunItemStack);

    FireMode fireSelect(ItemStack gunItemStack);

    /**
     * 从实体的位置，向指定的方向开枪
     *
     * @param gunItemStack 枪物品
     * @param pitch        开火方向的俯仰角(即 xRot )
     * @param yaw          开火方向的偏航角(即 yRot )
     * @return 本次射击的结果
     */
    ShootResult shoot(ItemStack gunItemStack, float pitch, float yaw);

    void aim(ItemStack gunItemStack, boolean isAim);

    static IGunOperator fromLivingEntity(LivingEntity entity) {
        return (IGunOperator) entity;
    }
}
