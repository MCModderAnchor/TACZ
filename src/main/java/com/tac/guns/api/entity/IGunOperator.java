package com.tac.guns.api.entity;

import com.tac.guns.api.gun.FireMode;
import com.tac.guns.api.gun.ReloadState;
import com.tac.guns.api.gun.ShootResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public interface IGunOperator {
    /**
     * LivingEntity 通过 Mixin 的方式实现了这个接口
     */
    static IGunOperator fromLivingEntity(LivingEntity entity) {
        return (IGunOperator) entity;
    }

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

    /**
     * 服务端切枪逻辑
     */
    void draw(ItemStack gunItemStack);

    void reload();

    FireMode fireSelect();

    /**
     * 从实体的位置，向指定的方向开枪
     *
     * @param pitch 开火方向的俯仰角(即 xRot )
     * @param yaw   开火方向的偏航角(即 yRot )
     * @return 本次射击的结果
     */
    ShootResult shoot(float pitch, float yaw);

    void aim(boolean isAim);
}
