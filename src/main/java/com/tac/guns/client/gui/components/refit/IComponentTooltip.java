package com.tac.guns.client.gui.components.refit;

import net.minecraft.network.chat.MutableComponent;

import java.util.function.Consumer;

public interface IComponentTooltip {
    /**
     * 添加此接口，会调用此渲染文本提示
     *
     * @param consumer 需要渲染的文本提示
     */
    void renderTooltip(Consumer<MutableComponent> consumer);
}
