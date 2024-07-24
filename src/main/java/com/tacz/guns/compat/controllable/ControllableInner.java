package com.tacz.guns.compat.controllable;

import com.mrcrayfish.controllable.Controllable;
import com.mrcrayfish.controllable.client.binding.BindingRegistry;
import com.mrcrayfish.controllable.client.binding.ButtonBinding;
import com.mrcrayfish.controllable.client.binding.IBindingContext;
import com.mrcrayfish.controllable.client.input.Buttons;
import com.mrcrayfish.controllable.client.input.Controller;
import com.mrcrayfish.controllable.event.ControllerEvents;
import com.mrcrayfish.controllable.event.Value;
import com.mrcrayfish.framework.api.event.TickEvents;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.client.input.*;
import com.tacz.guns.client.resource.pojo.display.gun.ControllableData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.settings.KeyConflictContext;

import java.util.EnumMap;

public class ControllableInner {
    public static final IBindingContext GUN_KEY_CONFLICT = new GunKeyConflict();
    public static final ButtonBinding AIM = new ButtonBinding(Buttons.LEFT_TRIGGER, "key.tacz.aim.desc", "key.category.tacz", GUN_KEY_CONFLICT);
    public static final ButtonBinding SHOOT = new ButtonBinding(Buttons.RIGHT_TRIGGER, "key.tacz.shoot.desc", "key.category.tacz", GUN_KEY_CONFLICT);
    public static final ButtonBinding RELOAD = new ButtonBinding(Buttons.B, "key.tacz.reload.desc", "key.category.tacz", GUN_KEY_CONFLICT);
    public static final ButtonBinding MELEE = new ButtonBinding(Buttons.X, "key.tacz.melee.desc", "key.category.tacz", GUN_KEY_CONFLICT);
    public static final ButtonBinding ZOOM = new ButtonBinding(Buttons.X, "key.tacz.zoom.desc", "key.category.tacz", GUN_KEY_CONFLICT);
    public static final ButtonBinding CRAWL = new ButtonBinding(Buttons.LEFT_THUMB_STICK, "key.tacz.crawl.desc", "key.category.tacz", GUN_KEY_CONFLICT);
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

        ControllerEvents.INPUT.register(ControllableInner::onButtonInput);
        TickEvents.END_CLIENT.register(ControllableInner::onClientTickEnd);
    }

    public static boolean onButtonInput(Controller controller, Value<Integer> newButton, int originalButton, boolean isPress) {
        if (!GUN_KEY_CONFLICT.isActive()) {
            return false;
        }
        if (AIM.getButton() == newButton.get() && AimKey.onAimControllerPress(isPress)) {
            return true;
        }
        if (SHOOT.getButton() == newButton.get() && ShootKey.semiShootController(isPress)) {
            doRumble(controller);
            return true;
        }
        if (RELOAD.getButton() == newButton.get() && ReloadKey.onReloadControllerPress(isPress)) {
            return true;
        }
        if (MELEE.getButton() == newButton.get() && MeleeKey.onMeleeControllerPress(isPress)) {
            return true;
        }
        if (CRAWL.getButton() == newButton.get() && CrawlKey.onCrawlControllerPress(isPress)) {
            return true;
        }
        if (ZOOM.getButton() == newButton.get() && ZoomKey.onZoomControllerPress(isPress)) {
            return true;
        }
        if (FIRE_SELECT.getButton() == newButton.get() && FireSelectKey.onFireSelectControllerPress(isPress)) {
            return true;
        }
        if (INTERACT.getButton() == newButton.get() && InteractKey.onInteractControllerPress(isPress)) {
            return true;
        }
        return INSPECT.getButton() == newButton.get() && InspectKey.onInspectControllerPress(isPress);
    }

    public static void onClientTickEnd() {
        if (!GUN_KEY_CONFLICT.isActive()) {
            return;
        }
        Controller controller = Controllable.getController();
        if (controller == null) {
            return;
        }
        if (controller.isButtonPressed(SHOOT.getButton()) && ShootKey.autoShootController()) {
            doRumble(controller);
        }
    }

    private static void doRumble(Controller controller) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        ItemStack mainHandItem = player.getMainHandItem();
        IGun iGun = IGun.getIGunOrNull(mainHandItem);
        if (iGun == null) {
            return;
        }
        ResourceLocation gunId = iGun.getGunId(mainHandItem);
        FireMode fireMode = iGun.getFireMode(mainHandItem);
        TimelessAPI.getClientGunIndex(gunId).ifPresent(index -> {
            EnumMap<FireMode, ControllableData> data = index.getControllableData();
            if (data.containsKey(fireMode)) {
                ControllableData controllableData = data.get(fireMode);
                controller.rumble(controllableData.getLowFrequency(), controllableData.getHighFrequency(), controllableData.getTimeInMs());
            } else {
                if (fireMode == FireMode.AUTO) {
                    controller.rumble(0.15f, 0.25f, 80);
                } else {
                    controller.rumble(0.25f, 0.5f, 100);
                }
            }
        });
    }

    public static class GunKeyConflict implements IBindingContext {
        @Override
        public boolean isActive() {
            LocalPlayer player = Minecraft.getInstance().player;
            return !KeyConflictContext.GUI.isActive() && player != null && IGun.mainhandHoldGun(player);
        }

        @Override
        public boolean conflicts(IBindingContext other) {
            return this == other;
        }
    }
}
