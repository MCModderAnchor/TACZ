package com.tacz.guns.util.block;

import com.tacz.guns.config.common.AmmoConfig;
import com.tacz.guns.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public final class BlockRayTrace {
    private static final Predicate<BlockState> IGNORES = input -> input != null &&
            input.is(ModBlocks.BULLET_IGNORE_BLOCKS);

    public static BlockHitResult rayTraceBlocks(Level level, ClipContext context) {
        return performRayTrace(context, (rayTraceContext, blockPos) -> {
            BlockState blockState = level.getBlockState(blockPos);
            // 这里添加判断方块是否可以穿透，如果可以穿透则返回 null
            List<String> ids = AmmoConfig.PASS_THROUGH_BLOCKS.get();
            ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(blockState.getBlock());
            if (blockId != null && ids.contains(blockId.toString())) {
                return null;
            }
            // tag
            if (IGNORES.test(blockState)) {
                return null;
            }
            return getBlockHitResult(level, rayTraceContext, blockPos, blockState);
        }, (rayTraceContext) -> {
            Vec3 vec3 = rayTraceContext.getFrom().subtract(rayTraceContext.getTo());
            return BlockHitResult.miss(rayTraceContext.getTo(), Direction.getNearest(vec3.x, vec3.y, vec3.z), BlockPos.containing(rayTraceContext.getTo()));
        });
    }

    @Nullable
    private static BlockHitResult getBlockHitResult(Level level, ClipContext rayTraceContext, BlockPos blockPos, BlockState blockState) {
        FluidState fluidState = level.getFluidState(blockPos);
        Vec3 startVec = rayTraceContext.getFrom();
        Vec3 endVec = rayTraceContext.getTo();
        VoxelShape blockShape = rayTraceContext.getBlockShape(blockState, level, blockPos);
        BlockHitResult blockResult = level.clipWithInteractionOverride(startVec, endVec, blockPos, blockShape, blockState);
        VoxelShape fluidShape = rayTraceContext.getFluidShape(fluidState, level, blockPos);
        BlockHitResult fluidResult = fluidShape.clip(startVec, endVec, blockPos);
        double blockDistance = blockResult == null ? Double.MAX_VALUE : rayTraceContext.getFrom().distanceToSqr(blockResult.getLocation());
        double fluidDistance = fluidResult == null ? Double.MAX_VALUE : rayTraceContext.getFrom().distanceToSqr(fluidResult.getLocation());
        return blockDistance <= fluidDistance ? blockResult : fluidResult;
    }

    private static <T> T performRayTrace(ClipContext context, BiFunction<ClipContext, BlockPos, T> hitFunction, Function<ClipContext, T> missFactory) {
        Vec3 startVec = context.getFrom();
        Vec3 endVec = context.getTo();
        if (!startVec.equals(endVec)) {
            double startX = Mth.lerp(-0.0000001, endVec.x, startVec.x);
            double startY = Mth.lerp(-0.0000001, endVec.y, startVec.y);
            double startZ = Mth.lerp(-0.0000001, endVec.z, startVec.z);
            double endX = Mth.lerp(-0.0000001, startVec.x, endVec.x);
            double endY = Mth.lerp(-0.0000001, startVec.y, endVec.y);
            double endZ = Mth.lerp(-0.0000001, startVec.z, endVec.z);

            int blockX = Mth.floor(endX);
            int blockY = Mth.floor(endY);
            int blockZ = Mth.floor(endZ);

            BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos(blockX, blockY, blockZ);
            T t = hitFunction.apply(context, mutablePos);
            if (t != null) {
                return t;
            }

            double deltaX = startX - endX;
            double deltaY = startY - endY;
            double deltaZ = startZ - endZ;
            int signX = Mth.sign(deltaX);
            int signY = Mth.sign(deltaY);
            int signZ = Mth.sign(deltaZ);
            double d9 = signX == 0 ? Double.MAX_VALUE : (double) signX / deltaX;
            double d10 = signY == 0 ? Double.MAX_VALUE : (double) signY / deltaY;
            double d11 = signZ == 0 ? Double.MAX_VALUE : (double) signZ / deltaZ;
            double d12 = d9 * (signX > 0 ? 1.0D - Mth.frac(endX) : Mth.frac(endX));
            double d13 = d10 * (signY > 0 ? 1.0D - Mth.frac(endY) : Mth.frac(endY));
            double d14 = d11 * (signZ > 0 ? 1.0D - Mth.frac(endZ) : Mth.frac(endZ));

            while (d12 <= 1.0D || d13 <= 1.0D || d14 <= 1.0D) {
                if (d12 < d13) {
                    if (d12 < d14) {
                        blockX += signX;
                        d12 += d9;
                    } else {
                        blockZ += signZ;
                        d14 += d11;
                    }
                } else if (d13 < d14) {
                    blockY += signY;
                    d13 += d10;
                } else {
                    blockZ += signZ;
                    d14 += d11;
                }

                T t1 = hitFunction.apply(context, mutablePos.set(blockX, blockY, blockZ));
                if (t1 != null) {
                    return t1;
                }
            }
        }
        return missFactory.apply(context);
    }
}
