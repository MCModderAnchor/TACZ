package com.tacz.guns.entity.sync;

import net.minecraft.world.entity.Entity;

/**
 * SyncedEntityData 注册方法的转发
 */
public class SyncedEntityDataManager {
    public static void registerEntityData(SyncedDataKey<? extends Entity, ?> dataKey) {
        SyncedEntityData.instance().registerDataKey(dataKey);
    }
}
