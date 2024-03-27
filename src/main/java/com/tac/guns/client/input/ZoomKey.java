package com.tac.guns.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.tac.guns.api.TimelessAPI;
import com.tac.guns.api.attachment.AttachmentType;
import com.tac.guns.api.item.IAttachment;
import com.tac.guns.api.item.IGun;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import static com.tac.guns.util.InputExtraCheck.isInGame;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ZoomKey {
    public static final KeyMapping ZOOM_KEY = new KeyMapping("key.tac.zoom.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.category.tac");

    @SubscribeEvent
    public static void onReloadPress(InputEvent.KeyInputEvent event) {
        if (isInGame() && event.getAction() == GLFW.GLFW_PRESS && ZOOM_KEY.matches(event.getKey(), event.getScanCode())) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }
            ItemStack gunItem = player.getMainHandItem();
            IGun iGun = IGun.getIGunOrNull(gunItem);
            if (iGun != null) {
                ItemStack scopeItem = iGun.getAttachment(gunItem, AttachmentType.SCOPE);
                IAttachment iAttachment = IAttachment.getIAttachmentOrNull(scopeItem);
                if (iAttachment != null) {
                    TimelessAPI.getClientAttachmentIndex(iAttachment.getAttachmentId(scopeItem)).ifPresent(index -> {
                        if (index.getZoom() != null && index.getZoom().length != 0) {
                            int zoomNumber = iAttachment.getZoomNumber(scopeItem);
                            ++zoomNumber;
                            zoomNumber = zoomNumber % index.getZoom().length;
                            iAttachment.setZoomNumber(scopeItem, zoomNumber);
                        }
                    });
                }
            }
        }
    }
}
