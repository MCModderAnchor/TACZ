package com.tacz.guns.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.client.model.DisplayGunModelInstance;
import com.tacz.guns.client.model.ExternalGunModel;
import org.joml.Vector3f;

/** 根据 SBM 骨骼定义构造外部视角定位矩阵，不访问 TACZ 的旧模型实例。 */
public class SbmGunTransforms {
    private SbmGunTransforms() {}

    public static void applyFixed(PoseStack pose, ExternalGunModel external, Vector3f configuredScale) {
        apply(pose, external, "fixed", configuredScale);
    }

    public static void applyGround(PoseStack pose, ExternalGunModel external, Vector3f configuredScale) {
        apply(pose, external, "ground", configuredScale);
    }

    public static void applyThirdPerson(PoseStack pose, ExternalGunModel external, Vector3f configuredScale) {
        apply(pose, external, "thirdperson_hand", configuredScale);
    }

    public static void apply(PoseStack pose, ExternalGunModel external, String anchor, Vector3f configuredScale) {
        Vector3f scale = configuredScale == null ? new Vector3f(1, 1, 1) : configuredScale;
        // BedrockGunModel 的模型空间与 SBM 烘焙空间相差一个固定的轴变换 B。
        // 将旧外部锚点语义在 SBM 坐标内表示为 B * positioning * displayScale * B。
        bedrockToSbm(pose);
        pose.translate(0, 1.5, 0);
        var bones = external.model().bones();
        int index = external.model().getIndex(anchor);
        while (index >= 0) {
            var bone = bones[index];
            var rotation = bone.bindEulerRotation();
            pose.mulPose(Axis.XP.rotation(rotation.x()));
            pose.mulPose(Axis.YP.rotation(rotation.y()));
            pose.mulPose(Axis.ZN.rotation(rotation.z()));
            if (bone.parentIndex() >= 0) {
                pose.translate(bone.bindX() * scale.x() / 16.0F,
                        bone.bindY() * scale.y() / 16.0F,
                        -bone.bindZ() * scale.z() / 16.0F);
            } else {
                pose.translate(bone.pivotX() * scale.x(), bone.pivotY() * scale.y(),
                        -bone.pivotZ() * scale.z());
            }
            index = bone.parentIndex();
        }
        if (configuredScale != null) {
            pose.scale(scale.x(), scale.y(), scale.z());
        }
        pose.translate(0, -1.5, 0);
        bedrockToSbm(pose);
    }

    public static void bedrockToSbm(PoseStack pose) {
        pose.translate(0, 1.5, 0);
        pose.scale(-1, -1, 1);
    }

    public static void applyBonePivot(PoseStack pose, DisplayGunModelInstance instance, int index) {
        instance.mulGlobalTransform(pose, index);
        var bone = instance.getBone(index).definition();
        pose.translate(bone.pivotX(), bone.pivotY(), bone.pivotZ());
    }
}
