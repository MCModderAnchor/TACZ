package com.tacz.guns.resource.index;

import com.tacz.guns.resource.pojo.BlockIndexPOJO;

public class CommonBlockIndex {
    private BlockIndexPOJO pojo;

    public static CommonBlockIndex getInstance(BlockIndexPOJO gunIndexPOJO) throws IllegalArgumentException {
        CommonBlockIndex index = new CommonBlockIndex();
        index.pojo = gunIndexPOJO;
        return index;
    }

    public BlockIndexPOJO getPojo() {
        return pojo;
    }
}
