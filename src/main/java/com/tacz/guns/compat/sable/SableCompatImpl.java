package com.tacz.guns.compat.sable;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.StreamSupport;

final class SableCompatImpl {
    private SableCompatImpl() {
    }

    static BlockHitResult clip(Level level, ClipContext context) {
        BlockHitResult result = level.clip(context);
        if (result.getType() == HitResult.Type.MISS) {
            return result;
        }
        SubLevel subLevel = Sable.HELPER.getContaining(level, result.getBlockPos());
        return subLevel == null ? result : toWorldHit(result, subLevel.logicalPose());
    }

    static BlockHitResult toWorldHit(BlockHitResult result, Pose3dc pose) {
        // The hit vector feeds entity traces and packets; the block and face still address the plot.
        return new BlockHitResult(pose.transformPosition(result.getLocation()), result.getDirection(),
                result.getBlockPos(), result.isInside());
    }

    static Vec3 toWorldPosition(Level level, Vec3 position) {
        return Sable.HELPER.projectOutOfSubLevel(level, position);
    }

    static Vec3 toLocalPosition(Level level, BlockPos blockPos, Vec3 position) {
        SubLevel subLevel = Sable.HELPER.getContaining(level, blockPos);
        if (subLevel == null || Sable.HELPER.getContaining(level, position) == subLevel) {
            return position;
        }
        return subLevel.logicalPose().transformPositionInverse(position);
    }

    static Vec3 worldNormal(Level level, BlockPos blockPos, Direction direction) {
        Vec3 normal = Vec3.atLowerCornerOf(direction.getNormal());
        SubLevel subLevel = Sable.HELPER.getContaining(level, blockPos);
        if (subLevel == null) {
            return normal;
        }
        Vector3d rotated = subLevel.logicalPose().orientation().transform(new Vector3d(normal.x, normal.y, normal.z));
        return new Vec3(rotated.x, rotated.y, rotated.z);
    }

    static SableCompat.ExplosionTrace explosionTrace(Level level, Vec3 position,
                                                     ExplosionDamageCalculator calculator, boolean knockback) {
        return new SubLevelExplosionTrace(level, position, calculator, knockback);
    }

    private static final class SubLevelExplosionTrace implements SableCompat.ExplosionTrace {
        private final Level level;
        private final Vec3 position;
        private final ExplosionDamageCalculator calculator;
        private final boolean knockback;
        private final Map<BlockPos, List<SubLevel>> intersecting = new HashMap<>();
        private final Set<BlockPos> pushedBlocks = new HashSet<>();

        private SubLevelExplosionTrace(Level level, Vec3 position, ExplosionDamageCalculator calculator,
                                       boolean knockback) {
            this.level = level;
            this.position = position;
            this.calculator = calculator;
            this.knockback = knockback;
        }

        @Override
        public float sample(Explosion explosion, Vec3 rayPosition, float strength, Set<BlockPos> toBlow) {
            BlockPos worldPos = BlockPos.containing(rayPosition);
            List<SubLevel> subLevels = intersecting.computeIfAbsent(worldPos, pos -> StreamSupport.stream(
                    Sable.HELPER.getAllIntersecting(level, new BoundingBox3d(pos)).spliterator(), false).toList());
            for (SubLevel subLevel : subLevels) {
                if (subLevel.isRemoved()) {
                    continue;
                }
                Pose3dc pose = subLevel.logicalPose();
                // Sample the actual explosion ray, rather than every block in a rotated cell's AABB.
                BlockPos localPos = BlockPos.containing(pose.transformPositionInverse(rayPosition));
                if (Sable.HELPER.getContaining(level, localPos) != subLevel) {
                    continue;
                }
                BlockState state = level.getBlockState(localPos);
                if (state.isAir()) {
                    continue;
                }
                float before = strength;
                var resistance = calculator.getBlockExplosionResistance(explosion, level, localPos, state,
                        level.getFluidState(localPos));
                if (resistance.isPresent()) {
                    strength -= (resistance.get() + 0.3F) * 0.3F;
                }
                if (strength > 0.0F && calculator.shouldBlockExplode(explosion, level, localPos, state, strength)) {
                    toBlow.add(localPos);
                }
                if (knockback && before > 0.0F && strength <= 0.0F && pushedBlocks.add(localPos)
                        && subLevel instanceof ServerSubLevel serverSubLevel) {
                    RigidBodyHandle handle = RigidBodyHandle.of(serverSubLevel);
                    if (handle != null && handle.isValid()) {
                        Vec3 center = localPos.getCenter();
                        Vec3 force = center.subtract(pose.transformPositionInverse(position)).normalize().scale(5.0);
                        handle.applyImpulseAtPoint(center, force);
                    }
                }
                if (strength <= 0.0F) {
                    break;
                }
            }
            return strength;
        }
    }
}
