package com.tacz.guns.resource.index;

import com.google.common.base.Preconditions;
import com.tacz.guns.resource.pojo.AmmoIndexPOJO;

public class CommonAmmoIndex {
    private int stackSize;
    private AmmoIndexPOJO pojo;

    private CommonAmmoIndex() {
    }

    public static CommonAmmoIndex getInstance(AmmoIndexPOJO ammoIndexPOJO) throws IllegalArgumentException {
        CommonAmmoIndex index = new CommonAmmoIndex();
        index.pojo = ammoIndexPOJO;
        checkIndex(ammoIndexPOJO, index);
        return index;
    }

    private static void checkIndex(AmmoIndexPOJO ammoIndexPOJO, CommonAmmoIndex index) {
        Preconditions.checkArgument(ammoIndexPOJO != null, "index object file is empty");
        index.stackSize = Math.max(ammoIndexPOJO.getStackSize(), 1);
    }

    public int getStackSize() {
        return stackSize;
    }

    public AmmoIndexPOJO getPojo() {
        return pojo;
    }
}