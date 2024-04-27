package com.tac.guns.resource.index;

import com.google.common.collect.Maps;
import com.tac.guns.api.gun.FireMode;
import com.tac.guns.resource.CommonAssetManager;
import com.tac.guns.resource.DefaultAssets;
import com.tac.guns.resource.pojo.GunIndexPOJO;
import com.tac.guns.resource.pojo.data.gun.*;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Map;

public class CommonGunIndex {
    private GunData gunData;
    private String type;

    private CommonGunIndex() {
    }

    public static CommonGunIndex getInstance(GunIndexPOJO gunIndexPOJO) throws IllegalArgumentException {
        CommonGunIndex index = new CommonGunIndex();
        checkIndex(gunIndexPOJO, index);
        checkData(gunIndexPOJO, index);
        return index;
    }

    private static void checkIndex(GunIndexPOJO gunIndexPOJO, CommonGunIndex index) {
        if (gunIndexPOJO == null) {
            throw new IllegalArgumentException("index object file is empty");
        }
        if (StringUtils.isBlank(gunIndexPOJO.getType())) {
            throw new IllegalArgumentException("index object missing type field");
        }
        index.type = gunIndexPOJO.getType();
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
        if (data.getAmmoId() == null) {
            throw new IllegalArgumentException("ammo id is empty");
        }
        if (data.getAmmoAmount() < 1) {
            throw new IllegalArgumentException("ammo count must >= 1");
        }
        int[] extendedMagAmmoAmount = data.getExtendedMagAmmoAmount();
        if (extendedMagAmmoAmount != null && extendedMagAmmoAmount.length < 3) {
            throw new IllegalArgumentException("extended_mag_ammo_amount size must is 3");
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
        checkRecoil(data);
        index.gunData = data;
    }

    private static void checkInaccuracy(GunData data) {
        GunData defaultData = CommonAssetManager.INSTANCE.getGunData(DefaultAssets.DEFAULT_GUN_DATA);
        Map<InaccuracyType, Float> defaultInaccuracy = Maps.newHashMap(defaultData.getInaccuracy());
        Map<InaccuracyType, Float> readInaccuracy = data.getInaccuracy();
        if (readInaccuracy == null || readInaccuracy.isEmpty()) {
            data.setInaccuracy(defaultInaccuracy);
        } else {
            defaultInaccuracy.forEach(readInaccuracy::putIfAbsent);
        }
    }

    private static void checkRecoil(GunData data) {
        GunRecoil recoil = data.getRecoil();
        GunRecoilKeyFrame[] pitch = recoil.getPitch();
        GunRecoilKeyFrame[] yaw = recoil.getYaw();
        if (pitch != null) {
            for (GunRecoilKeyFrame keyFrame : pitch) {
                float[] value = keyFrame.getValue();
                if (value.length != 2) {
                    throw new IllegalArgumentException("Recoil value's length must be 2");
                }
                if (value[0] > value[1]) {
                    throw new IllegalArgumentException("Recoil value's left must be less than right");
                }
                if (keyFrame.getTime() < 0) {
                    throw new IllegalArgumentException("Recoil time must be more than 0");
                }
            }
            Arrays.sort(pitch);
        }

        if (yaw != null) {
            for (GunRecoilKeyFrame keyFrame : yaw) {
                float[] value = keyFrame.getValue();
                if (value.length != 2) {
                    throw new IllegalArgumentException("Recoil value's length must be 2");
                }
                if (value[0] > value[1]) {
                    throw new IllegalArgumentException("Recoil value's left must be less than right");
                }
                if (keyFrame.getTime() < 0) {
                    throw new IllegalArgumentException("Recoil time must be more than 0");
                }
            }
            Arrays.sort(yaw);
        }
    }

    public GunData getGunData() {
        return gunData;
    }

    public BulletData getBulletData() {
        return gunData.getBulletData();
    }

    public String getType() {
        return type;
    }
}
