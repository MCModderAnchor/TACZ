package com.tac.guns.resource.index;

import com.tac.guns.resource.pojo.AmmoIndexPOJO;

public class CommonAmmoIndex {
    private int stackSize;

    private CommonAmmoIndex() {
    }

    public static CommonAmmoIndex getInstance(AmmoIndexPOJO ammoIndexPOJO) throws IllegalArgumentException {
        CommonAmmoIndex index = new CommonAmmoIndex();
        index.stackSize = Math.max(ammoIndexPOJO.getStackSize(), 1);
        return index;
    }

    public int getStackSize() {
        return stackSize;
    }
}