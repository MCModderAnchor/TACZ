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
    public static void onSetupEvent(FMLCommonSetupEvent event) {
        event.enqueueWork(ModSyncedEntityData::init);
        event.enqueueWork(TargetHitTrigger::register);
        event.enqueueWork(GunRefitTrigger::register);
        event.enqueueWork(() -> CauldronInteraction.WATER.put(ModItems.GUN_PEGBOARD.get(), CauldronInteraction.DYED_ITEM));
        event.enqueueWork(() -> CauldronInteraction.WATER.put(ModItems.GLOW_GUN_PEGBOARD.get(), CauldronInteraction.DYED_ITEM));
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
