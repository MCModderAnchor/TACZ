package com.tacz.guns.api.client.other;

import com.google.common.collect.Maps;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;

/**
 * 简单的第三人称持枪动画 Manager
 */
public final class ThirdPersonManager {
    private static final Map<String, IThirdPersonAnimation> CACHE = Maps.newHashMap();
    private static final String RESERVED_DEFAULT_NAME = "default";
    private static final IThirdPersonAnimation DEFAULT = new IThirdPersonAnimation() {
        @Override
        public void animateGunHold(LivingEntity entity, ModelPart rightArm, ModelPart leftArm, ModelPart body, ModelPart head) {
            rightArm.yRot = -0.3F + head.yRot;
            leftArm.yRot = 0.8F + head.yRot;
            rightArm.xRot = -1.4F + head.xRot;
            leftArm.xRot = -1.4F + head.xRot;
        }

        @Override
        public void animateGunAim(LivingEntity entity, ModelPart rightArm, ModelPart leftArm, ModelPart body, ModelPart head, float aimProgress) {
            float lerp1 = Mth.lerp(aimProgress, 0.3f, 0.35f);
            float lerp2 = Mth.lerp(aimProgress, 1.4f, 1.6f);
            rightArm.yRot = -lerp1 + head.yRot;
            leftArm.yRot = 0.8F + head.yRot;
            rightArm.xRot = -lerp2 + head.xRot;
            leftArm.xRot = -lerp2 + head.xRot;
        }
    };

    public static void registerDefault() {
        CACHE.put(RESERVED_DEFAULT_NAME, DEFAULT);
    }

    public static void register(String name, IThirdPersonAnimation animation) {
        if (name.equals(RESERVED_DEFAULT_NAME)) {
            return;
        }
        CACHE.put(name, animation);
    }

    public static IThirdPersonAnimation getAnimation(String name) {
        return CACHE.getOrDefault(name, DEFAULT);
    }
}
