package com.tacz.guns.client.particle;

import com.tacz.guns.init.ModBlocks;
import com.tacz.guns.particles.BulletHoleOption;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashMap;
import java.util.Map;

public final class HitBlockEffectHandler {
    private static final int MAX_SAME_STEP_SOUND_PER_TICK = 1;
    private static final double MAX_HIT_STEP_SOUND_DISTANCE_SQR = 64.0D * 64.0D;
    private static final Map<SoundEvent, SoundLimit> STEP_SOUND_LIMITS = new HashMap<>();

    private HitBlockEffectHandler() {
    }

    public static void handle(ClientLevel level, Vec3 hitPos, Direction direction, BlockPos blockPos, String ammoId, String gunId, String gunDisplayId, boolean incendiary) {
        BlockState state = level.getBlockState(blockPos);
        if (state.is(ModBlocks.TARGET.get()) || shouldRemove(level, blockPos, hitPos)) {
            return;
        }
        HitSurfaceParticleSpawner.spawn(level, hitPos, direction, blockPos, ammoId, gunId, gunDisplayId, incendiary);
        playHitBlockStepSound(level, state, hitPos.x, hitPos.y, hitPos.z);
        if (BulletHoleParticle.isBulletHoleEnabled(ammoId, gunId, gunDisplayId)) {
            level.addParticle(new BulletHoleOption(direction, blockPos, ammoId, gunId, gunDisplayId, incendiary), true, hitPos.x, hitPos.y, hitPos.z, 0, 0, 0);
        }
    }

    private static void playHitBlockStepSound(ClientLevel level, BlockState state, double x, double y, double z) {
        if (!canHearHitBlockStepSound(x, y, z)) {
            return;
        }
        SoundType soundType = state.getSoundType();
        SoundEvent stepSound = soundType.getStepSound();
        if (!canPlayStepSound(level, stepSound)) {
            return;
        }
        float volume = soundType.getVolume() * 0.6F;
        float pitch = soundType.getPitch() * (0.9F + level.random.nextFloat() * 0.2F);
        level.playLocalSound(x, y, z, stepSound, SoundSource.BLOCKS, volume, pitch, false);
    }

    private static boolean canHearHitBlockStepSound(double x, double y, double z) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return false;
        }
        return minecraft.player.distanceToSqr(x, y, z) <= MAX_HIT_STEP_SOUND_DISTANCE_SQR;
    }

    private static boolean canPlayStepSound(ClientLevel level, SoundEvent sound) {
        long gameTime = level.getGameTime();
        SoundLimit limit = STEP_SOUND_LIMITS.get(sound);
        if (limit == null || limit.gameTime != gameTime) {
            STEP_SOUND_LIMITS.put(sound, new SoundLimit(gameTime, 1));
            return true;
        }
        if (limit.count >= MAX_SAME_STEP_SOUND_PER_TICK) {
            return false;
        }
        limit.count++;
        return true;
    }

    private static boolean shouldRemove(ClientLevel level, BlockPos blockPos, Vec3 hitPos) {
        BlockState blockState = level.getBlockState(blockPos);
        if (blockState.isAir()) {
            return true;
        }
        VoxelShape shape = blockState.getCollisionShape(level, blockPos);
        if (shape.isEmpty()) {
            return true;
        }
        AABB baseBlockBoundingBox = shape.bounds();
        AABB blockBoundingBox = baseBlockBoundingBox.move(blockPos);
        boolean intersects = blockBoundingBox.intersects(
                hitPos.x - 0.1, hitPos.y - 0.1, hitPos.z - 0.1,
                hitPos.x + 0.1, hitPos.y + 0.1, hitPos.z + 0.1);
        return !intersects;
    }

    private static class SoundLimit {
        private final long gameTime;
        private int count;

        private SoundLimit(long gameTime, int count) {
            this.gameTime = gameTime;
            this.count = count;
        }
    }
}
