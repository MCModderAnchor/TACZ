package com.tacz.guns.init;

import com.tacz.guns.advancement.TargetHitTrigger;
import com.tacz.guns.advancement.GunRefitTrigger;
import com.tacz.guns.entity.sync.ModSyncedEntityData;
import com.tacz.guns.resource.GunPackLoader;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public final class CommonRegistry {
    private static boolean LOAD_COMPLETE = false;

    @SubscribeEvent
    public static void registerCapabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                ModBlocks.AMMO_STORAGE_BE.get(), (storage, side) -> storage.isRemoved() ? null : storage.getItemHandler());
    }

    @SubscribeEvent
    public static void registerCriteria(net.neoforged.neoforge.registries.RegisterEvent event) {
        event.register(net.minecraft.core.registries.Registries.TRIGGER_TYPE, helper -> {
            helper.register(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("tacz", "target_hit"), TargetHitTrigger.INSTANCE);
            helper.register(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("tacz", "gun_refit"), GunRefitTrigger.INSTANCE);
        });
    }

    @SubscribeEvent
    public static void onSetupEvent(FMLCommonSetupEvent event) {
        event.enqueueWork(ModSyncedEntityData::init);
        event.enqueueWork(() -> CauldronInteraction.WATER.map().put(ModItems.GUN_PEGBOARD.get(), CauldronInteraction.DYED_ITEM));
        event.enqueueWork(() -> CauldronInteraction.WATER.map().put(ModItems.GLOW_GUN_PEGBOARD.get(), CauldronInteraction.DYED_ITEM));
    }

    @SubscribeEvent
    public static void onLoadComplete(FMLLoadCompleteEvent event) {
        LOAD_COMPLETE = true;
    }

    public static boolean isLoadComplete() {
        return LOAD_COMPLETE;
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeModificationEvent event) {
        event.getTypes().forEach(type -> {
            event.add(type, ModAttributes.BULLET_RESISTANCE.getDelegate());
        });
    }

    @SubscribeEvent
    public static void onAddPackFinders(AddPackFindersEvent event) {
        event.addRepositorySource(GunPackLoader.INSTANCE);
    }
}
