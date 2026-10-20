package com.tacz.guns.client.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.block.entity.GunPegboardBlockEntity;
import com.tacz.guns.init.ModBlocks;
import com.tacz.guns.init.ModItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import com.tacz.guns.item.GunPegboardItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@net.neoforged.fml.common.EventBusSubscriber(modid = GunMod.MOD_ID, bus = net.neoforged.fml.common.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GunPegboardColors {
    @SubscribeEvent
    public static void blockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> level != null && pos != null
                && level.getBlockEntity(pos) instanceof GunPegboardBlockEntity pegboard && pegboard.getColor() >= 0
                ? pegboard.getColor() : -1, ModBlocks.GUN_PEGBOARD.get(), ModBlocks.GLOW_GUN_PEGBOARD.get());
    }

    @SubscribeEvent
    public static void itemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> GunPegboardItem.hasCustomColor(stack)
                ? net.minecraft.util.FastColor.ARGB32.opaque(GunPegboardItem.getColor(stack)) : -1, ModItems.GUN_PEGBOARD.get(), ModItems.GLOW_GUN_PEGBOARD.get());
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(ModItems.GUN_PEGBOARD.get(), ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "dyed"),
                (stack, level, entity, seed) -> GunPegboardItem.hasCustomColor(stack) ? 1 : 0));
        event.enqueueWork(() -> ItemProperties.register(ModItems.GLOW_GUN_PEGBOARD.get(), ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "dyed"),
                (stack, level, entity, seed) -> GunPegboardItem.hasCustomColor(stack) ? 1 : 0));
    }
}
