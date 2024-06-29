package com.tacz.guns.debug;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class GunMeleeDebug {
    public static void showRange(LivingEntity user, int distance, Vec3 centrePos, Vec3 eyeVec, float rangeAngle) {
        if (!(user.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        // 起点坐标
        int half = distance / 2;
        Vec3 startPos = user.getEyePosition().subtract(half, half, half);
        // 先尝试生成 distance^3 范围的坐标
        for (int i = 0; i < distance * 2; i++) {
            for (int j = 0; j < distance * 2; j++) {
                for (int k = 0; k < distance * 2; k++) {
                    // 待检查的坐标
                    Vec3 tmpPos = startPos.add(i / 2.0, j / 2.0, k / 2.0);
                    // 待检查的坐标->球心向量
                    Vec3 targetVec = tmpPos.subtract(centrePos);
                    // 目标到球心距离
                    double targetLength = targetVec.length();
                    // 距离在一倍距离之内的，在玩家背后，不进行生成
                    if (targetLength < distance) {
                        continue;
                    }
                    // 计算出向量夹角
                    double degree = Math.toDegrees(Math.acos(targetVec.dot(eyeVec) / (targetLength * distance)));
                    // 向量夹角在范围内的，生成粒子
                    if (degree < (rangeAngle / 2)) {
                        serverLevel.sendParticles(ParticleTypes.FLAME, tmpPos.x, tmpPos.y, tmpPos.z, 1, 0, 0, 0, 0);
                    }
                }
            }
        }
    }
}
