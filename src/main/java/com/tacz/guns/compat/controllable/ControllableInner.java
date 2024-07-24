package com.tacz.guns.compat.controllable;

import com.mrcrayfish.controllable.Controllable;
import com.mrcrayfish.controllable.client.*;
import com.mrcrayfish.controllable.event.ControllerEvent;
import com.mrcrayfish.controllable.event.GatherActionsEvent;
import com.mrcrayfish.framework.api.event.TickEvents;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.input.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Map;

public class ControllableInner {
    public static final IKeyConflictContext GUN_KEY_CONFLICT = new GunKeyConflict();
    public static final ButtonBinding AIM = new ButtonBinding(Buttons.LEFT_TRIGGER, "key.tacz.aim.desc", "key.category.tacz", GUN_KEY_CONFLICT);
    public static final ButtonBinding SHOOT = new ButtonBinding(Buttons.RIGHT_TRIGGER, "key.tacz.shoot.desc", "key.category.tacz", GUN_KEY_CONFLICT);
    public static final ButtonBinding RELOAD = new ButtonBinding(Buttons.B, "key.tacz.reload.desc", "key.category.tacz", GUN_KEY_CONFLICT);
    public static final ButtonBinding MELEE = new ButtonBinding(Buttons.X, "key.tacz.melee.desc", "key.category.tacz", GUN_KEY_CONFLICT);
    public static final ButtonBinding CRAWL = new ButtonBinding(Buttons.LEFT_THUMB_STICK, "key.tacz.crawl.desc", "key.category.tacz", GUN_KEY_CONFLICT);
    public static final ButtonBinding ZOOM = new ButtonBinding(Buttons.X, "key.tacz.zoom.desc", "key.category.tacz", GUN_KEY_CONFLICT);
    public static final ButtonBinding FIRE_SELECT = new ButtonBinding(Buttons.DPAD_LEFT, "key.tacz.fire_select.desc", "key.category.tacz", GUN_KEY_CONFLICT);
    public static final ButtonBinding INTERACT = new ButtonBinding(-1, "key.tacz.interact.desc", "key.category.tacz", GUN_KEY_CONFLICT);
    public static final ButtonBinding INSPECT = new ButtonBinding(-1, "key.tacz.inspect.desc", "key.category.tacz", GUN_KEY_CONFLICT);

    public static void init() {
        BindingRegistry.getInstance().register(AIM);
        BindingRegistry.getInstance().register(SHOOT);
        BindingRegistry.getInstance().register(RELOAD);
        BindingRegistry.getInstance().register(MELEE);
        BindingRegistry.getInstance().register(CRAWL);
        BindingRegistry.getInstance().register(ZOOM);
        BindingRegistry.getInstance().register(FIRE_SELECT);
        BindingRegistry.getInstance().register(INTERACT);
        BindingRegistry.getInstance().register(INSPECT);

        MinecraftForge.EVENT_BUS.register(new ControllableInner());
        TickEvents.END_CLIENT.register(ControllableInner::onClientTickEnd);
    }

    private static void onClientTickEnd() {
        if (!GUN_KEY_CONFLICT.isActive()) {
            return;
        }
        Controller controller = Controllable.getController();
        if (controller == null) {
            return;
        }
        if (controller.isButtonPressed(SHOOT.getButton())) {
            ShootKey.autoShootController();
        }
    }

    @SubscribeEvent
    public void onButtonInput(ControllerEvent.ButtonInput event) {
        boolean isPress = event.getState();
        int button = event.getButton();
        if (!GUN_KEY_CONFLICT.isActive()) {
            return;
        }
        if (AIM.getButton() == button && AimKey.onAimControllerPress(isPress)) {
            event.setCanceled(true);
        }
        if (SHOOT.getButton() == button && ShootKey.semiShootController(isPress)) {
            event.setCanceled(true);
        }
        if (RELOAD.getButton() == button && ReloadKey.onReloadControllerPress(isPress)) {
            event.setCanceled(true);
        }
        if (MELEE.getButton() == button && MeleeKey.onMeleeControllerPress(isPress)) {
            event.setCanceled(true);
        }
        if (CRAWL.getButton() == button && CrawlKey.onCrawlControllerPress(isPress)) {
            event.setCanceled(true);
        }
        if (ZOOM.getButton() == button && ZoomKey.onZoomControllerPress(isPress)) {
            event.setCanceled(true);
        }
        if (FIRE_SELECT.getButton() == button && FireSelectKey.onFireSelectControllerPress(isPress)) {
            event.setCanceled(true);
        }
        if (INTERACT.getButton() == button && InteractKey.onInteractControllerPress(isPress)) {
            event.setCanceled(true);
        }
        if (INSPECT.getButton() == button && InspectKey.onInspectControllerPress(isPress)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onAvailableActions(GatherActionsEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) {
            return;
        }
        Player player = mc.player;
        if (player == null) {
            return;
        }
        if (IGun.mainhandHoldGun(player)) {
            Map<ButtonBinding, Action> actions = event.getActions();

            // 移除自带的
            actions.remove(ButtonBindings.ATTACK);
            actions.remove(ButtonBindings.INVENTORY);

            // 左侧显示主要按键：瞄准、开火，换弹
            actions.put(AIM, new Action(Component.translatable("key.tacz.aim.desc"), Action.Side.LEFT));
            actions.put(SHOOT, new Action(Component.translatable("key.tacz.shoot.desc"), Action.Side.LEFT));
            actions.put(RELOAD, new Action(Component.translatable("key.tacz.reload.desc"), Action.Side.LEFT));

            // 右侧显示次要按键：近战，切换开火模式
            actions.put(MELEE, new Action(Component.translatable("key.tacz.melee.desc"), Action.Side.RIGHT));
            actions.put(FIRE_SELECT, new Action(Component.translatable("key.tacz.fire_select.desc"), Action.Side.RIGHT));
        }
    }

    public static class GunKeyConflict implements IKeyConflictContext {
        @Override
        public boolean isActive() {
            LocalPlayer player = Minecraft.getInstance().player;
            return !KeyConflictContext.GUI.isActive() && player != null && IGun.mainhandHoldGun(player);
        }

        @Override
        public boolean conflicts(IKeyConflictContext other) {
            return this == other;
        }
    }
}
