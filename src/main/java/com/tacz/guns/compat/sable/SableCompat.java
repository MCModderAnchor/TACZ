package com.tacz.guns.compat.sable;

import com.tacz.guns.config.common.AmmoConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;

import javax.annotation.Nullable;
import java.util.Set;

/** Vanilla-only entry points keep Sable optional, including on dedicated servers. */
public final class SableCompat {
    private SableCompat() {
    }

    public static boolean isEnabled() {
        return isLoaded() && AmmoConfig.SABLE_COMPAT.get();
    }

    private static boolean isLoaded() {
        return ModList.get() != null && ModList.get().isLoaded("sable");
    }

    @Nullable
    public static BlockRenderPose blockRenderPose(BlockEntity blockEntity, float partialTick) {
        Level level = blockEntity.getLevel();
        return level != null && level.isClientSide() && isLoaded()
                ? SableClientCompat.blockRenderPose(blockEntity, partialTick) : null;
    }

    public record BlockRenderPose(Vec3 origin, Vec3 center, Matrix4fc rotationAndScale) {
    }

    public static BlockHitResult clip(Level level, ClipContext context) {
        return SableCompatImpl.clip(level, context);
    }

    public static Vec3 toWorldPosition(Level level, Vec3 position) {
        return isEnabled() ? SableCompatImpl.toWorldPosition(level, position) : position;
    }

    public static Vec3 toLocalPosition(Level level, BlockPos blockPos, Vec3 position) {
        return isEnabled() ? SableCompatImpl.toLocalPosition(level, blockPos, position) : position;
    }

    public static Vec3 worldNormal(Level level, BlockPos blockPos, Direction direction) {
        return isEnabled() ? SableCompatImpl.worldNormal(level, blockPos, direction)
                : Vec3.atLowerCornerOf(direction.getNormal());
    }

    @Nullable
    public static BulletHoleAttachment attachBulletHole(Level level, BlockPos pos, Vec3 position) {
        return isEnabled() && level.isClientSide() ? SableClientCompat.attachBulletHole(level, pos, position) : null;
    }

    @Nullable
    public static ExplosionTrace explosionTrace(Level level, Vec3 position, ExplosionDamageCalculator calculator,
                                                 boolean knockback) {
        return isEnabled() ? SableCompatImpl.explosionTrace(level, position, calculator, knockback) : null;
    }

    public interface BulletHoleAttachment {
        Vec3 localPosition();

        Vec3 worldPosition();

        BulletHolePose renderPose(Direction direction, float partialTick);

        boolean isRemoved();
    }

    public record BulletHolePose(Vec3 position, Quaternionf rotation) {
    }

    public interface ExplosionTrace {
        float sample(Explosion explosion, Vec3 rayPosition, float strength, Set<BlockPos> toBlow);
    }
}
