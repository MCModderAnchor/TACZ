package com.tac.guns.resource.index;

import com.tac.guns.api.gun.FireMode;
import com.tac.guns.item.nbt.GunItemData;
import com.tac.guns.resource.CommonAssetManager;
import com.tac.guns.resource.pojo.GunIndexPOJO;
import com.tac.guns.resource.pojo.data.GunData;
import com.tac.guns.resource.pojo.data.InaccuracyType;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

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
        // TODO 检查 ammo 是否存在
        if (data.getAmmoId() == null) {
            throw new IllegalArgumentException("ammo id is empty");
        }
        if (data.getAmmoAmount() < 1) {
            throw new IllegalArgumentException("ammo count must >= 1");
        }
        if (data.getRoundsPerMinute() < 1) {
            throw new IllegalArgumentException("rpm count must >= 1");
        }
        if (data.getBolt() == null) {
            throw new IllegalArgumentException("bolt type is error");
        }
        if (data.getReloadData().getType() == null) {
            throw new IllegalArgumentException("reload type is error");
        }
        if (data.getFireModeSet().isEmpty()) {
            throw new IllegalArgumentException("fire mode is empty");
        }
        if (data.getFireModeSet().contains(null) || data.getFireModeSet().contains(FireMode.UNKNOWN)) {
            throw new IllegalArgumentException("fire mode is error");
        }
        checkInaccuracy(data);
        index.gunData = data;
    }

    private static void checkInaccuracy(GunData data) {
        GunData defaultData = CommonAssetManager.INSTANCE.getGunData(GunItemData.DEFAULT_DATA);
        Map<InaccuracyType, Float> dataInaccuracy = defaultData.getInaccuracy();
        Map<InaccuracyType, Float> readInaccuracy = data.getInaccuracy();
        if (readInaccuracy == null || readInaccuracy.isEmpty()) {
            data.setInaccuracy(dataInaccuracy);
        } else {
            dataInaccuracy.forEach(readInaccuracy::putIfAbsent);
        }
    }

    public GunData getGunData() {
        return gunData;
    }
}
