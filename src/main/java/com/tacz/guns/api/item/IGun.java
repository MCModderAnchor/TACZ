package com.tacz.guns.api.item;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.GunProperty;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 这里不包含枪械的逻辑，只包含枪械的各种 nbt 访问。<br>
 * 你可以在 {@link AbstractGunItem} 看到枪械逻辑
 */
public interface IGun {
    /**
     * @return 如果物品类型为 IGun 则返回显式转换后的实例，否则返回 null。
     */
    @Nullable
    static IGun getIGunOrNull(@Nullable ItemStack stack) {
        if (stack == null) {
            return null;
        }
        if (stack.getItem() instanceof IGun iGun) {
            return iGun;
        }
        return null;
    }

    /**
     * 是否主手持枪
     */
    @Deprecated
    static boolean mainhandHoldGun(LivingEntity livingEntity) {
        return livingEntity.getMainHandItem().getItem() instanceof IGun;
    }

    /**
     * 是否主手持枪
     */
    static boolean mainHandHoldGun(LivingEntity livingEntity) {
        return livingEntity.getMainHandItem().getItem() instanceof IGun;
    }

    /**
     * 获取主手枪械的开火模式
     */
    @Deprecated
    static FireMode getMainhandFireMode(LivingEntity livingEntity) {
        ItemStack mainHandItem = livingEntity.getMainHandItem();
        if (mainHandItem.getItem() instanceof IGun iGun) {
            return iGun.getFireMode(mainHandItem);
        }
        return FireMode.UNKNOWN;
    }

    /**
     * 获取主手枪械的开火模式
     */
    static FireMode getMainHandFireMode(LivingEntity livingEntity) {
        ItemStack mainHandItem = livingEntity.getMainHandItem();
        if (mainHandItem.getItem() instanceof IGun iGun) {
            return iGun.getFireMode(mainHandItem);
        }
        return FireMode.UNKNOWN;
    }

    /**
     * 获取瞄准放大倍率
     */
    float getAimingZoom(ItemStack gunItem);

    /**
     * 枪械换弹时是否使用"虚拟备弹"而不是背包里的实际弹药
     */
    boolean useDummyAmmo(ItemStack gun);

    /**
     * 获取枪械当前的"虚拟备弹"数量
     */
    int getDummyAmmoAmount(ItemStack gun);

    /**
     * 设置枪械当前的"虚拟备弹"数量
     */
    void setDummyAmmoAmount(ItemStack gun, int amount);

    /**
     * 添加枪械当前的"虚拟备弹"数量
     */
    void addDummyAmmoAmount(ItemStack gun, int amount);

    /**
     * 检查是否有设置"虚拟备弹"最大数量
     */
    boolean hasMaxDummyAmmo(ItemStack gun);

    /**
     * 获取枪械当前的"虚拟备弹"最大数量
     */
    int getMaxDummyAmmoAmount(ItemStack gun);

    /**
     * 设置枪械当前的"虚拟备弹"最大数量
     */
    void setMaxDummyAmmoAmount(ItemStack gun, int amount);

    /**
     * 获取枪械的"配件锁"情况
     */
    boolean hasAttachmentLock(ItemStack gun);

    /**
     * 设置枪械的"配件锁"
     */
    void setAttachmentLock(ItemStack gun, boolean locked);

    /**
     * 获取枪械 ID
     */
    @NotNull
    ResourceLocation getGunId(ItemStack gun);

    /**
     * 设置枪械 ID
     */
    void setGunId(ItemStack gun, @Nullable ResourceLocation gunId);

    /**
     * 获取枪械客户端效果 ID, 如果是默认皮肤将返回 {@link DefaultAssets#DEFAULT_GUN_DISPLAY_ID}<br/>
     * 你应该使用 {@link com.tacz.guns.api.TimelessAPI#getGunDisplay(ItemStack)} 获取正确的客户端效果
     */
    @NotNull
    ResourceLocation getGunDisplayId(ItemStack gun);

    /**
     * 设置枪械客户端效果 ID
     */
    void setGunDisplayId(ItemStack gun, @Nullable ResourceLocation displayId);

    /**
     * 获取输入的经验值对应的等级。
     *
     * @param exp 经验值
     * @return 对应的等级
     */
    int getLevel(int exp);

    /**
     * 获取输入的等级需要至少多少的经验值。
     *
     * @param level 等级
     * @return 至少需要的经验值
     */
    int getExp(int level);

    /**
     * 返回允许的最大等级。
     *
     * @return 最大等级
     */
    int getMaxLevel();

    /**
     * 获取枪械当前等级
     */
    int getLevel(ItemStack gun);

    /**
     * 获取积累的全部经验值。
     *
     * @param gun 输入物品
     * @return 全部经验值
     */
    int getExp(ItemStack gun);

    /**
     * 获取到下个等级需要的经验值。
     *
     * @param gun 输入物品
     * @return 到下个等级需要的经验值。如果等级已经到达最大，则返回 0
     */
    int getExpToNextLevel(ItemStack gun);

    /**
     * 获取当前等级已经积累的经验值。
     *
     * @param gun 输入物品
     * @return 当前等级已经积累的经验值
     */
    int getExpCurrentLevel(ItemStack gun);

    /**
     * 获取开火模式
     *
     * @param gun 枪
     * @return 开火模式
     */
    FireMode getFireMode(ItemStack gun);

    /**
     * 设置开火模式
     */
    void setFireMode(ItemStack gun, @Nullable FireMode fireMode);

    /**
     * 获取当前枪械弹药数
     */
    int getCurrentAmmoCount(ItemStack gun);

    /**
     * 设置当前枪械弹药数
     */
    void setCurrentAmmoCount(ItemStack gun, int ammoCount);

    /**
     * 减少一个当前枪械弹药数
     */
    void reduceCurrentAmmoCount(ItemStack gun);

    /**
     * 动态修改枪械的属性。
     * 注意：对于某些复杂属性来说，{@code GunProperty} 的类型可能会和值的类型不一样。
     * 比如伤害和精准度这样的复杂属性，GunProperty 的类型是复杂的数据结构，传入和返回的值就只是简单的浮点数。
     *
     * @param dataHolder 状态数据
     * @param gunItem 枪械物品
     * @param shooter 射击者
     * @param id 属性 id，请参阅 {@link com.tacz.guns.api.GunProperties}
     * @param type 属性的数据类型
     * @param original 属性原来的值
     * @return 脚本或子类修改后的属性
     * @param <T> 属性的数据类型
     *
     * @author ChloePrime
     * @since 1.1.7
     */
    default <T> T modifyProperty(ShooterDataHolder dataHolder, ItemStack gunItem, LivingEntity shooter,
                                 GunProperty<?> id, Class<T> type, T original) {
        return modifyProperty(dataHolder, gunItem ,shooter, id.name(), type, original);
    }

    /**
     * 动态修改枪械的属性
     *
     * @param dataHolder 状态数据
     * @param gunItem 枪械物品
     * @param shooter 射击者
     * @param id 属性 id，请参阅 {@link com.tacz.guns.api.GunProperties}
     * @param type 属性的数据类型
     * @param original 属性原来的值
     * @return 脚本或子类修改后的属性
     * @param <T> 属性的数据类型
     *
     * @author ChloePrime
     * @since 1.1.7
     */
    default <T> T modifyProperty(ShooterDataHolder dataHolder, ItemStack gunItem, LivingEntity shooter,
                                 String id, Class<T> type, T original) {
        return modifyProperty(dataHolder, gunItem ,shooter, "modify_property", id, type, original);
    }

    /**
     * 动态修改枪械的属性，
     * 允许指定修改用的 lua 函数的名称
     *
     * @param dataHolder 状态数据
     * @param gunItem 枪械物品
     * @param shooter 射击者
     * @param luaMethodName 修改属性的 lua 函数的函数名
     * @param id 属性 id，请参阅 {@link com.tacz.guns.api.GunProperties}
     * @param type 属性的数据类型
     * @param original 属性原来的值
     * @return 脚本或子类修改后的属性
     * @param <T> 属性的数据类型
     *
     * @author ChloePrime
     * @since 1.1.7
     */
    default <T> T modifyProperty(ShooterDataHolder dataHolder, ItemStack gunItem, LivingEntity shooter,
                                 String luaMethodName, String id, Class<T> type, T original) {
        return original;
    }

    /**
     * 取下枪内所有子弹。玩家的特殊方法，默认卸载弹药时使用
     */
    void dropAllAmmo(Player player, ItemStack gun);

    /**
     * 获取当前枪械指定类型的配件
     */
    @Nonnull
    ItemStack getAttachment(HolderLookup.Provider provider, ItemStack gun, AttachmentType type);

    @Nonnull
    ItemStack getBuiltinAttachment(ItemStack gun, AttachmentType type);

    /**
     * 获取当前枪械指定类型的配件的 NBT 数据
     *
     * @return 如果为空，那么没有配件数据
     */
    @Nullable
    CompoundTag getAttachmentTag(ItemStack gun, AttachmentType type);

    void setAttachmentTag(ItemStack gun, AttachmentType type, CompoundTag attachmentTag);

    @Nonnull
    ResourceLocation getBuiltInAttachmentId(ItemStack gun, AttachmentType type);

    /**
     * 获取枪械的配件 ID
     * <p>
     * 如果不存在，返回 {@link DefaultAssets#EMPTY_ATTACHMENT_ID};
     */
    @Nonnull
    ResourceLocation getAttachmentId(ItemStack gun, AttachmentType type);

    /**
     * 安装配件
     */
    void installAttachment(HolderLookup.Provider provider, @Nonnull ItemStack gun, @Nonnull ItemStack attachment);

    /**
     * 卸载配件
     */
    void unloadAttachment(HolderLookup.Provider provider, @Nonnull ItemStack gun, AttachmentType type);

    /**
     * 该枪械是否允许装配该配件
     */
    boolean allowAttachment(ItemStack gun, ItemStack attachmentItem);

    /**
     * 该枪械是否允许某类型配件
     */
    boolean allowAttachmentType(ItemStack gun, AttachmentType type);

    /**
     * 枪管中是否有子弹，用于闭膛待击的枪械
     */
    boolean hasBulletInBarrel(ItemStack gun);

    /**
     * 设置枪管中的子弹有无，用于闭膛待击的枪械
     */
    void setBulletInBarrel(ItemStack gun, boolean bulletInBarrel);

    /**
     * 枪械是否为备弹直读
     */
    boolean useInventoryAmmo(ItemStack gun);

    /**
     * 获取枪械是否有备弹 (只针对背包直读读的机制使用)
     */
    boolean hasInventoryAmmo(LivingEntity shooter, ItemStack gun, boolean needCheckAmmo);

    /**
     * 获取 RPM
     */
    int getRPM(ItemStack gun);

    /**
     * 获取是否可以趴下
     */
    boolean isCanCrawl(ItemStack gun);

    boolean hasCustomLaserColor(ItemStack gun);

    int getLaserColor(ItemStack gun);

    void setLaserColor(ItemStack gun, int color);

    /**
     * Heat Data
     */
    boolean hasHeatData(ItemStack gun);

    /**
     * 是否完全过热
     */
    boolean isOverheatLocked(ItemStack gun);

    void setOverheatLocked(ItemStack gun, boolean locked);

    /**
     * 设置当前过热值
     */
    void setHeatAmount(ItemStack gun, float amount);

    float lerpRPM(ItemStack gun);

    float lerpInaccuracy(ItemStack gun);

    float getHeatAmount(ItemStack gun);
}
