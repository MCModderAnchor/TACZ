package com.tacz.guns.api.item.builder;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.item.IBlock;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public final class BlockItemBuilder {
    private int count = 1;
    private ResourceLocation blockId = DefaultAssets.DEFAULT_BLOCK_ID;
    private final ItemLike blockItem;

    private BlockItemBuilder(ItemLike blockItem) {
        this.blockItem = blockItem;
    }

    public static BlockItemBuilder create(ItemLike blockItem) {
        return new BlockItemBuilder(blockItem);
    }

    public BlockItemBuilder setCount(int count) {
        this.count = Math.max(count, 1);
        return this;
    }

    public BlockItemBuilder setId(ResourceLocation id) {
        this.blockId = id;
        return this;
    }

    public ItemStack build() {
        ItemStack block = new ItemStack(blockItem ,this.count);
        if (block.getItem() instanceof IBlock iBlock) {
            iBlock.setBlockId(block, this.blockId);
        }
        return block;
    }
}
