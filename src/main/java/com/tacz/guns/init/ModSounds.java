package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.sound.SoundManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, GunMod.MOD_ID);

    public static final RegistryObject<SoundEvent> GUN = SOUNDS.register("gun", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(GunMod.MOD_ID, "gun")));
    public static final RegistryObject<SoundEvent> TARGET_HIT = SOUNDS.register("target_block_hit", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(GunMod.MOD_ID, "target_block_hit")));

    public static final RegistryObject<SoundEvent> BULLET_FLYING_BY = SOUNDS.register("bullet_flying_by", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(GunMod.MOD_ID, "bullet_flying_by")));

    public static final RegistryObject<SoundEvent> BULLET_REFLECTION = SOUNDS.register("bullet_reflection", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(GunMod.MOD_ID, "bullet_reflection")));

    public static RegistryObject<SoundEvent> getSoundByName(String registryName) {
        return ModSounds.SOUNDS.getEntries().stream()
                .filter(entry -> entry.getId().getPath().equals(registryName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Sound not found: " + registryName));
    }
}
