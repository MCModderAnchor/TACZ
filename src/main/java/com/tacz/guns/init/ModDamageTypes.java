package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;

public class ModDamageTypes {
    public static final ResourceKey<DamageType> BULLET = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(GunMod.MOD_ID, "bullet"));
    public static final ResourceKey<DamageType> BULLET_IGNORE_ARMOR = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(GunMod.MOD_ID, "bullet_ignore_armor"));
    public static final ResourceKey<DamageType> BULLET_VOID = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(GunMod.MOD_ID, "bullet_void"));
    public static final ResourceKey<DamageType> BULLET_VOID_IGNORE_ARMOR = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(GunMod.MOD_ID, "bullet_void_ignore_armor"));

    public static final TagKey<DamageType> BULLETS_TAG = TagKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(GunMod.MOD_ID, "bullets"));

    public static class Sources {
        private static Holder.Reference<DamageType> getHolder(RegistryAccess access, ResourceKey<DamageType> damageTypeKey) {
            return access.registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(damageTypeKey);
        }

        public static DamageSource bullet(RegistryAccess access, Entity bullet, Entity shooter, boolean ignoreArmor) {
            return new DamageSource(getHolder(access, ignoreArmor ? BULLET_IGNORE_ARMOR : BULLET), bullet, shooter);
        }

        public static DamageSource bulletVoid(RegistryAccess access, Entity bullet, Entity shooter, boolean ignoreArmor) {
            return new DamageSource(getHolder(access, ignoreArmor ? BULLET_VOID_IGNORE_ARMOR : BULLET_VOID), bullet, shooter);
        }
    }
}
