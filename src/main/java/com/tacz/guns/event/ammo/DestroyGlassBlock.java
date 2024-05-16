package com.tacz.guns.event.ammo;

import com.tacz.guns.api.event.server.AmmoHitBlockEvent;
import com.tacz.guns.config.common.AmmoConfig;
import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class DestroyGlassBlock {
    @SubscribeEvent
    public static void onAmmoHitBlock(AmmoHitBlockEvent event) {
        Level level = event.getLevel();
        BlockState state = event.getState();
        BlockPos pos = event.getHitResult().getBlockPos();
        EntityKineticBullet ammo = event.getAmmo();
        if (AmmoConfig.DESTROY_GLASS.get() && state.getMaterial() == Material.GLASS) {
            level.destroyBlock(pos, false, ammo.getOwner());
        }
    }
}
