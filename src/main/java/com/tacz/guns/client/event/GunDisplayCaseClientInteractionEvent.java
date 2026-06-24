package com.tacz.guns.client.event;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.block.GunDisplayCaseBlock;
import com.tacz.guns.client.gui.overlay.DisplayCaseLockHintOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class GunDisplayCaseClientInteractionEvent {
    @SubscribeEvent
    public static void onUseKey(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || player.isSpectator() || event.getAction() != GLFW.GLFW_PRESS || !minecraft.options.keyUse.matches(event.getKey(), event.getScanCode())) {
            return;
        }
        if (!player.getMainHandItem().isEmpty() || player.isShiftKeyDown()) {
            return;
        }
        showHintForTarget(minecraft, false);
    }

    @SubscribeEvent
    public static void onUseMouse(InputEvent.MouseButton.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || player.isSpectator() || event.getAction() != GLFW.GLFW_PRESS || !minecraft.options.keyUse.matchesMouse(event.getButton())) {
            return;
        }
        if (!player.getMainHandItem().isEmpty() || player.isShiftKeyDown()) {
            return;
        }
        showHintForTarget(minecraft, false);
    }

    public static void showHintForTarget(Minecraft minecraft, boolean lockedOnly) {
        if (!(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK || minecraft.level == null) {
            return;
        }
        BlockState state = minecraft.level.getBlockState(hit.getBlockPos());
        if (!(state.getBlock() instanceof GunDisplayCaseBlock)) {
            return;
        }
        boolean locked = state.getValue(GunDisplayCaseBlock.LOCKED);
        if (lockedOnly && !locked) {
            return;
        }
        DisplayCaseLockHintOverlay.show(createHint(locked, minecraft));
    }

    private static Component createHint(boolean locked, Minecraft minecraft) {
        String shiftKey = minecraft.options.keyShift.getTranslatedKeyMessage().getString();
        String useKey = minecraft.options.keyUse.getTranslatedKeyMessage().getString();
        String chord = shiftKey + " + " + useKey;
        return Component.translatable(locked ? "gui.tacz.display_case.locked_hint" : "gui.tacz.display_case.unlocked_hint", chord);
    }

    public static boolean holdingGun(LocalPlayer player) {
        return player.getMainHandItem().getItem() instanceof IGun;
    }
}
