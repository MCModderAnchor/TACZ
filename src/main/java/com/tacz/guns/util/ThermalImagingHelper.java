package com.tacz.guns.util;

import com.tacz.guns.GunMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

/** Entity-type rules shared by thermal imaging; tag membership is read live after data-pack reloads. */
public final class ThermalImagingHelper {
    public static final TagKey<EntityType<?>> BLACKLIST = TagKey.create(Registries.ENTITY_TYPE,
            new ResourceLocation(GunMod.MOD_ID, "thermal_imaging/blacklist"));
    public static final TagKey<EntityType<?>> WHITELIST = TagKey.create(Registries.ENTITY_TYPE,
            new ResourceLocation(GunMod.MOD_ID, "thermal_imaging/whitelist"));

    private ThermalImagingHelper() {
    }

    /** A blacklist veto takes precedence, including when a type also appears in the whitelist. */
    public static boolean isHeatSource(Entity entity) {
        EntityType<?> type = entity.getType();
        return !type.is(BLACKLIST) && (entity instanceof LivingEntity || type.is(WHITELIST));
    }
}
