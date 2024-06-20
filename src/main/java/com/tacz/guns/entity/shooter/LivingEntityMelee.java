package com.tacz.guns.entity.shooter;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.attachment.MeleeData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class LivingEntityMelee {
    private final LivingEntity shooter;
    private final ShooterDataHolder data;
    private final LivingEntityDrawGun draw;

    public LivingEntityMelee(LivingEntity shooter, ShooterDataHolder data, LivingEntityDrawGun draw) {
        this.shooter = shooter;
        this.data = data;
        this.draw = draw;
    }

    public void melee() {
        if (data.currentGunItem == null) {
            return;
        }
        // 检查是否在切枪
        if (draw.getDrawCoolDown() != 0) {
            return;
        }
        // 检查是否在拉栓
        if (data.boltCoolDown >= 0) {
            return;
        }
        long coolDown = getMeleeCoolDown();
        if (coolDown != 0) {
            return;
        }
        ItemStack currentGunItem = data.currentGunItem.get();
        if (currentGunItem.getItem() instanceof AbstractGunItem logicGun) {
            data.meleeTimestamp = System.currentTimeMillis();
            ItemStack attachmentStack = logicGun.getAttachment(currentGunItem, AttachmentType.MUZZLE);
            IAttachment iAttachment = IAttachment.getIAttachmentOrNull(attachmentStack);
            if (iAttachment == null) {
                return;
            }
            ResourceLocation attachmentId = iAttachment.getAttachmentId(attachmentStack);
            TimelessAPI.getCommonAttachmentIndex(attachmentId).ifPresent(index -> {
                MeleeData meleeData = index.getData().getMeleeData();
                if (meleeData == null) {
                    return;
                }
                float prepTime = meleeData.getPrepTime();
                data.meleePrepTickCount = (int) Math.max(0, prepTime * 20);
            });
        }
    }

    public void scheduleTickMelee() {
        if (this.data.meleePrepTickCount > 0) {
            this.data.meleePrepTickCount--;
            return;
        }
        if (this.data.meleePrepTickCount == 0) {
            this.data.meleePrepTickCount = -1;
            if (data.currentGunItem == null) {
                return;
            }
            ItemStack currentGunItem = data.currentGunItem.get();
            if (currentGunItem.getItem() instanceof AbstractGunItem logicGun) {
                logicGun.melee(this.shooter, currentGunItem);
            }
        }
    }

    public long getMeleeCoolDown() {
        if (data.currentGunItem == null) {
            return 0;
        }
        ItemStack currentGunItem = data.currentGunItem.get();
        if (!(currentGunItem.getItem() instanceof IGun iGun)) {
            return 0;
        }
        ResourceLocation gunId = iGun.getGunId(currentGunItem);
        Optional<CommonGunIndex> gunIndex = TimelessAPI.getCommonGunIndex(gunId);
        return gunIndex.map(index -> {
            long coolDown = (long) (index.getGunData().getMeleeData().getCooldown() * 1000) - (System.currentTimeMillis() - data.meleeTimestamp);
            // 给 5 ms 的窗口时间，以平衡延迟
            coolDown = coolDown - 5;
            if (coolDown < 0) {
                return 0L;
            }
            return coolDown;
        }).orElse(-1L);
    }
}