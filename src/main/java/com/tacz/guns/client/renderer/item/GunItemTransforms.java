package com.tacz.guns.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.client.resource.pojo.TransformScale;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Vector3f;

import java.util.List;

/**
 * 现有 TACZ 模型的外部视角局部变换，仅供旧渲染路径使用。
 * 输入仍处于 ItemRenderer 调用自定义物品渲染器时的坐标空间；不包含槽位或相机变换。
 * SBM 模型有独立坐标约定，不能将本类的翻转矩阵直接套到 SBM 顶点上。
 */
public final class GunItemTransforms {
    private GunItemTransforms() {}

    public static void apply(ItemDisplayContext context, TransformScale scale, BedrockGunModel model, PoseStack pose) {
        pose.translate(0.5, 2, 0.5);
        pose.scale(-1, -1, 1);
        switch (context) {
            case FIXED -> applyPositioningNode(model.getFixedOriginPath(), pose, scale.getFixed());
            case GROUND -> applyPositioningNode(model.getGroundOriginPath(), pose, scale.getGround());
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND ->
                    applyPositioningNode(model.getThirdPersonHandOriginPath(), pose, scale.getThirdPerson());
        }
        applyScale(context, scale, pose);
    }

    private static void applyScale(ItemDisplayContext context, TransformScale scale, PoseStack pose) {
        if (scale == null) return;
        Vector3f value = switch (context) {
            case FIXED -> scale.getFixed();
            case GROUND -> scale.getGround();
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND -> scale.getThirdPerson();
            default -> null;
        };
        if (value != null) {
            pose.translate(0, 1.5, 0);
            pose.scale(value.x(), value.y(), value.z());
            pose.translate(0, -1.5, 0);
        }
    }

    private static void applyPositioningNode(List<BedrockPart> path, PoseStack pose, Vector3f scale) {
        if (path == null) return;
        if (scale == null) scale = new Vector3f(1, 1, 1);
        pose.translate(0, 1.5, 0);
        for (int i = path.size() - 1; i >= 0; i--) {
            BedrockPart part = path.get(i);
            pose.mulPose(Axis.XN.rotation(part.xRot));
            pose.mulPose(Axis.YN.rotation(part.yRot));
            pose.mulPose(Axis.ZN.rotation(part.zRot));
            if (part.getParent() != null) {
                pose.translate(-part.x * scale.x() / 16.0F, -part.y * scale.y() / 16.0F, -part.z * scale.z() / 16.0F);
            } else {
                pose.translate(-part.x * scale.x() / 16.0F, (1.5F - part.y / 16.0F) * scale.y(), -part.z * scale.z() / 16.0F);
            }
        }
        pose.translate(0, -1.5, 0);
    }
}
