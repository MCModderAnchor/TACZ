package com.tacz.guns.event.ammo;

import com.tacz.guns.api.event.server.AmmoHitBlockEvent;
import com.tacz.guns.config.common.AmmoConfig;
import com.tacz.guns.config.util.AmmoBlockConfigRead;
import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber
public class DestroyGlassBlock {
    @SubscribeEvent
    public static void onAmmoHitBlock(AmmoHitBlockEvent event) {
        if (!AmmoConfig.DESTROY_GLASS.get()) {
            return;
        }
        Level level = event.getLevel();
        BlockState state = event.getState();
        BlockPos pos = event.getHitResult().getBlockPos();
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0 || hardness > AmmoConfig.DESTROY_BLOCK_HARDNESS_THRESHOLD.get()) {
            return;
        }
        EntityKineticBullet ammo = event.getAmmo();
        if (AmmoBlockConfigRead.canDestroy(state)) {
            level.destroyBlock(pos, false, ammo.getOwner());
        }
    }
}
