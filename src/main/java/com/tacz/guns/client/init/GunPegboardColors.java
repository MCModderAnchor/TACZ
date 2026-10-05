package com.tacz.guns.client.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.block.entity.GunPegboardBlockEntity;
import com.tacz.guns.init.ModBlocks;
import com.tacz.guns.init.ModItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = GunMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GunPegboardColors {
    @SubscribeEvent
    public static void blockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> level != null && pos != null
                && level.getBlockEntity(pos) instanceof GunPegboardBlockEntity pegboard && pegboard.getColor() >= 0
                ? pegboard.getColor() : -1, ModBlocks.GUN_PEGBOARD.get(), ModBlocks.GLOW_GUN_PEGBOARD.get());
    }

    @SubscribeEvent
    public static void itemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> ((DyeableLeatherItem) stack.getItem()).hasCustomColor(stack)
                ? ((DyeableLeatherItem) stack.getItem()).getColor(stack) : -1, ModItems.GUN_PEGBOARD.get(), ModItems.GLOW_GUN_PEGBOARD.get());
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(ModItems.GUN_PEGBOARD.get(), new ResourceLocation(GunMod.MOD_ID, "dyed"),
                (stack, level, entity, seed) -> ((DyeableLeatherItem) stack.getItem()).hasCustomColor(stack) ? 1 : 0));
        event.enqueueWork(() -> ItemProperties.register(ModItems.GLOW_GUN_PEGBOARD.get(), new ResourceLocation(GunMod.MOD_ID, "dyed"),
                (stack, level, entity, seed) -> ((DyeableLeatherItem) stack.getItem()).hasCustomColor(stack) ? 1 : 0));
    }
}
