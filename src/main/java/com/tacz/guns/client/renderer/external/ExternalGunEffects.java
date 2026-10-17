package com.tacz.guns.client.renderer.external;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.model.ExternalGunModel;
import com.tacz.guns.client.model.functional.BeamRenderer;
import com.tacz.guns.client.renderer.block.DisplayGunGeometry;
import com.tacz.guns.client.renderer.item.GunItemRendererWrapper;
import com.tacz.guns.client.renderer.item.SbmGunTransforms;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class ExternalGunEffects {
    private ExternalGunEffects() {}

    public static void renderThirdPerson(ExternalGunAppearance.Snapshot snapshot, PoseStack pose,
                                         MultiBufferSource buffers) {
        ItemStack stack = snapshot.stack();
        IGun gun = IGun.getIGunOrNull(stack);
        if (gun == null) return;
        var attachments = DisplayGunGeometry.resolveAttachments(gun, stack);
        if (attachments == null) return;
        var instance = snapshot.model().displayInstance();
        instance.configureGun(gun, stack, attachments);

        pose.pushPose();
        DisplayGunGeometry.applyTransform(snapshot, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, true, pose);
        renderLasers(snapshot.model(), stack, pose, buffers);
        DisplayGunGeometry.forEachAttachmentModel(instance, pose, snapshot.useLod(), gun, stack, attachments,
                (model, texture, attached, modelPose) -> renderLasers(model, attached, modelPose, buffers));

        int muzzle = instance.muzzleFlashPosition();
        if (muzzle >= 0) {
            SbmGunTransforms.applyBonePivot(pose, instance, muzzle);
            pose.scale(-1, -1, 1);
            GunItemRendererWrapper.captureThirdPersonMuzzle(pose.last().pose());
        }
        pose.popPose();
    }

    private static void renderLasers(ExternalGunModel model, ItemStack stack, PoseStack pose,
                                     MultiBufferSource buffers) {
        var instance = model.displayInstance();
        for (int laser : instance.laserBeamBones()) {
            pose.pushPose();
            SbmGunTransforms.applyBonePivot(pose, instance, laser);
            pose.scale(-1, -1, 1);
            BeamRenderer.renderLaserBeamUnaccelerated(stack, pose, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, buffers);
            pose.popPose();
        }
    }
}
