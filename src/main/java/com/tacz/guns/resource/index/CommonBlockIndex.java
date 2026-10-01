package com.tacz.guns.resource.index;

import com.google.common.base.Preconditions;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.filter.RecipeFilter;
import com.tacz.guns.resource.pojo.BlockIndexPOJO;
import com.tacz.guns.resource.pojo.data.block.BlockData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;

public class CommonBlockIndex {

    private BlockIndexPOJO pojo;
    private BlockItem block;
    private BlockData data;
    private RecipeFilter filter;

    public static CommonBlockIndex getInstance(BlockIndexPOJO gunIndexPOJO) throws IllegalArgumentException {
        CommonBlockIndex index = new CommonBlockIndex();
        index.pojo = gunIndexPOJO;
        checkIndex(gunIndexPOJO, index);
        checkData(gunIndexPOJO, index);
        return index;
    }

    private static void checkIndex(BlockIndexPOJO block, CommonBlockIndex index) {
        ResourceLocation id = index.pojo.getId();
        Preconditions.checkArgument(block != null, "index object file is empty");
        if(!(BuiltInRegistries.ITEM.get(id) instanceof BlockItem item)) {
            throw new IllegalArgumentException("BlockItem not found for " + block.getName());
        }
        index.block = item;
    }

    private static void checkData(BlockIndexPOJO block, CommonBlockIndex index) {
        ResourceLocation pojoData = block.getData();
        Preconditions.checkArgument(pojoData != null, "index object missing pojoData field");
        BlockData data = CommonAssetsManager.get().getBlockData(pojoData);
        Preconditions.checkArgument(data != null, "there is no corresponding data file");
        RecipeFilter recipeFilter = CommonAssetsManager.get().getRecipeFilter(data.getFilter());
        Preconditions.checkArgument(recipeFilter != null, "there is no corresponding data file");
        index.data = data;
        index.filter = recipeFilter;
    }

    public BlockIndexPOJO getPojo() {
        return pojo;
    }

    public BlockItem getBlock() {
        return block;
    }

    public BlockData getData() {
        return data;
    }

    public RecipeFilter getFilter() {
        return filter;
    }
}
