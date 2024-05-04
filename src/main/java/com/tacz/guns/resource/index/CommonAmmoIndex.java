package com.tacz.guns.resource.index;

import com.tacz.guns.resource.pojo.AmmoIndexPOJO;

public class CommonAmmoIndex {
    private int stackSize;

    private CommonAmmoIndex() {
    }

    public static CommonAmmoIndex getInstance(AmmoIndexPOJO ammoIndexPOJO) throws IllegalArgumentException {
        CommonAmmoIndex index = new CommonAmmoIndex();
        checkIndex(ammoIndexPOJO, index);
        return index;
    }

    private static void checkIndex(AmmoIndexPOJO ammoIndexPOJO, CommonAmmoIndex index) {
        if (ammoIndexPOJO == null) {
            throw new IllegalArgumentException("index object file is empty");
        }
        index.stackSize = Math.max(ammoIndexPOJO.getStackSize(), 1);
    }

    public int getStackSize() {
        return stackSize;
    }
}