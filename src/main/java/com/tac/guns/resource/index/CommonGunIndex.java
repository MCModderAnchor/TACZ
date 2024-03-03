package com.tac.guns.resource.index;

import com.tac.guns.resource.CommonAssetManager;
import com.tac.guns.resource.pojo.GunIndexPOJO;
import com.tac.guns.resource.pojo.data.GunData;
import net.minecraft.resources.ResourceLocation;

public class CommonGunIndex {
    private GunData gunData;

    public static CommonGunIndex getInstance(GunIndexPOJO gunIndexPOJO) throws IllegalArgumentException {
        CommonGunIndex index = new CommonGunIndex();

        checkData(gunIndexPOJO, index);

        return index;
    }

    private static void checkData(GunIndexPOJO gunIndexPOJO, CommonGunIndex index) {
        ResourceLocation pojoData = gunIndexPOJO.getData();
        if (pojoData == null) {
            throw new IllegalArgumentException("index object missing pojoData field");
        }
        GunData data = CommonAssetManager.INSTANCE.getGunData(pojoData);
        if (data == null) {
            throw new IllegalArgumentException("there is no corresponding data file");
        }
        index.gunData = data;
    }

    public GunData getGunData() {
        return gunData;
    }
}
