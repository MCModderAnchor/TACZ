package com.tacz.guns.client.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.client.other.ThirdPersonManager;
import com.tacz.guns.client.download.ClientGunPackDownloadManager;
import com.tacz.guns.client.gui.overlay.GunHudOverlay;
import com.tacz.guns.client.gui.overlay.InteractKeyTextOverlay;
import com.tacz.guns.client.gui.overlay.KillAmountOverlay;
import com.tacz.guns.client.input.*;
import com.tacz.guns.client.tooltip.ClientAmmoBoxTooltip;
import com.tacz.guns.client.tooltip.ClientAttachmentItemTooltip;
import com.tacz.guns.client.tooltip.ClientGunTooltip;
import com.tacz.guns.init.ModItems;
import com.tacz.guns.inventory.tooltip.AmmoBoxTooltip;
import com.tacz.guns.inventory.tooltip.AttachmentItemTooltip;
import com.tacz.guns.inventory.tooltip.GunTooltip;
import com.tacz.guns.item.AmmoBoxItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.gui.ForgeIngameGui;
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
            ClientRegistry.registerKeyBinding(InteractKey.INTERACT_KEY);
            ClientRegistry.registerKeyBinding(FireSelectKey.FIRE_SELECT_KEY);
            ClientRegistry.registerKeyBinding(AimKey.AIM_KEY);
            ClientRegistry.registerKeyBinding(RefitKey.REFIT_KEY);
            ClientRegistry.registerKeyBinding(ZoomKey.ZOOM_KEY);
            ClientRegistry.registerKeyBinding(MeleeKey.MELEE_KEY);
            ClientRegistry.registerKeyBinding(ConfigKey.OPEN_CONFIG_KEY);
        });

        // 注册 HUD
        event.enqueueWork(() -> {
            OverlayRegistry.registerOverlayTop("TAC Gun HUD Overlay", GunHudOverlay::render);
            OverlayRegistry.registerOverlayTop("TAC Kill Amount Overlay", KillAmountOverlay::render);
            OverlayRegistry.registerOverlayAbove(ForgeIngameGui.CROSSHAIR_ELEMENT, "TAC Interact Key Overlay", InteractKeyTextOverlay::render);
        });

        // 注册自己的的硬编码第三人称动画
        event.enqueueWork(ThirdPersonManager::registerDefault);

        // 注册颜色
        event.enqueueWork(() -> Minecraft.getInstance().getItemColors().register(AmmoBoxItem::getColor, ModItems.AMMO_BOX.get()));

        // 注册变种
        // noinspection deprecation
        event.enqueueWork(() -> ItemProperties.register(ModItems.AMMO_BOX.get(), AmmoBoxItem.PROPERTY_NAME, AmmoBoxItem::getStatue));

        // 注册文本提示
        event.enqueueWork(() -> MinecraftForgeClient.registerTooltipComponentFactory(GunTooltip.class, ClientGunTooltip::new));
        event.enqueueWork(() -> MinecraftForgeClient.registerTooltipComponentFactory(AmmoBoxTooltip.class, ClientAmmoBoxTooltip::new));
        event.enqueueWork(() -> MinecraftForgeClient.registerTooltipComponentFactory(AttachmentItemTooltip.class, ClientAttachmentItemTooltip::new));

        // 初始化自己的枪包下载器
        event.enqueueWork(ClientGunPackDownloadManager::init);
    }
}
