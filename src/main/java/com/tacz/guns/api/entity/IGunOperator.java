package com.tacz.guns.api.entity;

import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.function.Supplier;

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
     * 获取从服务端同步的近战的冷却（主要是刺刀）
     */
    long getSynMeleeCoolDown();

    /**
     * 获取从服务端同步的切枪的冷却
     */
    long getSynDrawCoolDown();

    /**
     * 获取从服务端同步的手动换弹的冷却
     */
    long getSynBoltCoolDown();

    /**
     * 获取从服务端同步的换弹状态
     */
    ReloadState getSynReloadState();

    /**
     * 获取从服务端同步的瞄准进度
     */
    float getSynAimingProgress();

    /**
     * 获取该实体是否正在瞄准。
     * 注意，这个方法并不等价于 getSynAimingProgress() > 0。
     * 如果玩家正在瞄准，瞄准进度会增加，否则瞄准进度会减少。
     */
    boolean getSynIsAiming();

    /**
     * 获取玩家持枪奔跑的时长。
     * 最大不会大于枪械数据中设置的 sprintTime，最小不会小于 0。
     */
    float getSynSprintTime();

    /**
     * 初始化枪械操作的各个数据，如换弹冷却、开火冷却等。
     */
    void initialData();

    /**
     * 服务端切枪逻辑
     */
    void draw(Supplier<ItemStack> itemStackSupplier);

    /**
     * 服务端拉栓逻辑
     */
    void bolt();

    /**
     * 服务端换弹逻辑
     */
    void reload();

    /**
     * 服务端切换开火模式的逻辑
     */
    void fireSelect();

    /**
     * 服务端调整倍镜的逻辑
     */
    void zoom();

    /**
     * 服务端近战的逻辑（刺刀）
     */
    void melee();

    /**
     * 从实体的位置，向指定的方向开枪
     *
     * @param pitch 开火方向的俯仰角(即 xRot )
     * @param yaw   开火方向的偏航角(即 yRot )
     * @return 本次射击的结果
     */
    ShootResult shoot(Supplier<Float> pitch, Supplier<Float> yaw);

    /**
     * 服务端，该操作者是否受弹药数影响
     *
     * @return 如果为 false，那么开火时不会检查弹药，无论是玩家背包内还是枪械内的
     */
    boolean needCheckAmmo();

    /**
     * 服务端，开火是否消耗弹药
     *
     * @return 如果为 false，那么开火不会消耗枪械弹药
     */
    boolean consumesAmmoOrNot();

    /**
     * 服务端，应用瞄准的逻辑
     *
     * @param isAim 是否瞄准
     */
    void aim(boolean isAim);

    /**
     * 服务端应用趴下逻辑
     */
    void crawl(boolean isCrawl);

    /**
     * 更新枪械的配件属性修改值
     * <p>
     * 通过将配件修改的属性值缓存在实体上，避免频繁的计算，提升性能
     *
     * @param cacheProperty 更新完的配件属性修改值
     */
    void updateCacheProperty(AttachmentCacheProperty cacheProperty);

    /**
     * 获取配件属性修改值缓存
     *
     * @return 绝大部分情况下，这个数值都不可能为 null
     */
    @Nullable
    AttachmentCacheProperty getCacheProperty();
}
