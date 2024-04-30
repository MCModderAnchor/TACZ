package com.tac.guns.api.item;

import com.mojang.logging.LogUtils;
import com.tac.guns.api.TimelessAPI;
import com.tac.guns.api.attachment.AttachmentType;
import com.tac.guns.api.gun.FireMode;
import com.tac.guns.client.resource.index.ClientAttachmentIndex;
import com.tac.guns.client.resource.index.ClientGunIndex;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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

    default float getAimingZoom(ItemStack gunItem) {
        float zoom = 1;
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null) {
            return zoom;
        }
        ItemStack scopeItem = iGun.getAttachment(gunItem, AttachmentType.SCOPE);
        IAttachment iAttachment = IAttachment.getIAttachmentOrNull(scopeItem);
        if (iAttachment != null) {
            ResourceLocation scopeId = iAttachment.getAttachmentId(scopeItem);
            int zoomNumber = iAttachment.getZoomNumber(scopeItem);
            float[] zooms = TimelessAPI.getClientAttachmentIndex(scopeId).map(ClientAttachmentIndex::getZoom).orElse(null);
            if (zooms != null) {
                zoom = zooms[zoomNumber % zooms.length];
            }
        } else {
            zoom = TimelessAPI.getClientGunIndex(iGun.getGunId(gunItem)).map(ClientGunIndex::getIronZoom).orElse(1f);
            LogUtils.getLogger().info(zoom + "");
        }
        return zoom;
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
    @Nonnull
    ResourceLocation getGunId(ItemStack gun);

    void setGunId(ItemStack gun, @Nullable ResourceLocation gunId);

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

    void setFireMode(ItemStack gun, @Nullable FireMode fireMode);

    int getCurrentAmmoCount(ItemStack gun);

    void setCurrentAmmoCount(ItemStack gun, int ammoCount);

    void reduceCurrentAmmoCount(ItemStack gun);

    @Nonnull
    ItemStack getAttachment(ItemStack gun, AttachmentType type);

    void installAttachment(@Nonnull ItemStack gun, @Nonnull ItemStack attachment);

    void unloadAttachment(@Nonnull ItemStack gun, AttachmentType type);

    boolean allowAttachment(ItemStack gun, ItemStack attachmentItem);

    boolean allowAttachmentType(ItemStack gun, AttachmentType type);

    boolean hasBulletInBarrel(ItemStack gun);

    void setBulletInBarrel(ItemStack gun, boolean bulletInBarrel);
}