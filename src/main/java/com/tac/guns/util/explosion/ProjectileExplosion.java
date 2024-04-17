package com.tac.guns.util.explosion;

import com.google.common.collect.Sets;
import com.tac.guns.config.common.AmmoConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.ProtectionEnchantment;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ProjectileExplosion extends Explosion {
    private static final ExplosionDamageCalculator DEFAULT_CONTEXT = new ExplosionDamageCalculator();
    private final Level level;
    private final double x;
    private final double y;
    private final double z;
    private final float power;
    private final float radius;
    private final Entity exploder;
    private final ExplosionDamageCalculator damageCalculator;

    public ProjectileExplosion(Level level, Entity exploder, @Nullable DamageSource source, @Nullable ExplosionDamageCalculator damageCalculator, double x, double y, double z, float power, float radius, Explosion.BlockInteraction mode) {
        super(level, exploder, source, damageCalculator, x, y, z, radius, AmmoConfig.EXPLOSIVE_AMMO_FIRE.get(), mode);
        this.level = level;
        this.x = x;
        this.y = y;
        this.z = z;
        this.power = power;
        this.radius = radius;
        this.exploder = exploder;
        this.damageCalculator = damageCalculator == null ? DEFAULT_CONTEXT : damageCalculator;
    }

    private static BlockHitResult rayTraceBlocks(Level level, ClipContext context) {
        return performRayTrace(context, (rayTraceContext, blockPos) -> {
            BlockState blockState = level.getBlockState(blockPos);
            // 这里添加判断方块是否可以穿透，如果可以穿透则返回 null
            List<String> ids = AmmoConfig.EXPLOSIVE_PASS_THROUGH_BLOCKS.get();
            ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(blockState.getBlock());
            if (blockId != null && ids.contains(blockId.toString())) {
                return null;
            }
            return getBlockHitResult(level, rayTraceContext, blockPos, blockState);
        }, (rayTraceContext) -> {
            Vec3 vec3 = rayTraceContext.getFrom().subtract(rayTraceContext.getTo());
            return BlockHitResult.miss(rayTraceContext.getTo(), Direction.getNearest(vec3.x, vec3.y, vec3.z), new BlockPos(rayTraceContext.getTo()));
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

    @Override
    public void explode() {
        this.level.gameEvent(this.exploder, GameEvent.EXPLODE, new BlockPos(this.x, this.y, this.z));
        Set<BlockPos> set = Sets.newHashSet();
        int i = 16;

        for (int x = 0; x < i; ++x) {
            for (int y = 0; y < i; ++y) {
                for (int z = 0; z < i; ++z) {
                    if (x == 0 || x == i - 1 || y == 0 || y == i - 1 || z == 0 || z == i - 1) {
                        double d0 = ((float) x / (i - 1) * 2.0F - 1.0F);
                        double d1 = ((float) y / (i - 1) * 2.0F - 1.0F);
                        double d2 = ((float) z / (i - 1) * 2.0F - 1.0F);
                        double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
                        d0 /= d3;
                        d1 /= d3;
                        d2 /= d3;
                        float f = this.radius * (0.7F + this.level.random.nextFloat() * 0.6F);
                        double blockX = this.x;
                        double blockY = this.y;
                        double blockZ = this.z;

                        for (float f1 = 0.3F; f > 0.0F; f -= 0.22500001F) {
                            BlockPos pos = new BlockPos(blockX, blockY, blockZ);
                            BlockState blockState = this.level.getBlockState(pos);
                            FluidState fluidState = this.level.getFluidState(pos);
                            if (!this.level.isInWorldBounds(pos)) {
                                break;
                            }

                            Optional<Float> optional = this.damageCalculator.getBlockExplosionResistance(this, this.level, pos, blockState, fluidState);
                            if (optional.isPresent()) {
                                f -= (optional.get() + f1) * f1;
                            }

                            if (f > 0.0F && this.damageCalculator.shouldBlockExplode(this, this.level, pos, blockState, f)) {
                                set.add(pos);
                            }

                            blockX += d0 * (double) f1;
                            blockY += d1 * (double) f1;
                            blockZ += d2 * (double) f1;
                        }
                    }
                }
            }
        }

        this.getToBlow().addAll(set);
        float radius = this.radius;
        int minX = Mth.floor(this.x - (double) radius - 1.0D);
        int maxX = Mth.floor(this.x + (double) radius + 1.0D);
        int minY = Mth.floor(this.y - (double) radius - 1.0D);
        int maxY = Mth.floor(this.y + (double) radius + 1.0D);
        int minZ = Mth.floor(this.z - (double) radius - 1.0D);
        int maxZ = Mth.floor(this.z + (double) radius + 1.0D);
        radius *= 2;
        List<Entity> entities = this.level.getEntities(this.exploder, new AABB(minX, minY, minZ, maxX, maxY, maxZ));
        net.minecraftforge.event.ForgeEventFactory.onExplosionDetonate(this.level, this, entities, radius);
        Vec3 explosionPos = new Vec3(this.x, this.y, this.z);

        for (Entity entity : entities) {
            if (entity.ignoreExplosion()) {
                continue;
            }

            AABB boundingBox = entity.getBoundingBox();
            BlockHitResult result;
            double strength;
            double deltaX;
            double deltaY;
            double deltaZ;
            double minDistance = radius;

            Vec3[] d = new Vec3[15];

            if (!(entity instanceof LivingEntity)) {
                strength = Math.sqrt(entity.distanceToSqr(explosionPos)) * 2 / radius;
                deltaX = entity.getX() - this.x;
                deltaY = (entity instanceof PrimedTnt ? entity.getY() : entity.getEyeY()) - this.y;
                deltaZ = entity.getZ() - this.z;
            } else {
                deltaX = (boundingBox.maxX + boundingBox.minX) / 2;
                deltaY = (boundingBox.maxY + boundingBox.minY) / 2;
                deltaZ = (boundingBox.maxZ + boundingBox.minZ) / 2;
                d[0] = new Vec3(boundingBox.minX, boundingBox.minY, boundingBox.minZ);
                d[1] = new Vec3(boundingBox.minX, boundingBox.minY, boundingBox.maxZ);
                d[2] = new Vec3(boundingBox.minX, boundingBox.maxY, boundingBox.minZ);
                d[3] = new Vec3(boundingBox.maxX, boundingBox.minY, boundingBox.minZ);
                d[4] = new Vec3(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ);
                d[5] = new Vec3(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ);
                d[6] = new Vec3(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ);
                d[7] = new Vec3(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ);
                d[8] = new Vec3(boundingBox.minX, deltaY, deltaZ);
                d[9] = new Vec3(boundingBox.maxX, deltaY, deltaZ);
                d[10] = new Vec3(deltaX, boundingBox.minY, deltaZ);
                d[11] = new Vec3(deltaX, boundingBox.maxY, deltaZ);
                d[12] = new Vec3(deltaX, deltaY, boundingBox.minZ);
                d[13] = new Vec3(deltaX, deltaY, boundingBox.maxZ);
                d[14] = new Vec3(deltaX, deltaY, deltaZ);
                for (int s = 0; s < 15; s++) {
                    result = rayTraceBlocks(this.level, new ClipContext(explosionPos, d[s], ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));
                    minDistance = (result.getType() != BlockHitResult.Type.BLOCK) ? Math.min(minDistance, explosionPos.distanceTo(d[s])) : minDistance;
                }
                strength = minDistance * 2 / radius;
                deltaX -= this.x;
                deltaY -= this.y;
                deltaZ -= this.z;
            }

            if (strength > 1.0D) {
                continue;
            }

            double distanceToExplosion = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

            if (distanceToExplosion != 0.0D) {
                deltaX /= distanceToExplosion;
                deltaY /= distanceToExplosion;
                deltaZ /= distanceToExplosion;
            }

            double damage = 1.0D - strength;
            entity.hurt(this.getDamageSource(), (float) damage * this.power);

            if (entity instanceof LivingEntity) {
                damage = (float) ProtectionEnchantment.getExplosionKnockbackAfterDampener((LivingEntity) entity, damage);
            }

            // 启用击退效果
            if (AmmoConfig.EXPLOSIVE_AMMO_KNOCK_BACK.get()) {
                entity.setDeltaMovement(entity.getDeltaMovement().add(deltaX * damage * radius / 5, deltaY * damage * radius / 5, deltaZ * damage * radius / 5));
                if (entity instanceof Player player) {
                    if (!player.isSpectator() && (!player.isCreative() || !player.getAbilities().flying)) {
                        this.getHitPlayers().put(player, new Vec3(deltaX * damage * radius / 5, deltaY * damage * radius / 5, deltaZ * damage * radius / 5));
                    }
                }
            }
        }
    }
}
