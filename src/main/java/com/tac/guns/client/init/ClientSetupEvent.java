package com.tac.guns.client.init;

import com.tac.guns.GunMod;
import com.tac.guns.client.animation.thrid.ThirdPersonManager;
import com.tac.guns.client.gui.GunHudOverlay;
import com.tac.guns.client.input.*;
import com.tac.guns.client.tooltip.ClientAmmoBoxTooltip;
import com.tac.guns.init.ModItems;
import com.tac.guns.inventory.tooltip.AmmoBoxTooltip;
import com.tac.guns.item.AmmoBoxItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.gui.OverlayRegistry;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT, modid = GunMod.MOD_ID)
public class ClientSetupEvent {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // 注册键位
        event.enqueueWork(() -> {
            ClientRegistry.registerKeyBinding(InspectKey.INSPECT_KEY);
            ClientRegistry.registerKeyBinding(ReloadKey.RELOAD_KEY);
            ClientRegistry.registerKeyBinding(ShootKey.SHOOT_KEY);
            ClientRegistry.registerKeyBinding(FireSelectKey.FIRE_SELECT_KEY);
            ClientRegistry.registerKeyBinding(AimKey.AIM_KEY);
            ClientRegistry.registerKeyBinding(RefitKey.REFIT_KEY);
        });

        // 注册 HUD
        event.enqueueWork(() -> {
            OverlayRegistry.registerOverlayTop("TAC HUD Overlay", GunHudOverlay::render);
        });

        // 注册自己的的硬编码第三人称动画
        event.enqueueWork(ThirdPersonManager::registerInner);

        // 注册颜色
        event.enqueueWork(() -> Minecraft.getInstance().getItemColors().register(AmmoBoxItem::getColor, ModItems.AMMO_BOX.get()));

        // 注册变种
        // noinspection deprecation
        event.enqueueWork(() -> ItemProperties.register(ModItems.AMMO_BOX.get(), AmmoBoxItem.PROPERTY_NAME, AmmoBoxItem::getStatue));

        // 注册文本提示
        event.enqueueWork(() -> MinecraftForgeClient.registerTooltipComponentFactory(AmmoBoxTooltip.class, ClientAmmoBoxTooltip::new));
    }
}
