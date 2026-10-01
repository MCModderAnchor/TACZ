package com.tacz.guns.init;

import com.tacz.guns.GunMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(BuiltInRegistries.ATTRIBUTE, GunMod.MOD_ID);

    public static final DeferredHolder<Attribute, Attribute> BULLET_RESISTANCE = ATTRIBUTES.register("tacz.bullet_resistance",
            () -> new RangedAttribute("attribute.name.tacz.bullet_resistance", 0.0D, 0.0D, 1.0D).setSyncable(true));
//
//    public static final RegistryObject<Attribute> WEIGHT_CAPACITY = ATTRIBUTES.register("tacz.weight_capacity",
//            () -> new RangedAttribute("attribute.name.tacz.weight_capacity", 0.0D, -1024D, 1024.0D).setSyncable(true));
}
