package com.tacz.guns.mixin.common;

import com.tacz.guns.compat.sable.BulletRayTraceContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClipContext.class)
public abstract class ClipContextMixin {
    @Inject(method = "getBlockShape", at = @At("HEAD"), cancellable = true)
    private void tacz$ignoreBulletPassThroughBlocks(BlockState state, BlockGetter level, BlockPos pos,
                                                   CallbackInfoReturnable<VoxelShape> cir) {
        if (BulletRayTraceContext.ignores(state)) {
            cir.setReturnValue(Shapes.empty());
        }
    }

    @Inject(method = "getFluidShape", at = @At("HEAD"), cancellable = true)
    private void tacz$ignoreFluidInPassThroughBlocks(FluidState state, BlockGetter level, BlockPos pos,
                                                   CallbackInfoReturnable<VoxelShape> cir) {
        if (BulletRayTraceContext.ignores(level, pos)) {
            cir.setReturnValue(Shapes.empty());
        }
    }
}
