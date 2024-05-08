package com.tacz.guns.resource.index;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.resource.CommonAssetManager;
import com.tacz.guns.resource.DefaultAssets;
import com.tacz.guns.resource.pojo.GunIndexPOJO;
import com.tacz.guns.resource.pojo.data.gun.*;
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
        Preconditions.checkArgument(gunIndexPOJO != null, "index object file is empty");
        Preconditions.checkArgument(StringUtils.isNoneBlank(gunIndexPOJO.getType()), "index object missing type field");
        index.type = gunIndexPOJO.getType();
    }

    private static void checkData(GunIndexPOJO gunIndexPOJO, CommonGunIndex index) {
        ResourceLocation pojoData = gunIndexPOJO.getData();
        Preconditions.checkArgument(pojoData != null, "index object missing pojoData field");
        GunData data = CommonAssetManager.INSTANCE.getGunData(pojoData);
        Preconditions.checkArgument(data != null, "there is no corresponding data file");
        Preconditions.checkArgument(data.getAmmoId() != null, "ammo id is empty");
        Preconditions.checkArgument(data.getAmmoAmount() >= 1, "ammo count must >= 1");
        int[] extendedMagAmmoAmount = data.getExtendedMagAmmoAmount();
        Preconditions.checkArgument(extendedMagAmmoAmount == null || extendedMagAmmoAmount.length >= 3, "extended_mag_ammo_amount size must is 3");
        Preconditions.checkArgument(data.getRoundsPerMinute() >= 1, "rpm count must >= 1");
        Preconditions.checkArgument(data.getBolt() != null, "bolt type is error");
        Preconditions.checkArgument(data.getReloadData().getType() != null, "reload type is error");
        Preconditions.checkArgument(!data.getFireModeSet().isEmpty(), "fire mode is empty");
        Preconditions.checkArgument(!data.getFireModeSet().contains(null) && !data.getFireModeSet().contains(FireMode.UNKNOWN), "fire mode is error");
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
                Preconditions.checkArgument(value.length == 2, "Recoil value's length must be 2");
                Preconditions.checkArgument(value[0] <= value[1], "Recoil value's left must be less than right");
                Preconditions.checkArgument(keyFrame.getTime() >= 0, "Recoil time must be more than 0");
            }
            Arrays.sort(pitch);
        }

        if (yaw != null) {
            for (GunRecoilKeyFrame keyFrame : yaw) {
                float[] value = keyFrame.getValue();
                Preconditions.checkArgument(value.length == 2, "Recoil value's length must be 2");
                Preconditions.checkArgument(value[0] <= value[1], "Recoil value's left must be less than right");
                Preconditions.checkArgument(keyFrame.getTime() >= 0, "Recoil time must be more than 0");
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
