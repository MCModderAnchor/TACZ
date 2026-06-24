package com.tacz.guns.event;

import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.block.AmmoStorageBlock;
import com.tacz.guns.block.entity.AmmoStorageBlockEntity;
import com.tacz.guns.util.AmmoStorageInteractionHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class AmmoStorageInteractionEvent {
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (!player.isShiftKeyDown() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        ItemStack held = player.getItemInHand(event.getHand());
        if (!(held.getItem() instanceof IAmmoBox ammoBox)) {
            return;
        }
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof AmmoStorageBlock)) {
            return;
        }
        if (level.isClientSide()) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof AmmoStorageBlockEntity storage)) {
            return;
        }
        InteractionResult result = AmmoStorageInteractionHelper.unloadAmmoBox(storage, held, ammoBox, player);
        event.setCancellationResult(result);
        event.setCanceled(true);
    }
}
