package com.tacz.guns.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.tacz.guns.GunMod;
import com.tacz.guns.client.gui.GunRefitScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.List;

import static com.tacz.guns.util.InputExtraCheck.isInGame;

@net.neoforged.fml.common.EventBusSubscriber(value = Dist.CLIENT, modid = GunMod.MOD_ID)
public final class GunInputHandler {
    private static final List<PressBinding> PRESS_BINDINGS = List.of(
            new PressBinding(UnloadKey.UNLOAD_KEY, UnloadKey::onUnloadPress),
            new PressBinding(AimKey.AIM_KEY, AimKey::onAimPress),
            new PressBinding(CrawlKey.CRAWL_KEY, CrawlKey::onCrawlPress),
            new PressBinding(ReloadKey.RELOAD_KEY, ReloadKey::onReloadPress),
            new PressBinding(InspectKey.INSPECT_KEY, InspectKey::onInspectPress),
            new PressBinding(FireSelectKey.FIRE_SELECT_KEY, FireSelectKey::onFireSelectPress),
            new PressBinding(MeleeKey.MELEE_KEY, MeleeKey::onMeleePress),
            new PressBinding(ZoomKey.ZOOM_KEY, ZoomKey::onZoomPress),
            new PressBinding(InteractKey.INTERACT_KEY, InteractKey::onInteractPress),
            new PressBinding(RefitKey.REFIT_KEY, RefitKey::onRefitPress),
            new PressBinding(ConfigKey.OPEN_CONFIG_KEY, ConfigKey::onOpenConfig)
    );

    private GunInputHandler() {
    }

    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        for (PressBinding binding : PRESS_BINDINGS) {
            event.register(binding.keyMapping());
        }
        event.register(ShootKey.SHOOT_KEY);
    }

    @SubscribeEvent
    public static void onKeyPress(InputEvent.Key event) {
        InputConstants.Key key = InputConstants.getKey(event.getKey(), event.getScanCode());
        if (event.getAction() == GLFW.GLFW_RELEASE) {
            onRelease(key);
        }
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }
        if (!closeRefitScreen(key)) {
            onPress(key);
        }
    }

    @SubscribeEvent
    public static void onMousePress(InputEvent.MouseButton.Post event) {
        InputConstants.Key key = InputConstants.Type.MOUSE.getOrCreate(event.getButton());
        if (event.getAction() == GLFW.GLFW_RELEASE) {
            onRelease(key);
        }
        if (event.getAction() == GLFW.GLFW_PRESS) {
            onPress(key);
        }
    }

    @SubscribeEvent
    public static void onMousePressPre(InputEvent.MouseButton.Pre event) {
        if (event.getAction() == GLFW.GLFW_PRESS
                && closeRefitScreen(InputConstants.Type.MOUSE.getOrCreate(event.getButton()))) {
            // 消费关闭界面的点击，避免同一次输入在 Post 阶段重新打开界面。
            event.setCanceled(true);
        }
    }

    private static void onPress(InputConstants.Key key) {
        for (PressBinding binding : PRESS_BINDINGS) {
            // 某个动作可能打开界面，后续动作不能继续穿透到游戏中。
            if (!isInGame()) {
                return;
            }
            if (binding.keyMapping().isActiveAndMatches(key)) {
                binding.action().run();
                // 即使卸弹条件不满足，也不能让 Alt+R 穿透成普通换弹。
                if (binding.keyMapping() == UnloadKey.UNLOAD_KEY) {
                    return;
                }
            }
        }
    }

    private static boolean closeRefitScreen(InputConstants.Key key) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getOverlay() != null || !mc.isWindowActive()
                || !(mc.screen instanceof GunRefitScreen refitScreen)) {
            return false;
        }
        KeyMapping refitKey = RefitKey.REFIT_KEY;
        if (key != InputConstants.UNKNOWN && key.equals(refitKey.getKey())
                && refitKey.getKeyModifier().isActive(refitKey.getKeyConflictContext())) {
            refitScreen.onClose();
            return true;
        }
        return false;
    }

    private static void onRelease(InputConstants.Key key) {
        for (PressBinding binding : PRESS_BINDINGS) {
            if (key.equals(binding.keyMapping().getKey())) {
                binding.keyMapping().setDown(false);
            }
        }
        if (key.equals(ShootKey.SHOOT_KEY.getKey())) {
            ShootKey.SHOOT_KEY.setDown(false);
        }
    }

    private record PressBinding(KeyMapping keyMapping, Runnable action) {
    }
}
