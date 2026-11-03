package com.tacz.guns.api.event.common;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.LogicalSide;
import org.jetbrains.annotations.Nullable;

/**
 * 换弹流程结束或被打断后触发一次。
 * 不可取消的结束通知，此时弹药已经结算；阻止换弹请取消 {@link GunReloadEvent}。
 */
public class GunFinishReloadEvent extends Event implements KubeJSGunEventPoster<GunFinishReloadEvent>{
    private final LivingEntity entity;
    private final ItemStack gunItemStack;
    private final LogicalSide logicalSide;

    public GunFinishReloadEvent(LivingEntity entity, ItemStack gunItemStack, LogicalSide side) {
        this.entity = entity;
        this.gunItemStack = gunItemStack;
        this.logicalSide = side;
        postEventToKubeJS(this);
    }

    @Nullable
    public LivingEntity getEntity() {
        return entity;
    }

    public ItemStack getGunItemStack() {
        return gunItemStack;
    }

    public LogicalSide getLogicalSide() {
        return logicalSide;
    }
}
