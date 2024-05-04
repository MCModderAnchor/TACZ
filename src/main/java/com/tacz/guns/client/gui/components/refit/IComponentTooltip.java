package com.tacz.guns.client.gui.components.refit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.function.Consumer;

public interface IComponentTooltip {
    /**
     * 获取物品的文本提示
     */
    static List<Component> getTooltipFromItem(ItemStack stack) {
        Options options = Minecraft.getInstance().options;
        LocalPlayer player = Minecraft.getInstance().player;
        return stack.getTooltipLines(player, options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL);
    }

    /**
     * 添加此接口，会调用此渲染文本提示
     *
     * @param consumer 需要渲染的文本提示
     */
    void renderTooltip(Consumer<List<Component>> consumer);
}
