package com.tacz.guns.client.gui.overlay;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.input.InteractKey;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.config.common.OtherConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.gui.ForgeIngameGui;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

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
        Block block = player.level.getBlockState(blockPos).getBlock();
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block);
        if (blockId == null) {
            return;
        }
        var whiteList = OtherConfig.INTERACT_KEY_WHITELIST_BLOCKS.get();
        getAndRenderText(poseStack, width, height, mc, blockId, whiteList);
    }

    private static void renderEntityText(PoseStack poseStack, int width, int height, EntityHitResult entityHitResult, Minecraft mc) {
        Entity entity = entityHitResult.getEntity();
        ResourceLocation entityId = ForgeRegistries.ENTITIES.getKey(entity.getType());
        if (entityId == null) {
            return;
        }
        var whiteList = OtherConfig.INTERACT_KEY_WHITELIST_ENTITIES.get();
        getAndRenderText(poseStack, width, height, mc, entityId, whiteList);
    }

    private static void getAndRenderText(PoseStack poseStack, int width, int height, Minecraft mc, ResourceLocation id, List<List<String>> whiteList) {
        var first = whiteList.stream().filter(list -> list.get(0).equals(id.toString())).findFirst();
        first.ifPresent(list -> {
            String keyName = InteractKey.INTERACT_KEY.getTranslatedKeyMessage().getString();
            TranslatableComponent title = new TranslatableComponent("gui.tacz.interact_key.text.desc", StringUtils.capitalize(keyName));
            TranslatableComponent text = new TranslatableComponent(list.get(1));
            mc.font.drawShadow(poseStack, title, (width - mc.font.width(title)) / 2.0f, height / 2.0f - 30, ChatFormatting.YELLOW.getColor());
            mc.font.drawShadow(poseStack, text, (width - mc.font.width(text)) / 2.0f, height / 2.0f - 18, ChatFormatting.GRAY.getColor());
        });
    }
}