package com.tacz.guns.api.item.nbt;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.item.IBlock;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public interface BlockItemDataAccessor extends IBlock {
    String BLOCK_ID = "BlockId";

    @Override
    @Nonnull
    default ResourceLocation getBlockId(ItemStack block) {
        CompoundTag nbt = block.getOrCreateTag();
        if (nbt.contains(BLOCK_ID, Tag.TAG_STRING)) {
            ResourceLocation gunId = ResourceLocation.tryParse(nbt.getString(BLOCK_ID));
            return Objects.requireNonNullElse(gunId, DefaultAssets.EMPTY_BLOCK_ID);
        }
        return DefaultAssets.EMPTY_BLOCK_ID;
    }

    @Override
    default void setBlockId(ItemStack block, @Nullable ResourceLocation blockId) {
        CompoundTag nbt = block.getOrCreateTag();
        if (blockId != null) {
            nbt.putString(BLOCK_ID, blockId.toString());
            return;
        }
        nbt.putString(BLOCK_ID, DefaultAssets.EMPTY_BLOCK_ID.toString());
    }

}
