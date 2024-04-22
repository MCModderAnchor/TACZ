package com.tac.guns.client.renderer.other;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import com.tac.guns.api.TimelessAPI;
import com.tac.guns.api.item.IGun;
import com.tac.guns.client.model.BedrockAmmoModel;
import com.tac.guns.client.model.BedrockGunModel;
import com.tac.guns.client.resource.pojo.display.gun.ShellEjection;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.concurrent.ConcurrentLinkedDeque;

public class ShellRender {
    // 抛壳队列
    private static final ConcurrentLinkedDeque<Data> SHELL_QUEUE = new ConcurrentLinkedDeque<>();

    public static void addShell(Vector3f randomVelocity) {
        double xRandom = Math.random() * randomVelocity.x();
        double yRandom = Math.random() * randomVelocity.y();
        double zRandom = Math.random() * randomVelocity.z();
        Vector3f vector3f = new Vector3f((float) xRandom, (float) yRandom, (float) zRandom);
        SHELL_QUEUE.offerLast(new Data(System.currentTimeMillis(), vector3f));
    }

    public static void render(ItemStack currentGunItem, PoseStack poseStack, BedrockGunModel gunModel) {
        IGun iGun = IGun.getIGunOrNull(currentGunItem);
        if (iGun == null) {
            return;
        }
        TimelessAPI.getClientGunIndex(iGun.getGunId(currentGunItem)).ifPresent(index -> {
            ShellEjection shellEjection = index.getShellEjection();
            if (shellEjection == null) {
                SHELL_QUEUE.clear();
                return;
            }
            TimelessAPI.getClientAmmoIndex(index.getGunData().getAmmoId()).ifPresent(ammoIndex -> {
                BedrockAmmoModel model = ammoIndex.getShellModel();
                if (model == null) {
                    return;
                }
                ResourceLocation location = ammoIndex.getShellTextureLocation();
                if (location == null) {
                    return;
                }
                long lifeTime = (long) (shellEjection.getLivingTime() * 1000);

                // 检查有没有需要踢出去的队列
                checkShellQueue(lifeTime);
                // 各种参数的获取
                Vector3f initialVelocity = shellEjection.getInitialVelocity();
                Vector3f acceleration = shellEjection.getAcceleration();
                Vector3f angularVelocity = shellEjection.getAngularVelocity();
                // 渲染抛壳
                gunModel.delegateRender((poseStack1, vertexConsumer1, transformType1, light, overlay) -> {
                    for (Data data : SHELL_QUEUE) {
                        if (data.normal == null && data.pose == null) {
                            data.normal = poseStack.last().normal().copy();
                            data.pose = poseStack.last().pose().copy();
                        }
                        PoseStack poseStack2 = new PoseStack();
                        poseStack2.last().normal().mul(data.normal);
                        poseStack2.last().pose().multiply(data.pose);
                        poseStack2.translate(-0.5, 0, -0.5);
                        long remindTime = System.currentTimeMillis() - data.timeStamp;
                        double time = remindTime / 1000.0;
                        Vector3f right = data.randomRotation;
                        double x = (initialVelocity.x() + right.x()) * time + 0.5 * acceleration.x() * time * time;
                        double y = (initialVelocity.y() + right.y()) * time + 0.5 * acceleration.y() * time * time;
                        double z = (initialVelocity.z() + right.z()) * time + 0.5 * acceleration.z() * time * time;
                        poseStack2.translate(-x, -y, z);

                        double xw = time * angularVelocity.x();
                        double yw = time * angularVelocity.y();
                        double zw = time * angularVelocity.z();
                        poseStack2.translate(0, 1.5, 0);
                        poseStack2.mulPose(Vector3f.XN.rotationDegrees((float) xw));
                        poseStack2.mulPose(Vector3f.YN.rotationDegrees((float) yw));
                        poseStack2.mulPose(Vector3f.ZP.rotationDegrees((float) zw));
                        poseStack2.translate(0, -1.5, 0);

                        model.render(poseStack2, transformType1, RenderType.itemEntityTranslucentCull(location), light, overlay);
                    }
                });
            });
        });
    }

    private static void checkShellQueue(long lifeTime) {
        if (!SHELL_QUEUE.isEmpty()) {
            Data data = SHELL_QUEUE.peekFirst();
            if ((System.currentTimeMillis() - data.timeStamp) > lifeTime) {
                SHELL_QUEUE.pollFirst();
                checkShellQueue(lifeTime);
            }
        }
    }


    public static class Data {
        public final long timeStamp;
        public final Vector3f randomRotation;

        public Matrix3f normal = null;
        public Matrix4f pose = null;

        public Data(long timeStamp, Vector3f randomRotation) {
            this.timeStamp = timeStamp;
            this.randomRotation = randomRotation;
        }
    }
}
