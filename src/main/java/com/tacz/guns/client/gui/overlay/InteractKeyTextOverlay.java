package com.tacz.guns.client.gui.overlay;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.input.InteractKey;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.config.util.InteractKeyConfigRead;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.gui.ForgeIngameGui;
import org.apache.commons.lang3.StringUtils;

public class InteractKeyTextOverlay {
    public static void render(ForgeIngameGui gui, PoseStack poseStack, float partialTick, int width, int height) {
        if (RenderConfig.DISABLE_INTERACT_HUD_TEXT.get()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || player.isSpectator()) {
            return;
        }
        if (!IGun.mainhandHoldGun(player)) {
            return;
        }
        HitResult hitResult = mc.hitResult;
        if (hitResult == null) {
            return;
        }
        if (hitResult instanceof BlockHitResult blockHitResult) {
            renderBlockText(poseStack, width, height, blockHitResult, player, mc);
            return;
        }
        if (hitResult instanceof EntityHitResult entityHitResult) {
            renderEntityText(poseStack, width, height, entityHitResult, mc);
        }
    }

    private static void renderBlockText(PoseStack poseStack, int width, int height, BlockHitResult blockHitResult, LocalPlayer player, Minecraft mc) {
        BlockPos blockPos = blockHitResult.getBlockPos();
        Block block = player.level().getBlockState(blockPos).getBlock();
        if (InteractKeyConfigRead.canInteractBlock(block)) {
            renderText(poseStack, width, height, mc.font);
        }
    }

    private static void renderEntityText(PoseStack poseStack, int width, int height, EntityHitResult entityHitResult, Minecraft mc) {
        Entity entity = entityHitResult.getEntity();
        if (InteractKeyConfigRead.canInteractEntity(entity)) {
            renderText(poseStack, width, height, mc.font);
        }
    }

    private static void renderText(PoseStack poseStack, int width, int height, Font font) {
        String keyName = InteractKey.INTERACT_KEY.getTranslatedKeyMessage().getString();
        Component title = Component.translatable("gui.tacz.interact_key.text.desc", StringUtils.capitalize(keyName));
        font.drawShadow(poseStack, title, (width - font.width(title)) / 2.0f, height / 2.0f - 25, ChatFormatting.YELLOW.getColor());
    }
}