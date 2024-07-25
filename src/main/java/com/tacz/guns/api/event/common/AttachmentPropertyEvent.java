package com.tacz.guns.api.event.common;

import com.tacz.guns.entity.shooter.AttachmentProperty;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Event;

/**
 * 缓存配件属性修改值时触发的事件
 * <p>
 * 如果有其他模组想要添加自定义的配件属性修改值，可以捕获此事件
 */
public class AttachmentPropertyEvent extends Event {
    private final ItemStack gunItem;
    private final AttachmentProperty property;

    public AttachmentPropertyEvent(ItemStack gunItem, AttachmentProperty attachmentProperty) {
        this.gunItem = gunItem;
        this.property = attachmentProperty;
    }

    public ItemStack getGunItem() {
        return gunItem;
    }

    public AttachmentProperty getProperty() {
        return property;
    }
}
