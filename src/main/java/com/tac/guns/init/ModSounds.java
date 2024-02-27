package com.tac.guns.init;

import com.tac.guns.GunMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, GunMod.MOD_ID);

    public static final RegistryObject<SoundEvent> FIRE = registerSound("gun.ak47_fire");

    private static RegistryObject<SoundEvent> registerSound(String name) {
        return SOUNDS.register(name, () -> new SoundEvent(new ResourceLocation(GunMod.MOD_ID, name)));
    }
}
