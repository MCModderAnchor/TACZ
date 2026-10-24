package com.tacz.guns.client.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.client.other.ThirdPersonManager;
import com.tacz.guns.client.gui.overlay.*;
import com.tacz.guns.client.input.GunInputHandler;
import com.tacz.guns.client.gui.overlay.GunHudOverlay;
import com.tacz.guns.client.gui.overlay.HeatBarOverlay;
import com.tacz.guns.client.gui.overlay.InteractKeyTextOverlay;
import com.tacz.guns.client.gui.overlay.KillAmountOverlay;
import com.tacz.guns.client.input.*;
import com.tacz.guns.client.renderer.item.AmmoItemRenderer;
import com.tacz.guns.client.renderer.item.AttachmentItemRenderer;
import com.tacz.guns.client.renderer.item.GunSmithTableItemRenderer;
import com.tacz.guns.client.renderer.scope.ScopeLensRenderer;
import com.tacz.guns.client.resource.ClientAssetsManager;
import com.tacz.guns.client.sound.SoundPlayManager;
import com.tacz.guns.client.tooltip.*;
import com.tacz.guns.compat.ar.ARCompat;
import com.tacz.guns.compat.controllable.ControllableCompat;
import com.tacz.guns.compat.playeranimator.PlayerAnimatorCompat;
import com.tacz.guns.compat.shouldersurfing.ShoulderSurfingCompat;
import com.tacz.guns.init.ModBlocks;
import com.tacz.guns.init.ModItems;
import com.tacz.guns.inventory.tooltip.*;
import com.tacz.guns.item.AmmoBoxItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

import static net.neoforged.neoforge.client.gui.VanillaGuiLayers.CROSSHAIR;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT, modid = GunMod.MOD_ID)
public class ClientSetupEvent {
    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) {
        ScopeLensRenderer.registerShader(event);
    }

    @SubscribeEvent
    public static void onClientSetup(RegisterKeyMappingsEvent event) {
        // 注册键位
        GunInputHandler.registerKeyMappings(event);
    }

    @SubscribeEvent
    public static void onClientSetup(RegisterClientTooltipComponentFactoriesEvent event) {
        // 注册文本提示
        event.register(GunTooltip.class, ClientGunTooltip::new);
        event.register(AmmoBoxTooltip.class, ClientAmmoBoxTooltip::new);
        event.register(AmmoStorageTooltip.class, ClientAmmoStorageTooltip::new);
        event.register(AttachmentItemTooltip.class, ClientAttachmentItemTooltip::new);
        event.register(BlockItemTooltip.class, ClientBlockItemTooltip::new);
    }

    @SubscribeEvent
    public static void onRegisterGuiOverlays(RegisterGuiLayersEvent event) {
        // 注册 HUD
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "tac_gun_hud_overlay"), new GunHudOverlay());
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "tac_heat_bar"), new HeatBarOverlay());
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "tac_kill_amount_overlay"), new KillAmountOverlay());
        event.registerAbove(CROSSHAIR, ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "tac_interact_key_overlay"), new InteractKeyTextOverlay());
        event.registerAbove(CROSSHAIR, ResourceLocation.fromNamespaceAndPath(GunMod.MOD_ID, "tac_display_case_lock_hint"), new DisplayCaseLockHintOverlay());

    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // 注册自己的的硬编码第三人称动画
        event.enqueueWork(ThirdPersonManager::registerDefault);

        // 注册颜色
        event.enqueueWork(() -> Minecraft.getInstance().getItemColors().register(AmmoBoxItem::getColor, ModItems.AMMO_BOX.get()));

        // 注册变种
        // noinspection deprecation
        event.enqueueWork(() -> ItemProperties.register(ModItems.AMMO_BOX.get(), AmmoBoxItem.PROPERTY_NAME, AmmoBoxItem::getStatue));

        event.enqueueWork(() -> ItemBlockRenderTypes.setRenderLayer(ModBlocks.GUN_DISPLAY_CASE.get(), RenderType.cutout()));

        // 初始化自己的枪包下载器
//        event.enqueueWork(ClientGunPackDownloadManager::init);

//        // 与 player animator 的兼容
//        event.enqueueWork(PlayerAnimatorCompat::init);

        // 与 Shoulder Surfing Reloaded 的兼容
        event.enqueueWork(ShoulderSurfingCompat::init);

        // 与 Controllable 的兼容
        event.enqueueWork(ControllableCompat::init);

        // 与 Accelerated Rendering 的兼容
		event.enqueueWork(ARCompat::init);

        Minecraft minecraft = Minecraft.getInstance();
        AmmoItemRenderer.INSTANCE = new AmmoItemRenderer(minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
        AttachmentItemRenderer.INSTANCE = new AttachmentItemRenderer(minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
        GunSmithTableItemRenderer.INSTANCE = new GunSmithTableItemRenderer(minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels());
    }

    @SubscribeEvent
    public static void onClientResourceReload(RegisterClientReloadListenersEvent event) {
        PlayerAnimatorCompat.init();
        event.registerReloadListener((barrier, resourceManager, preparationProfiler, reloadProfiler, backgroundExecutor, gameExecutor) ->
                barrier.wait(Void.TYPE).thenRunAsync(SoundPlayManager::clearSoundResourceCache, gameExecutor));
        ClientAssetsManager.INSTANCE.reloadAndRegister(event::registerReloadListener);
        if (PlayerAnimatorCompat.isInstalled()) {
            PlayerAnimatorCompat.registerReloadListener(event::registerReloadListener);
        }
    }
}
