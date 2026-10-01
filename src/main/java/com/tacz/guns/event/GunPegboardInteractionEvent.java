package com.tacz.guns.event;

import com.tacz.guns.block.GunPegboardBlock;
import com.tacz.guns.block.GunPegboardSlotHelper;
import com.tacz.guns.block.entity.GunPegboardBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

@net.neoforged.fml.common.EventBusSubscriber
public class GunPegboardInteractionEvent {
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity().getMainHandItem().is(Items.DEBUG_STICK)) return;
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) {
            return;
        }
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof GunPegboardBlock)) {
            return;
        }
        HitResult picked = event.getEntity().pick(6.0D, 0.0F, false);
        if (!(picked instanceof BlockHitResult hit) || !hit.getBlockPos().equals(pos)) {
            return;
        }
        int slot = GunPegboardSlotHelper.getSlot(state, pos, hit.getLocation());
        if (slot < 0) {
            return;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof GunPegboardBlockEntity pegboard) || pegboard.getItem(slot).isEmpty()) {
            return;
        }
        event.setCanceled(true);
        Player player = event.getEntity();
        boolean shouldDrop = !player.isCreative() || player.isShiftKeyDown();
        if (!level.isClientSide() && (shouldDrop ? pegboard.dropItem(slot) : pegboard.removeItem(slot))) {
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }
}
