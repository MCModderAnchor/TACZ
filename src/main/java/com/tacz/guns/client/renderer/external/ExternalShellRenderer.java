package com.tacz.guns.client.renderer.external;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.client.model.BedrockAmmoModel;
import com.tacz.guns.client.model.DisplayGunModelInstance;
import com.tacz.guns.client.renderer.item.SbmGunTransforms;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.pojo.display.gun.ShellEjection;
import com.tacz.guns.compat.oculus.OculusCompat;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.util.time.GunTime;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.concurrent.ConcurrentLinkedDeque;

public final class ExternalShellRenderer {
    private static final int MAX_SHELLS = 128;
    private final ConcurrentLinkedDeque<Shell> shells = new ConcurrentLinkedDeque<>();

    public void addShell(int origin, Vector3f randomVelocity) {
        if (origin < 0) return;
        if (shells.size() >= MAX_SHELLS) shells.pollFirst();
        var randomOffset = new Vector3f((float) (Math.random() * randomVelocity.x()),
                (float) (Math.random() * randomVelocity.y()), (float) (Math.random() * randomVelocity.z()));
        shells.offerLast(new Shell(origin, randomOffset));
    }

    public void render(GunDisplayInstance display, GunData gunData, DisplayGunModelInstance instance,
                       PoseStack modelPose, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        if (OculusCompat.isRenderShadow()) return;
        ShellEjection ejection = display.getShellEjection();
        if (ejection == null) {
            shells.clear();
            return;
        }
        long now = GunTime.nowMillis();
        long epoch = GunTime.epoch();
        long lifetime = (long) (ejection.getLivingTime() * 1000);
        shells.removeIf(shell -> shell.epoch != epoch || now - shell.timestamp > lifetime);
        if (shells.isEmpty()) return;

        BedrockAmmoModel model = display.getShellModel();
        ResourceLocation texture = display.getShellTextureLocation();
        if (model == null || texture == null) {
            var ammoIndex = TimelessAPI.getClientAmmoIndex(gunData.getAmmoId()).orElse(null);
            if (ammoIndex == null) return;
            model = ammoIndex.getShellModel();
            texture = ammoIndex.getShellTextureLocation();
        }
        if (model == null || texture == null) return;

        int[] origins = instance.shellOriginBones();
        for (Shell shell : shells) {
            if (shell.pose == null) {
                if (shell.origin >= origins.length) continue;
                modelPose.pushPose();
                SbmGunTransforms.applyBonePivot(modelPose, instance, origins[shell.origin]);
                // 弹壳资源与运动参数使用基岩模型坐标轴。
                modelPose.scale(-1, -1, 1);
                shell.pose = new Matrix4f(modelPose.last().pose());
                shell.normal = new Matrix3f(modelPose.last().normal());
                modelPose.popPose();
            }
            renderShell(shell, now, ejection, model, texture, buffers, packedLight, packedOverlay);
        }
    }

    private static void renderShell(Shell shell, long now, ShellEjection ejection, BedrockAmmoModel model,
                                    ResourceLocation texture, MultiBufferSource buffers,
                                    int packedLight, int packedOverlay) {
        PoseStack pose = new PoseStack();
        pose.last().pose().set(shell.pose);
        pose.last().normal().set(shell.normal);
        double time = (now - shell.timestamp) / 1000.0;
        Vector3f velocity = ejection.getInitialVelocity();
        Vector3f acceleration = ejection.getAcceleration();
        Vector3f angularVelocity = ejection.getAngularVelocity();
        double x = (velocity.x() + shell.randomOffset.x()) * time + 0.5 * acceleration.x() * time * time;
        double y = (velocity.y() + shell.randomOffset.y()) * time + 0.5 * acceleration.y() * time * time;
        double z = (velocity.z() + shell.randomOffset.z()) * time + 0.5 * acceleration.z() * time * time;
        pose.translate(-x, -y, z);
        pose.mulPose(Axis.XN.rotationDegrees((float) (time * angularVelocity.x())));
        pose.mulPose(Axis.YN.rotationDegrees((float) (time * angularVelocity.y())));
        pose.mulPose(Axis.ZP.rotationDegrees((float) (time * angularVelocity.z())));
        pose.translate(0, -1.5, 0);
        model.render(pose, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, RenderType.entityCutout(texture),
                packedLight, packedOverlay, buffers);
    }

    private static final class Shell {
        final int origin;
        final long timestamp = GunTime.nowMillis();
        final long epoch = GunTime.epoch();
        final Vector3f randomOffset;
        Matrix4f pose;
        Matrix3f normal;

        Shell(int origin, Vector3f randomOffset) {
            this.origin = origin;
            this.randomOffset = randomOffset;
        }
    }
}
