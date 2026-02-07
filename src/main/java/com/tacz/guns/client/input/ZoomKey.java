package com.tacz.guns.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.nbt.AttachmentItemDataAccessor;
import com.tacz.guns.client.gui.overlay.AimlessZoomOverlay;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import com.tacz.guns.client.sound.SoundPlayManager;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ClientMessagePlayerZoom;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import static com.tacz.guns.util.InputExtraCheck.isInGame;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ZoomKey {
    public static final KeyMapping ZOOM_KEY = new KeyMapping("key.tacz.zoom.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            "key.category.tacz");

    @SubscribeEvent
    public static void onZoomKeyPress(InputEvent.Key event) {
        if (isInGame() && event.getAction() == GLFW.GLFW_PRESS && ZOOM_KEY.matches(event.getKey(), event.getScanCode())) {
            doZoomLogic();
        }
    }

    @SubscribeEvent
    public static void onZoomMousePress(InputEvent.MouseButton.Post event) {
        if (isInGame() && event.getAction() == GLFW.GLFW_PRESS && ZOOM_KEY.matchesMouse(event.getButton())) {
            doZoomLogic();
        }
    }

    public static boolean onZoomControllerPress(boolean isPress) {
        if (isInGame() && isPress) {
            return doZoomLogic();
        }
        return false;
    }

    private static boolean doZoomLogic() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.isSpectator()) {
            return false;
        }
        IClientPlayerGunOperator operator = IClientPlayerGunOperator.fromLocalPlayer(player);
        if (operator.isAim()) {
            NetworkHandler.CHANNEL.sendToServer(new ClientMessagePlayerZoom());
            return true;
        } else if (RenderConfig.AIMLESS_ZOOM_ENABLE.get()) {
            NetworkHandler.CHANNEL.sendToServer(new ClientMessagePlayerZoom());
            return maybeDisplayZoom(player, operator);
        }
        return false;
    }

    private static boolean maybeDisplayZoom(LocalPlayer player, IClientPlayerGunOperator operator) {
        if (!operator.isAim()) {
            ItemStack mainHandItem = player.getMainHandItem();
            IGun iGun = IGun.getIGunOrNull(mainHandItem);
            if (iGun == null) {
                return true;
            }

            ResourceLocation scopeId = iGun.getAttachmentId(mainHandItem, AttachmentType.SCOPE);
            CompoundTag scopeTag = iGun.getAttachmentTag(mainHandItem, AttachmentType.SCOPE);
            if (!DefaultAssets.isEmptyAttachmentId(scopeId) && scopeTag != null) {
                ClientAttachmentIndex index = TimelessAPI.getClientAttachmentIndex(scopeId).orElse(null);

                if (index != null && index.getZoom() != null && index.getZoom().length != 1) {
                    int zoomNumber = AttachmentItemDataAccessor.getZoomNumberFromTag(scopeTag);
                    ++zoomNumber;
                    zoomNumber = zoomNumber % (Integer.MAX_VALUE - 1);
                    AttachmentItemDataAccessor.setZoomNumberToTag(scopeTag, zoomNumber);

                    TimelessAPI.getGunDisplay(mainHandItem).ifPresent(gunIndex -> {
                        SoundPlayManager.playFireSelectSound(player, gunIndex);
                    });
                    float zoom = iGun.getAimingZoom(player.getMainHandItem());
                    AimlessZoomOverlay.renderZoomChange(zoom);

                } // no index of scope?(why?) / only one zoom -> ignored
            } // no scope / no scope tag -> ignored
        }
        return true;
    }
}
