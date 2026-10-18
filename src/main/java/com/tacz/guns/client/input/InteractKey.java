package com.tacz.guns.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.block.AmmoStorageBlock;
import com.tacz.guns.block.GunDisplayCaseBlock;
import com.tacz.guns.client.event.GunDisplayCaseClientInteractionEvent;
import com.tacz.guns.config.util.InteractKeyConfigRead;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ClientMessageAmmoStorageInteract;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

import static com.tacz.guns.util.InputExtraCheck.isInGame;

@OnlyIn(Dist.CLIENT)
public class InteractKey {
    public static final KeyMapping INTERACT_KEY = new KeyMapping("key.tacz.interact.desc",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            "key.category.tacz");

    public static boolean onInteractControllerPress(boolean isPress) {
        return isPress && onInteractPress();
    }

    public static boolean onInteractPress() {
        if (!isInGame()) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || player.isSpectator()) {
            return false;
        }
        if (!IGun.mainHandHoldGun(player)) {
            return false;
        }
        HitResult hitResult = mc.hitResult;
        if (hitResult == null) {
            return false;
        }
        if (hitResult instanceof BlockHitResult blockHitResult) {
            interactBlock(blockHitResult, player, mc);
            return true;
        }
        if (hitResult instanceof EntityHitResult entityHitResult) {
            interactEntity(entityHitResult, mc);
            return true;
        }
        return false;
    }

    private static void interactBlock(BlockHitResult blockHitResult, LocalPlayer player, Minecraft mc) {
        BlockPos blockPos = blockHitResult.getBlockPos();
        BlockState block = player.level().getBlockState(blockPos);
        if (block.getBlock() instanceof GunDisplayCaseBlock) {
            GunDisplayCaseClientInteractionEvent.showHintForTarget(mc, true);
        }
        if (block.getBlock() instanceof AmmoStorageBlock) {
            NetworkHandler.CHANNEL.sendToServer(new ClientMessageAmmoStorageInteract(blockPos));
            return;
        }
        if (InteractKeyConfigRead.canInteractBlock(block)) {
            mc.startUseItem();
        }
    }

    private static void interactEntity(EntityHitResult entityHitResult, Minecraft mc) {
        Entity entity = entityHitResult.getEntity();
        if (InteractKeyConfigRead.canInteractEntity(entity)) {
            mc.startUseItem();
        }
    }
}
