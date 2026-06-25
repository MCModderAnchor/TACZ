package com.tacz.guns.client.gui.overlay;

import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.block.AbstractGunSmithTableBlock;
import com.tacz.guns.block.AmmoStorageBlock;
import com.tacz.guns.block.GunDisplayCaseBlock;
import com.tacz.guns.block.GunDisplayCaseSlotHelper;
import com.tacz.guns.block.entity.GunDisplayCaseBlockEntity;
import com.tacz.guns.client.input.InteractKey;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.config.util.GunPegboardItemConfigRead;
import com.tacz.guns.config.util.InteractKeyConfigRead;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.apache.commons.lang3.StringUtils;

public class InteractKeyTextOverlay implements IGuiOverlay {

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        if (RenderConfig.DISABLE_INTERACT_HUD_TEXT.get()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || player.isSpectator()) {
            return;
        }
        HitResult hitResult = mc.hitResult;
        if (hitResult == null) {
            return;
        }
        if (hitResult instanceof BlockHitResult blockHitResult) {
            renderBlockText(graphics, width, height, blockHitResult, player, mc);
            return;
        }
        if (hitResult instanceof EntityHitResult entityHitResult) {
            renderEntityText(graphics, width, height, entityHitResult, mc);
        }
    }

    private static void renderBlockText(GuiGraphics graphics, int width, int height, BlockHitResult blockHitResult, LocalPlayer player, Minecraft mc) {
        BlockPos blockPos = blockHitResult.getBlockPos();
        BlockState block = player.level().getBlockState(blockPos);
        if (block.getBlock() instanceof GunDisplayCaseBlock) {
            renderDisplayCaseText(graphics, width, height, blockHitResult, player, mc, block);
            return;
        }
        if (block.getBlock() instanceof AmmoStorageBlock && IGun.mainHandHoldGun(player)) {
            renderText(graphics, width, height, mc.font, InteractKey.INTERACT_KEY.getTranslatedKeyMessage().getString(),
                    Component.translatable("gui.tacz.interact_key.text.ammo_storage_pickup"));
            return;
        }
        if (InteractKeyConfigRead.canInteractBlock(block)) {
            boolean mainHandHoldGun = IGun.mainHandHoldGun(player);
            boolean hasGunSmithTableFilterItem = hasGunSmithTableFilterItem(block, player);
            boolean willFilterByHand = RenderConfig.AUTO_SELECT_GUN_SMITH_TABLE_FILTER.get() && hasGunSmithTableFilterItem;
            if (mainHandHoldGun) {
                renderText(graphics, width, height, mc.font, InteractKey.INTERACT_KEY.getTranslatedKeyMessage().getString(), willFilterByHand ? Component.translatable("gui.tacz.interact_key.text.gun_smith_table_filter") : null);
            } else if (hasGunSmithTableFilterItem) {
                renderText(graphics, width, height, mc.font, mc.options.keyUse.getTranslatedKeyMessage().getString(), willFilterByHand ? Component.translatable("gui.tacz.interact_key.text.gun_smith_table_filter") : null);
            }
        }
    }

    private static void renderDisplayCaseText(GuiGraphics graphics, int width, int height, BlockHitResult blockHitResult, LocalPlayer player, Minecraft mc, BlockState block) {
        if (!IGun.mainHandHoldGun(player) || block.getValue(GunDisplayCaseBlock.LOCKED)) {
            return;
        }
        int slot = GunDisplayCaseSlotHelper.getSlot(block, blockHitResult.getBlockPos(), blockHitResult.getLocation());
        if (slot < 0 || !(player.level().getBlockEntity(blockHitResult.getBlockPos()) instanceof GunDisplayCaseBlockEntity displayCase)) {
            return;
        }
        boolean occupied = !displayCase.getItem(slot).isEmpty();
        boolean canPlaceHeld = GunPegboardItemConfigRead.canPlace(player.getMainHandItem());
        if (occupied || canPlaceHeld) {
            renderText(graphics, width, height, mc.font, InteractKey.INTERACT_KEY.getTranslatedKeyMessage().getString(), null);
        }
    }

    private static void renderEntityText(GuiGraphics graphics, int width, int height, EntityHitResult entityHitResult, Minecraft mc) {
        if (mc.player == null || !IGun.mainHandHoldGun(mc.player)) {
            return;
        }
        Entity entity = entityHitResult.getEntity();
        if (InteractKeyConfigRead.canInteractEntity(entity)) {
            renderText(graphics, width, height, mc.font, InteractKey.INTERACT_KEY.getTranslatedKeyMessage().getString(), null);
        }
    }

    private static boolean hasGunSmithTableFilterItem(BlockState block, LocalPlayer player) {
        if (!(block.getBlock() instanceof AbstractGunSmithTableBlock)) {
            return false;
        }
        ItemStack stack = player.getMainHandItem();
        Item item = stack.getItem();
        return item instanceof IGun || item instanceof IAttachment || item instanceof IAmmo;
    }

    private static void renderText(GuiGraphics graphics, int width, int height, Font font, String keyName, Component subtitle) {
        Component title = Component.translatable("gui.tacz.interact_key.text.desc", StringUtils.capitalize(keyName));
        graphics.drawString(font, title, (int) ((width - font.width(title)) / 2.0f), (int) (height / 2.0f - 25), ChatFormatting.YELLOW.getColor(), false);
        if (subtitle != null) {
            graphics.drawString(font, subtitle, (int) ((width - font.width(subtitle)) / 2.0f), (int) (height / 2.0f - 14), ChatFormatting.GRAY.getColor(), false);
        }
    }
}
