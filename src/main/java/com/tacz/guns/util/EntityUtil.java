package com.tacz.guns.util;

import com.tacz.guns.config.util.HeadShotAABBConfigRead;
import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class EntityUtil {
    private static final Predicate<Entity> PROJECTILE_TARGETS = input -> input != null && input.isPickable() && !input.isSpectator();
    @Nullable
    public static EntityKineticBullet.EntityResult findEntityOnPath(Projectile bulletEntity, Vec3 startVec, Vec3 endVec) {

        Vec3 hitVec = null;
        Entity hitEntity = null;
        boolean headshot = false;
        // 获取子弹 tick 路径上所有的实体
        List<Entity> entities = bulletEntity.level().getEntities(bulletEntity, bulletEntity.getBoundingBox().expandTowards(bulletEntity.getDeltaMovement()).inflate(1.0), PROJECTILE_TARGETS);
        double closestDistance = Double.MAX_VALUE;
        Entity owner = bulletEntity.getOwner();
        for (Entity entity : entities) {
            // 禁止对自己造成伤害（如有需要可以增加 Config 开启对自己的伤害）
            if (!entity.equals(owner)) {
                // 射击无视自己的载具
                if (owner != null && entity.equals(owner.getVehicle())) {
                    continue;
                }
                EntityKineticBullet.EntityResult result = getHitResult(bulletEntity, entity, startVec, endVec);
                if (result == null) {
                    continue;
                }
                Vec3 hitPos = result.getHitPos();
                double distanceToHit = startVec.distanceTo(hitPos);
                if (entity.isAlive()) {
                    if (distanceToHit < closestDistance) {
                        hitVec = hitPos;
                        hitEntity = entity;
                        closestDistance = distanceToHit;
                        headshot = result.isHeadshot();
                    }
                }
            }
        }
        return hitEntity != null ? new EntityKineticBullet.EntityResult(hitEntity, hitVec, headshot) : null;
    }

    @NotNull
    public static List<EntityKineticBullet.EntityResult> findEntitiesOnPath(Projectile bulletEntity, Vec3 startVec, Vec3 endVec) {
        List<EntityKineticBullet.EntityResult> hitEntities = new ArrayList<>();
        List<Entity> entities = bulletEntity.level().getEntities(bulletEntity, bulletEntity.getBoundingBox().expandTowards(bulletEntity.getDeltaMovement()).inflate(1.0), PROJECTILE_TARGETS);
        Entity owner = bulletEntity.getOwner();
        for (Entity entity : entities) {
            if (!entity.equals(owner)) {
                if (owner != null && entity.equals(owner.getVehicle())) {
                    continue;
                }
                EntityKineticBullet.EntityResult result = getHitResult(bulletEntity, entity, startVec, endVec);
                if (result == null) {
                    continue;
                }
                if (entity.isAlive()) {
                    hitEntities.add(result);
                }
            }
        }
        return hitEntities;
    }

    @Nullable
    protected static EntityKineticBullet.EntityResult getHitResult(Projectile bulletEntity, Entity entity, Vec3 startVec, Vec3 endVec) {
        AABB boundingBox = HitboxHelper.getFixedBoundingBox(entity, bulletEntity.getOwner());
        // 计算射线与实体 boundingBox 的交点
        Vec3 hitPos = boundingBox.clip(startVec, endVec).orElse(null);
        // 爆头判定
        if (hitPos == null) {
            return null;
        }
        Vec3 hitBoxPos = hitPos.subtract(entity.position());
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        // 有配置的调用配置
        if (entityId != null) {
            AABB aabb = HeadShotAABBConfigRead.getAABB(entityId);
            if (aabb != null) {
                return new EntityKineticBullet.EntityResult(entity, hitPos, aabb.contains(hitBoxPos));
            }
        }
        // 没有配置的默认给一个
        boolean headshot = false;
        float eyeHeight = entity.getEyeHeight();
        if ((eyeHeight - 0.25) < hitBoxPos.y && hitBoxPos.y < (eyeHeight + 0.25)) {
            headshot = true;
        }
        return new EntityKineticBullet.EntityResult(entity, hitPos, headshot);
    }
}
