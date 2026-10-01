package com.tacz.guns.compat.controllable;

import com.mrcrayfish.controllable.Controllable;
import com.mrcrayfish.controllable.client.binding.ButtonBinding;
import com.mrcrayfish.controllable.client.binding.context.BindingContext;
import com.mrcrayfish.controllable.client.binding.context.InGameContext;
import com.mrcrayfish.controllable.client.binding.handlers.OnPressAndReleaseHandler;
import com.mrcrayfish.controllable.client.input.Buttons;
import com.mrcrayfish.controllable.client.input.Controller;
import com.mrcrayfish.framework.api.event.TickEvents;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.client.input.*;
import com.tacz.guns.client.resource.pojo.display.gun.ControllableData;
import com.tacz.guns.config.sync.SyncConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.Optional;

public class ControllableInner {
    public static final IBindingContext GUN_KEY_CONFLICT = new GunKeyConflict();
    public static final IBindingContext CRAWL_KEY_CONFLICT = new GunKeyConflict() {
        @Override
        public boolean isActive() {
            LocalPlayer player = Minecraft.getInstance().player;
            boolean inputEnabled = !KeyConflictContext.GUI.isActive() && player != null;
            if (!inputEnabled || !SyncConfig.ENABLE_CRAWL.get()) {
                return false;
            }
            boolean itemAllowsCrawl = IGun.mainHandHoldGun(player) || SyncConfig.ENABLE_CRAWL_WITHOUT_GUN.get();
            return itemAllowsCrawl;
        }
    };
    public static final ButtonBinding AIM = new ButtonBinding(Buttons.LEFT_TRIGGER, "key.tacz.aim.desc", "key.category.tacz", GUN_KEY_CONFLICT);
    public static final ButtonBinding SHOOT = new ButtonBinding(Buttons.RIGHT_TRIGGER, "key.tacz.shoot.desc", "key.category.tacz", GUN_KEY_CONFLICT);
    public static final ButtonBinding RELOAD = new ButtonBinding(Buttons.B, "key.tacz.reload.desc", "key.category.tacz", GUN_KEY_CONFLICT);
    public static final ButtonBinding MELEE = new ButtonBinding(Buttons.X, "key.tacz.melee.desc", "key.category.tacz", GUN_KEY_CONFLICT);
    public static final ButtonBinding ZOOM = new ButtonBinding(Buttons.X, "key.tacz.zoom.desc", "key.category.tacz", GUN_KEY_CONFLICT);
    public static final ButtonBinding CRAWL = new ButtonBinding(Buttons.LEFT_THUMB_STICK, "key.tacz.crawl.desc", "key.category.tacz", CRAWL_KEY_CONFLICT);
    public static final ButtonBinding FIRE_SELECT = new ButtonBinding(Buttons.DPAD_LEFT, "key.tacz.fire_select.desc", "key.category.tacz", GUN_KEY_CONFLICT);
    public static final ButtonBinding INTERACT = new ButtonBinding(-1, "key.tacz.interact.desc", "key.category.tacz", GUN_KEY_CONFLICT);
    public static final ButtonBinding INSPECT = new ButtonBinding(-1, "key.tacz.inspect.desc", "key.category.tacz", GUN_KEY_CONFLICT);

    public static void init() {
        Controllable.getBindingRegistry().register(AIM);
        Controllable.getBindingRegistry().register(SHOOT);
        Controllable.getBindingRegistry().register(RELOAD);
        Controllable.getBindingRegistry().register(MELEE);
        Controllable.getBindingRegistry().register(CRAWL);
        Controllable.getBindingRegistry().register(ZOOM);
        Controllable.getBindingRegistry().register(FIRE_SELECT);
        Controllable.getBindingRegistry().register(INTERACT);
        Controllable.getBindingRegistry().register(INSPECT);

        TickEvents.END_CLIENT.register(ControllableInner::onClientTickEnd);
    }

    public static boolean onButtonInput(Controller controller, Value<Integer> newButton, int originalButton, boolean isPress) {
        if (!GUN_KEY_CONFLICT.isActive()) {
            boolean crawlInput = CRAWL_KEY_CONFLICT.isActive() && CRAWL.getButton() == newButton.get();
            return crawlInput && CrawlKey.onCrawlControllerPress(isPress);
        }
        if (AIM.getButton() == newButton.get() && AimKey.onAimControllerPress(isPress)) {
            return true;
        }
        if (SHOOT.getButton() == newButton.get()) {
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
        Controller controller = Controllable.getController();
        boolean active = controller != null && GUN_KEY_CONFLICT.isActive();
        AimKey.setControllerAimDown(active && controller.isButtonPressed(AIM.getButton()));
        boolean crawlActive = controller != null && CRAWL_KEY_CONFLICT.isActive();
        CrawlKey.setControllerCrawlDown(crawlActive && controller.isButtonPressed(CRAWL.getButton()));
        ShootKey.shootControllerTick(active && controller.isButtonPressed(SHOOT.getButton()));
    }

    public static void rumbleShoot(ItemStack mainHandItem, FireMode fireMode) {
        Controller controller = Controllable.getController();
        if (controller == null) {
            return;
        }
        IGun iGun = IGun.getIGunOrNull(mainHandItem);
        if (iGun == null) {
            return;
        }

        TimelessAPI.getGunDisplay(mainHandItem).ifPresent(index -> {
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

    public static class GunKeyConflict extends InGameContext {
        protected GunKeyConflict(ResourceLocation id) {
            super(id);
        }

        @Override
        public boolean conflicts(IBindingContext other) {
            return other instanceof GunKeyConflict;
        }
    }
}
