package com.tacz.guns.resource_legacy.loader;

import com.tacz.guns.resource.filter.RecipeFilter;
import com.tacz.guns.resource.network.DataType;
import com.tacz.guns.resource.pojo.data.block.BlockData;

public class CommonDataLoaders {
    public static final DataLoader<RecipeFilter> RECIPE_FILTER = new DataLoader<>(DataType.RECIPE_FILTER,
            RecipeFilter.class,
            "RecipeFilter",
            "filters");

    public static final DataLoader<BlockData> BLOCKS = new DataLoader<>(DataType.BLOCK_DATA,
            BlockData.class,
            "BlockData",
            "blocks/data");
}
