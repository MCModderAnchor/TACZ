package com.tacz.guns.resource_legacy.loader.index;

import com.tacz.guns.resource_legacy.CommonGunPackLoader;
import com.tacz.guns.resource.index.CommonBlockIndex;
import com.tacz.guns.resource_legacy.loader.IndexLoader;
import com.tacz.guns.resource.network.DataType;
import com.tacz.guns.resource.pojo.BlockIndexPOJO;

public class CommonIndexLoaders {
    public static final IndexLoader<BlockIndexPOJO, CommonBlockIndex> BLOCK = new IndexLoader<>(DataType.BLOCK_INDEX,
            BlockIndexPOJO.class,
            CommonBlockIndex.class,
            "BlockIndex",
            "blocks/index",
            CommonGunPackLoader.BLOCK_INDEX::put,
            CommonBlockIndex::getInstance);
}
