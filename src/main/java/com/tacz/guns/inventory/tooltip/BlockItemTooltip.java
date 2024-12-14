package com.tacz.guns.inventory.tooltip;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public class BlockItemTooltip implements TooltipComponent {
    private final ResourceLocation blockId;

    public BlockItemTooltip(ResourceLocation blockId) {
        this.blockId = blockId;
    }

    public ResourceLocation getBlockId() {
        return blockId;
    }
}
