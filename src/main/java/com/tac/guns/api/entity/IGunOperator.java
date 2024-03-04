package com.tac.guns.api.entity;

import com.tac.guns.api.gun.ReloadState;
import com.tac.guns.api.gun.FireMode;
import com.tac.guns.api.gun.ShootResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public interface IGunOperator {
    /**
     * 获取双端同步的射击的冷却，在客户端改变此值不会影响服务端逻辑。
     */
    long getSynShootCoolDown();

    /**
     * 获取双端同步的换弹状态，在客户端改变此值不会影响服务端逻辑。
     */
    ReloadState getSynReloadState();

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

    static IGunOperator fromLivingEntity(LivingEntity entity) {
        return (IGunOperator) entity;
    }
}
