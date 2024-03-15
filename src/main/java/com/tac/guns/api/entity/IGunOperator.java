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
     * 获取从服务端同步的切枪的冷却
     */
    long getSynDrawCoolDown();

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

    /**
     * 服务端换弹逻辑
     */
    void reload();

    /**
     * 服务端切换开火模式的逻辑
     *
     * @return 切换后的开火模式
     */
    FireMode fireSelect();

    /**
     * 从实体的位置，向指定的方向开枪
     *
     * @param pitch 开火方向的俯仰角(即 xRot )
     * @param yaw   开火方向的偏航角(即 yRot )
     * @return 本次射击的结果
     */
    ShootResult shoot(float pitch, float yaw);

    /**
     * 服务端，该操作者是否受弹药数影响
     *
     * @return 如果为 false，那么开火不会检查弹药，也不会消耗枪械弹药
     */
    boolean needCheckAmmo();

    /**
     * 服务端，应用瞄准的逻辑
     *
     * @param isAim 是否瞄准
     */
    void aim(boolean isAim);

    void setKnockbackStrength(double strength);

    void resetKnockbackStrength();

    double getKnockbackStrength();
}
