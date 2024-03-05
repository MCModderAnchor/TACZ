package com.tac.guns.init;

import com.tac.guns.GunMod;
import com.tac.guns.entity.serializer.ModEntityDataSerializers;
import com.tac.guns.entity.EntityBullet;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.DataSerializerEntry;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITIES, GunMod.MOD_ID);
    public static final DeferredRegister<DataSerializerEntry> DATA_SERIALIZERS = DeferredRegister.create(ForgeRegistries.Keys.DATA_SERIALIZERS, GunMod.MOD_ID);

    public static RegistryObject<EntityType<EntityBullet>> BULLET = ENTITY_TYPES.register("bullet", () -> EntityBullet.TYPE);
    public static RegistryObject<DataSerializerEntry> LONG_SERIALIZERS = DATA_SERIALIZERS.register("long", () -> new DataSerializerEntry(ModEntityDataSerializers.LONG));
    public static RegistryObject<DataSerializerEntry> RELOAD_STATE_SERIALIZERS = DATA_SERIALIZERS.register("reload_state", () -> new DataSerializerEntry(ModEntityDataSerializers.RELOAD_STATE));
}