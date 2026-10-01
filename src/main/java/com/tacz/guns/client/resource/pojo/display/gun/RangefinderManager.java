package com.tacz.guns.client.resource.pojo.display.gun;

import com.tacz.guns.api.GunProperties;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.event.CameraSetupEvent;
import com.tacz.guns.config.common.AmmoConfig;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.util.HitboxHelper;
import com.tacz.guns.util.block.BlockRayTrace;
import com.tacz.guns.util.math.Easing;
import com.tacz.guns.util.math.MathUtil;
import com.tacz.guns.util.time.GunTime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Predicate;

@OnlyIn(Dist.CLIENT)
public final class RangefinderManager {
    private static final String MISS_TEXT = "---";
    private static final double ENTITY_XZ_EXPAND = 0.25;
    private static final double ENTITY_Y_EXPAND = 8.0;
    private static final long MILLIS_PER_TICK = 50L;
    private static final Predicate<Entity> RANGEFINDER_TARGETS = entity -> entity != null && entity.isPickable() && !entity.isSpectator() && entity.isAlive();

    private static Rangefinder currentConfig = null;
    private static long lastUpdateTimestamp = 0L;
    private static int lastRefreshTick = 2;
    private static String text = MISS_TEXT;
    private static boolean hitEntity = false;
    private static double hitDistance = 0.0;
    private static float impactProjectionY = 0.0f;
    private static int impactPredictionColor = 0xFFFFFFFF;
    private static float impactPredictionScale = 1.0f;
    private static boolean showImpactPrediction = false;

    private RangefinderManager() {
    }

    public static void resetTimestamps() {
        currentConfig = null;
        lastUpdateTimestamp = 0L;
        lastRefreshTick = 2;
        clearResult();
    }

    public static void setCurrentConfig(@Nullable Rangefinder config) {
        currentConfig = config;
    }

    public static void clearCurrentConfig() {
        currentConfig = null;
    }

    public static String getText(ItemStack stack) {
        if (currentConfig == null) {
            return "";
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || stack == null || stack.isEmpty()) {
            lastUpdateTimestamp = 0L;
            clearResult();
            return MISS_TEXT;
        }
        int refreshTick = currentConfig.getRefreshTick();
        long now = GunTime.nowMillis();
        if (lastUpdateTimestamp != 0L && GunTime.isWithin(lastUpdateTimestamp, refreshTick * MILLIS_PER_TICK) && refreshTick == lastRefreshTick) {
            return text;
        }
        lastUpdateTimestamp = now;
        lastRefreshTick = refreshTick;
        updateResult(player, stack, currentConfig);
        return text;
    }

    public static boolean shouldRenderImpactPrediction() {
        if (!showImpactPrediction) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !GunTime.isWithin(lastUpdateTimestamp, (lastRefreshTick + 1L) * MILLIS_PER_TICK)) {
            showImpactPrediction = false;
            return false;
        }
        if (!(player instanceof IClientPlayerGunOperator operator) || operator.getClientAimingProgress(mc.getFrameTime()) < 0.9f) {
            showImpactPrediction = false;
            return false;
        }
        return true;
    }

    public static float getImpactProjectionY() {
        return impactProjectionY;
    }

    public static int getImpactPredictionColor() {
        return impactPredictionColor;
    }

    public static float getImpactPredictionScale() {
        return impactPredictionScale;
    }

    private static void updateResult(LocalPlayer player, ItemStack stack, Rangefinder config) {
        impactPredictionColor = config.getImpactPredictionColor();
        impactPredictionScale = config.getImpactPredictionScale();
        if (!(stack.getItem() instanceof IGun iGun) || !(player instanceof IClientPlayerGunOperator operator)) {
            clearResult();
            return;
        }
        if (operator.getClientAimingProgress(Minecraft.getInstance().getFrameTime()) < 0.9f) {
            clearResult();
            return;
        }

        float maxDistance = config.getMaxDistance();
        Vec3 start = player.getEyePosition(Minecraft.getInstance().getFrameTime());
        Vec3 look = player.getViewVector(Minecraft.getInstance().getFrameTime());
        Vec3 end = start.add(look.scale(maxDistance));

        BlockHitResult blockResult = BlockRayTrace.rayTraceBlocks(player.level(), new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double blockDistance = blockResult.getType() == HitResult.Type.MISS ? Double.MAX_VALUE : start.distanceTo(blockResult.getLocation());
        EntityHit entityHit = findEntityHit(player, start, end, blockDistance).orElse(null);

        if (entityHit != null) {
            double distance = start.distanceTo(entityHit.hitPos());
            setDistanceText(distance, true, maxDistance);
            updateImpactPrediction(player, stack, iGun, config, distance);
        } else if (blockDistance != Double.MAX_VALUE) {
            setDistanceText(blockDistance, false, maxDistance);
            showImpactPrediction = false;
        } else {
            clearResult();
        }
    }

    private static Optional<EntityHit> findEntityHit(LocalPlayer player, Vec3 start, Vec3 end, double maxDistance) {
        AABB scanBox = new AABB(start, end).inflate(ENTITY_XZ_EXPAND, ENTITY_Y_EXPAND, ENTITY_XZ_EXPAND);
        return player.level().getEntities(player, scanBox, RANGEFINDER_TARGETS)
                .stream()
                .filter(entity -> !entity.equals(player))
                .filter(entity -> !entity.isPassengerOfSameVehicle(player))
                .map(entity -> HitboxHelper.getFixedBoundingBox(entity, player).inflate(ENTITY_XZ_EXPAND, ENTITY_Y_EXPAND, ENTITY_XZ_EXPAND).clip(start, end).map(EntityHit::new).orElse(null))
                .filter(hit -> hit != null && start.distanceTo(hit.hitPos()) <= maxDistance)
                .min(Comparator.comparingDouble(hit -> start.distanceTo(hit.hitPos())));
    }

    private static void setDistanceText(double distance, boolean entityHit, float maxDistance) {
        hitDistance = distance;
        hitEntity = entityHit;
        int roundedDistance = Mth.floor(distance + 0.5);
        // 与距离使用相同的取整规则，按测距上限的位数补零。
        int digits = Integer.toString(Mth.floor(maxDistance + 0.5)).length();
        String distanceText = Integer.toString(roundedDistance);
        text = "0".repeat(Math.max(0, digits - distanceText.length())) + distanceText;
    }

    private static void clearResult() {
        text = MISS_TEXT;
        hitEntity = false;
        hitDistance = 0.0;
        impactProjectionY = 0.0f;
        showImpactPrediction = false;
    }

    private static void updateImpactPrediction(LocalPlayer player, ItemStack stack, IGun iGun, Rangefinder config, double distance) {
        showImpactPrediction = false;
        if (!hitEntity || !config.isShowImpactPrediction() || MISS_TEXT.equals(text) || distance <= 0.0) {
            return;
        }
        ResourceLocation gunId = iGun.getGunId(stack);
        ClientGunIndex gunIndex = TimelessAPI.getClientGunIndex(gunId).orElse(null);
        if (gunIndex == null) {
            return;
        }
        GunData gunData = gunIndex.getGunData();
        if (gunData == null || gunData.getBulletData() == null) {
            return;
        }
        BulletData bulletData = gunData.getBulletData();
        float speed = getAmmoSpeed(player, stack, bulletData);
        if (speed <= 0.0f) {
            return;
        }
        Vec3 look = player.getViewVector(Minecraft.getInstance().getFrameTime());
        double drop = calculateDrop(distance, look.y, speed / 20.0f, bulletData.getGravity(), bulletData.getFriction());
        if (!Double.isFinite(drop)) {
            return;
        }

        float fov = getCurrentFov(player, stack, iGun);
        double angle = Math.atan2(-drop, Math.max(distance, 0.001));
        impactProjectionY = (float) (Math.tan(angle) / Math.tan(Math.toRadians(fov) / 2.0));
        showImpactPrediction = Float.isFinite(impactProjectionY);
    }

    private static float getAmmoSpeed(LocalPlayer player, ItemStack stack, BulletData bulletData) {
        float speed = bulletData.getSpeed();
        AttachmentCacheProperty cacheProperty = IGunOperator.fromLivingEntity(player).getCacheProperty();
        if (cacheProperty != null) {
            try {
                speed = cacheProperty.getCache(GunProperties.AMMO_SPEED);
            } catch (RuntimeException ignored) {
                speed = bulletData.getSpeed();
            }
        }
        return Math.max(0.0f, speed * AmmoConfig.GLOBAL_BULLET_SPEED_MODIFIER.get().floatValue());
    }

    private static float getCurrentFov(LocalPlayer player, ItemStack stack, IGun iGun) {
        float configuredFov = Minecraft.getInstance().options.fov().get().floatValue();
        float aimingProgress = IClientPlayerGunOperator.fromLocalPlayer(player).getClientAimingProgress(Minecraft.getInstance().getFrameTime());
        AdsFov.Channel curve = TimelessAPI.getGunDisplay(stack)
                .map(GunDisplayInstance::getAdsFov).map(AdsFov::getWorld).orElse(null);
        float fovProgress = curve == null ? (float) Easing.easeInExpo(aimingProgress)
                : curve.evaluate(aimingProgress);
        float zoom = iGun.getAimingZoom(stack);
        float targetFov = (float) MathUtil.magnificationToFov(1 + (zoom - 1) * fovProgress, configuredFov);
        if (curve != null && curve.isSmooth()) {
            float actualFov = CameraSetupEvent.WORLD_FOV_DYNAMICS.get();
            if (Float.isFinite(actualFov) && actualFov > 0) {
                return actualFov;
            }
        }
        return targetFov;
    }

    private static double calculateDrop(double distance, double lookY, float speedPerTick, float gravity, float friction) {
        double traveled = 0.0;
        double vertical = 0.0;
        double speed = speedPerTick;
        double verticalVelocity = lookY * speedPerTick;
        int maxTicks = 20 * 30;
        for (int i = 0; i < maxTicks && traveled < distance && speed > 1.0E-4; i++) {
            double step = Math.min(speed, distance - traveled);
            double ratio = step / speed;
            vertical += verticalVelocity * ratio;
            traveled += step;
            speed *= 1.0f - friction;
            verticalVelocity = verticalVelocity * (1.0f - friction) - gravity;
        }
        return vertical - lookY * distance;
    }

    private record EntityHit(Vec3 hitPos) {
    }
}
