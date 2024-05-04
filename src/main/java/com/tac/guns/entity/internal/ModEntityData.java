package com.tac.guns.entity.internal;

import com.tac.guns.GunMod;
import com.tac.guns.api.gun.ReloadState;
import com.tac.guns.api.sync.Serializers;
import com.tac.guns.api.sync.SyncedClassKey;
import com.tac.guns.api.sync.SyncedDataKey;
import com.tac.guns.api.sync.SyncedEntityDataManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public class ModEntityData {
    public static final SyncedDataKey<LivingEntity, Long> SHOOT_COOL_DOWN_KEY  = SyncedDataKey.builder(SyncedClassKey.LIVING_ENTITY, Serializers.LONG)
            .id(new ResourceLocation(GunMod.MOD_ID, "shoot_cool_down"))
            .defaultValueSupplier(() -> -1L)
            .syncMode(SyncedDataKey.SyncMode.ALL)
            .build();

    public static final SyncedDataKey<LivingEntity, ReloadState> RELOAD_STATE_KEY  = SyncedDataKey.builder(SyncedClassKey.LIVING_ENTITY, ModSerializers.RELOAD_STATE)
            .id(new ResourceLocation(GunMod.MOD_ID, "reload_state"))
            .defaultValueSupplier(ReloadState::new)
            .syncMode(SyncedDataKey.SyncMode.ALL)
            .build();

    public static final SyncedDataKey<LivingEntity, Float> AIMING_PROGRESS_KEY  = SyncedDataKey.builder(SyncedClassKey.LIVING_ENTITY, Serializers.FLOAT)
            .id(new ResourceLocation(GunMod.MOD_ID, "aiming_progress"))
            .defaultValueSupplier(() -> 0f)
            .syncMode(SyncedDataKey.SyncMode.ALL)
            .build();

    public static final SyncedDataKey<LivingEntity, Long> DRAW_COOL_DOWN_KEY  = SyncedDataKey.builder(SyncedClassKey.LIVING_ENTITY, Serializers.LONG)
            .id(new ResourceLocation(GunMod.MOD_ID, "draw_cool_down"))
            .defaultValueSupplier(() -> -1L)
            .syncMode(SyncedDataKey.SyncMode.ALL)
            .build();

    public static final SyncedDataKey<LivingEntity, Boolean> IS_AIMING_KEY  = SyncedDataKey.builder(SyncedClassKey.LIVING_ENTITY, Serializers.BOOLEAN)
            .id(new ResourceLocation(GunMod.MOD_ID, "is_aiming"))
            .defaultValueSupplier(() -> false)
            .syncMode(SyncedDataKey.SyncMode.ALL)
            .build();

    public static final SyncedDataKey<LivingEntity, Float> SPRINT_TIME_KEY  = SyncedDataKey.builder(SyncedClassKey.LIVING_ENTITY, Serializers.FLOAT)
            .id(new ResourceLocation(GunMod.MOD_ID, "sprint_time"))
            .defaultValueSupplier(() -> 0f)
            .syncMode(SyncedDataKey.SyncMode.ALL)
            .build();

    public static final SyncedDataKey<LivingEntity, Long> BOLT_COOL_DOWN_KEY  = SyncedDataKey.builder(SyncedClassKey.LIVING_ENTITY, Serializers.LONG)
            .id(new ResourceLocation(GunMod.MOD_ID, "bolt_cool_down"))
            .defaultValueSupplier(() -> -1L)
            .syncMode(SyncedDataKey.SyncMode.ALL)
            .build();
    public static void init(){
        SyncedEntityDataManager.registerEntityData(SHOOT_COOL_DOWN_KEY);
        SyncedEntityDataManager.registerEntityData(RELOAD_STATE_KEY);
        SyncedEntityDataManager.registerEntityData(AIMING_PROGRESS_KEY);
        SyncedEntityDataManager.registerEntityData(DRAW_COOL_DOWN_KEY);
        SyncedEntityDataManager.registerEntityData(IS_AIMING_KEY);
        SyncedEntityDataManager.registerEntityData(SPRINT_TIME_KEY);
        SyncedEntityDataManager.registerEntityData(BOLT_COOL_DOWN_KEY);
    }
}
