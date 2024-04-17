package com.tac.guns.event.ammo;

import com.tac.guns.api.event.AmmoHitBlockEvent;
import com.tac.guns.entity.EntityBullet;
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
        EntityBullet ammo = event.getAmmo();
        if (state.getMaterial() == Material.GLASS) {
            // TODO 打碎玻璃（可以给个 Config 开关）
            level.destroyBlock(pos, false, ammo.getOwner());
        }
    }
}
