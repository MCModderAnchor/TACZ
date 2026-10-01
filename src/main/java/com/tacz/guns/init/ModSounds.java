package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, GunMod.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> GUN = SOUNDS.register("gun", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "gun")));
    public static final DeferredHolder<SoundEvent, SoundEvent> TARGET_HIT = SOUNDS.register("target_block_hit", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "target_block_hit")));
}
