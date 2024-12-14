package com.tacz.guns.api.item;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.api.item.gun.FireMode;
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
     * 取下枪内所有子弹。玩家的特殊方法，默认卸载弹药时使用
     */
    void dropAllAmmo(Player player, ItemStack gun);

    /**
     * 获取当前枪械指定类型的配件
     */
    @Nonnull
    ItemStack getAttachment(ItemStack gun, AttachmentType type);

    @Nonnull
    ItemStack getBuiltinAttachment(ItemStack gun, AttachmentType type);

    /**
     * 获取当前枪械指定类型的配件的 NBT 数据
     *
     * @return 如果为空，那么没有配件数据
     */
    @Nullable
    CompoundTag getAttachmentTag(ItemStack gun, AttachmentType type);

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
    void installAttachment(@Nonnull ItemStack gun, @Nonnull ItemStack attachment);

    /**
     * 卸载配件
     */
    void unloadAttachment(@Nonnull ItemStack gun, AttachmentType type);

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
}