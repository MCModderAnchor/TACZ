package com.tacz.guns.client.event;

import com.tacz.guns.api.item.nbt.AmmoItemDataAccessor;
import com.tacz.guns.api.item.nbt.AttachmentItemDataAccessor;
import com.tacz.guns.api.item.nbt.BlockItemDataAccessor;
import com.tacz.guns.api.item.nbt.GunItemDataAccessor;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber
public class TooltipEvent {
    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (event.getFlags().isAdvanced() && RenderConfig.ENABLE_TACZ_ID_IN_TOOLTIP.get()) {
            if (event.getItemStack().getItem() instanceof GunItemDataAccessor item) {
                event.getToolTip().add(formatTooltip(GunItemDataAccessor.GUN_ID_TAG, item.getGunId(event.getItemStack())));
            } else if (event.getItemStack().getItem() instanceof AmmoItemDataAccessor item) {
                event.getToolTip().add(formatTooltip(AmmoItemDataAccessor.AMMO_ID_TAG, item.getAmmoId(event.getItemStack())));
            } else if (event.getItemStack().getItem() instanceof AttachmentItemDataAccessor item) {
                event.getToolTip().add(formatTooltip(AttachmentItemDataAccessor.ATTACHMENT_ID_TAG, item.getAttachmentId(event.getItemStack())));
            } else if (event.getItemStack().getItem() instanceof BlockItemDataAccessor item && !ModItems.GUN_SMITH_TABLE.get().equals(item)) {
                event.getToolTip().add(formatTooltip(BlockItemDataAccessor.BLOCK_ID, item.getBlockId(event.getItemStack())));
            }
        }
    }

    public static Component formatTooltip(String key, ResourceLocation value) {
        return Component.literal(String.format("%s: \"%s\"", key, value)).withStyle(ChatFormatting.DARK_GRAY);
    }
}
