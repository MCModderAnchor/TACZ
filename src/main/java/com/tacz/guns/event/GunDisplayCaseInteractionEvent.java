package com.tacz.guns.event;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.block.GunDisplayCaseBlock;
import com.tacz.guns.block.GunDisplayCaseSlotHelper;
import com.tacz.guns.block.entity.GunDisplayCaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class GunDisplayCaseInteractionEvent {
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Player player = event.getEntity();
        if (!player.isShiftKeyDown() || player.getMainHandItem().getItem() instanceof IGun) {
            return;
        }
        if (!(player.getOffhandItem().getItem() instanceof IGun)) {
            return;
        }
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof GunDisplayCaseBlock displayCaseBlock)) {
            return;
        }
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
        event.setCanceled(true);
        displayCaseBlock.toggleLockFromInteraction(level, pos, state);
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) {
            return;
        }
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof GunDisplayCaseBlock) || state.getValue(GunDisplayCaseBlock.LOCKED)) {
            return;
        }
        HitResult picked = event.getEntity().pick(6.0D, 0.0F, false);
        if (!(picked instanceof BlockHitResult hit) || !hit.getBlockPos().equals(pos)) {
            return;
        }
        int slot = GunDisplayCaseSlotHelper.getSlot(state, pos, hit.getLocation());
        if (slot < 0) {
            return;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof GunDisplayCaseBlockEntity displayCase) || displayCase.getItem(slot).isEmpty()) {
            return;
        }
        event.setCanceled(true);
        Player player = event.getEntity();
        boolean shouldDrop = !player.isCreative() || player.isShiftKeyDown();
        if (!level.isClientSide() && (shouldDrop ? displayCase.dropItem(slot) : displayCase.removeItem(slot))) {
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }
}
