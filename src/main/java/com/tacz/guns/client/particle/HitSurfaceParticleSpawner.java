package com.tacz.guns.client.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.client.resource.pojo.display.ammo.HitSurfaceParticle;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.util.ColorHex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.StringUtils;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.List;

public final class HitSurfaceParticleSpawner {
    private static final int MAX_BLOCK_PARTICLE_COUNT = 32;
    private static final int MAX_DISPLAY_PARTICLE_COUNT_PER_HIT = 64;
    private static final float DEFAULT_SURFACE_OFFSET = 0.03f;
    private static final float DEFAULT_RANDOM_OFFSET = 0.01f;
    private static final int DEFAULT_BLOCK_PARTICLE_COUNT = 3;
    private static final float DEFAULT_BLOCK_NORMAL_SPEED = 0.14f;
    private static final float DEFAULT_BLOCK_RANDOM_SPEED = 0.015f;
    private static final float DEFAULT_BLOCK_SPREAD_ANGLE = 30.0f;
    private static final String DUST = "minecraft:dust";
    private static final String DUST_COLOR_TRANSITION = "minecraft:dust_color_transition";
    private static final String BLOCK = "minecraft:block";
    private static final String FALLING_DUST = "minecraft:falling_dust";
    private static final String ITEM = "minecraft:item";

    private HitSurfaceParticleSpawner() {
    }

    public static void spawn(ClientLevel level, Vec3 hitPos, Direction direction, BlockPos blockPos, String ammoId, String gunId, String gunDisplayId, boolean incendiary) {
        if (!RenderConfig.HIT_PARTICLE_ENABLE.get() || !isCloseEnough(hitPos)) {
            return;
        }
        BlockState blockState = level.getBlockState(blockPos);
        ParticleEngine particleEngine = Minecraft.getInstance().particleEngine;
        RandomSource random = level.random;
        Integer blockParticleCount = getAmmoBlockParticleCount(ammoId);
        spawnBlockParticles(particleEngine, random, blockState, hitPos, direction, blockParticleCount, incendiary);
        if (!RenderConfig.HIT_EXTRA_PARTICLE_ENABLE.get()) {
            return;
        }
        List<HitSurfaceParticle> particles = getDisplayParticles(ammoId, gunId, gunDisplayId);
        if (particles == null || particles.isEmpty()) {
            return;
        }
        int remainingCount = MAX_DISPLAY_PARTICLE_COUNT_PER_HIT;
        for (HitSurfaceParticle particle : particles) {
            remainingCount -= spawnDisplayParticle(particleEngine, random, blockState, hitPos, direction, particle, remainingCount);
            if (remainingCount <= 0) {
                return;
            }
        }
    }

    private static boolean isCloseEnough(Vec3 hitPos) {
        int maxDistance = RenderConfig.HIT_PARTICLE_MAX_DISTANCE.get();
        if (maxDistance <= 0) {
            return true;
        }
        Player player = Minecraft.getInstance().player;
        double maxDistanceSqr = (double) maxDistance * (double) maxDistance;
        return player != null && player.distanceToSqr(hitPos) <= maxDistanceSqr;
    }

    private static void spawnBlockParticles(ParticleEngine particleEngine, RandomSource random, BlockState blockState, Vec3 hitPos, Direction direction, @Nullable Integer configuredCount, boolean incendiary) {
        int count = configuredCount == null ? DEFAULT_BLOCK_PARTICLE_COUNT : Math.min(configuredCount, MAX_BLOCK_PARTICLE_COUNT);
        if (count <= 0) {
            return;
        }
        ParticleOptions options = incendiary ? ParticleTypes.LAVA : new BlockParticleOption(ParticleTypes.BLOCK, blockState);
        for (int i = 0; i < count; i++) {
            Vec3 pos = randomPosition(random, hitPos, direction, DEFAULT_SURFACE_OFFSET, DEFAULT_RANDOM_OFFSET);
            Vec3 speed = randomVelocity(random,
                    direction,
                    DEFAULT_BLOCK_NORMAL_SPEED,
                    DEFAULT_BLOCK_RANDOM_SPEED,
                    DEFAULT_BLOCK_SPREAD_ANGLE);
            particleEngine.createParticle(options, pos.x, pos.y, pos.z, speed.x, speed.y, speed.z);
        }
    }

    @Nullable
    private static Integer getAmmoBlockParticleCount(String ammoId) {
        try {
            return TimelessAPI.getClientAmmoIndex(ResourceLocation.parse(ammoId))
                    .map(ammoIndex -> ammoIndex.getHitBlockParticleCount())
                    .orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable
    private static List<HitSurfaceParticle> getDisplayParticles(String ammoId, String gunId, String gunDisplayId) {
        try {
            ResourceLocation gunDisplayLocation = ResourceLocation.parse(gunDisplayId);
            ResourceLocation gunLocation = ResourceLocation.parse(gunId);
            var gunDisplay = TimelessAPI.getGunDisplay(gunDisplayLocation, gunLocation);
            if (gunDisplay.isPresent() && gunDisplay.get().getHitSurfaceParticles() != null) {
                return gunDisplay.get().getHitSurfaceParticles();
            }
            return TimelessAPI.getClientAmmoIndex(ResourceLocation.parse(ammoId))
                    .map(ammoIndex -> ammoIndex.getHitSurfaceParticles())
                    .orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static int spawnDisplayParticle(ParticleEngine particleEngine, RandomSource random, BlockState blockState, Vec3 hitPos, Direction direction, HitSurfaceParticle particle, int maxCount) {
        ParticleOptions options = parseParticle(blockState, particle);
        if (options == null || maxCount <= 0) {
            return 0;
        }
        int count = Math.min(randomCount(random, particle), maxCount);
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            Vec3 pos = randomPosition(random, hitPos, direction, particle.getSurfaceOffset(), particle.getRandomOffset());
            Vec3 speed = randomVelocity(random, direction, particle.getNormalSpeed(), particle.getRandomSpeed(), particle.getSpreadAngle());
            Particle result = particleEngine.createParticle(options, pos.x, pos.y, pos.z, speed.x, speed.y, speed.z);
            if (result != null) {
                applyParticleOverrides(result, particle);
                spawned++;
            }
        }
        return spawned;
    }

    private static int randomCount(RandomSource random, HitSurfaceParticle particle) {
        int min = particle.getCountMin();
        int max = particle.getCountMax();
        if (max < min) {
            int swap = min;
            min = max;
            max = swap;
        }
        if (max == min) {
            return min;
        }
        return min + random.nextInt(max - min + 1);
    }

    @Nullable
    private static ParticleOptions parseParticle(BlockState blockState, HitSurfaceParticle particle) {
        String name = particle.getName();
        if (StringUtils.isBlank(name)) {
            return null;
        }
        String trimmedName = name.trim();
        boolean hasArguments = trimmedName.split("\\s+", 2).length > 1;
        if (hasArguments) {
            try {
                return ParticleArgument.readParticle(new StringReader(trimmedName), com.tacz.guns.client.util.ClientRegistryAccess.registries());
            } catch (CommandSyntaxException ignored) {
                return null;
            }
        }
        String particleId = normalizeParticleId(trimmedName);
        if (BLOCK.equals(particleId)) {
            return new BlockParticleOption(ParticleTypes.BLOCK, blockState);
        }
        if (FALLING_DUST.equals(particleId)) {
            return new BlockParticleOption(ParticleTypes.FALLING_DUST, blockState);
        }
        if (DUST.equals(particleId)) {
            float[] color = colorOrDefault(particle.getColor(), 1.0f, 0.35f, 0.1f);
            return new DustParticleOptions(new Vector3f(color[0], color[1], color[2]), positiveScale(particle));
        }
        if (DUST_COLOR_TRANSITION.equals(particleId)) {
            float[] color = colorOrDefault(particle.getColor(), 1.0f, 0.35f, 0.1f);
            Vector3f fromColor = new Vector3f(color[0], color[1], color[2]);
            Vector3f toColor = new Vector3f(color[0] * 0.25f, color[1] * 0.25f, color[2] * 0.25f);
            return new DustColorTransitionOptions(fromColor, toColor, positiveScale(particle));
        }
        if (ITEM.equals(particleId)) {
            return null;
        }
        try {
            return ParticleArgument.readParticle(new StringReader(trimmedName), com.tacz.guns.client.util.ClientRegistryAccess.registries());
        } catch (CommandSyntaxException ignored) {
            return null;
        }
    }

    private static String normalizeParticleId(String particleName) {
        String firstToken = particleName.split("\\s+", 2)[0];
        ResourceLocation id = ResourceLocation.tryParse(firstToken);
        if (id == null) {
            return firstToken;
        }
        return id.toString();
    }

    private static float positiveScale(HitSurfaceParticle particle) {
        Float scale = particle.getScale();
        return scale == null ? 1.0f : Math.max(scale, 0.0f);
    }

    private static float[] colorOrDefault(@Nullable String color, float red, float green, float blue) {
        if (StringUtils.isBlank(color)) {
            return new float[]{red, green, blue};
        }
        return ColorHex.colorTextToRbgFloatArray(color);
    }

    private static void applyParticleOverrides(Particle result, HitSurfaceParticle particle) {
        result.setLifetime(particle.getLifeTime());
        String color = particle.getColor();
        if (StringUtils.isNotBlank(color)) {
            float[] rgb = ColorHex.colorTextToRbgFloatArray(color);
            result.setColor(rgb[0], rgb[1], rgb[2]);
        }
        Float scale = particle.getScale();
        if (scale != null && scale > 0 && !usesScaleInParticleOptions(particle)) {
            result.scale(scale);
        }
    }

    private static boolean usesScaleInParticleOptions(HitSurfaceParticle particle) {
        String name = particle.getName();
        if (StringUtils.isBlank(name)) {
            return false;
        }
        String particleId = normalizeParticleId(name.trim());
        return DUST.equals(particleId) || DUST_COLOR_TRANSITION.equals(particleId);
    }

    private static Vec3 randomPosition(RandomSource random, Vec3 hitPos, Direction direction, float surfaceOffset, float randomOffset) {
        Vec3 normal = Vec3.atLowerCornerOf(direction.getNormal());
        double offsetX = randomOffset == 0 ? 0 : random.nextGaussian() * randomOffset;
        double offsetY = randomOffset == 0 ? 0 : random.nextGaussian() * randomOffset;
        double offsetZ = randomOffset == 0 ? 0 : random.nextGaussian() * randomOffset;
        return hitPos.add(normal.scale(surfaceOffset)).add(offsetX, offsetY, offsetZ);
    }

    private static Vec3 randomVelocity(RandomSource random, Direction direction, float normalSpeed, float randomSpeed, float spreadAngle) {
        Vec3 normal = Vec3.atLowerCornerOf(direction.getNormal()).normalize();
        Vec3 tangent = Math.abs(normal.y) > 0.9 ? new Vec3(1, 0, 0) : normal.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 bitangent = normal.cross(tangent).normalize();
        double spread = Math.tan(Math.toRadians(spreadAngle));
        Vec3 cone = normal
                .add(tangent.scale(random.nextGaussian() * spread))
                .add(bitangent.scale(random.nextGaussian() * spread))
                .normalize()
                .scale(normalSpeed);
        Vec3 randomVector = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).scale(randomSpeed);
        return cone.add(randomVector);
    }
}
