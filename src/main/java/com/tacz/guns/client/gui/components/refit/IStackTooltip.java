package com.tacz.guns.client.gui.components.refit;

import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

public interface IStackTooltip {
    /**
     * 添加此接口，会调用此渲染文本提示
     *
     * @param consumer 需要渲染文本提示的物品
     */
    void renderTooltip(Consumer<ItemStack> consumer);
}
