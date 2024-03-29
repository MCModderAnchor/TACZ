package com.tac.guns.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import com.tac.guns.GunMod;
import com.tac.guns.api.TimelessAPI;
import com.tac.guns.api.attachment.AttachmentType;
import com.tac.guns.api.client.event.RenderItemInHandBobEvent;
import com.tac.guns.api.client.player.IClientPlayerGunOperator;
import com.tac.guns.api.event.GunShootEvent;
import com.tac.guns.api.item.IAttachment;
import com.tac.guns.api.item.IGun;
import com.tac.guns.client.animation.internal.GunAnimationStateMachine;
import com.tac.guns.client.gui.GunRefitScreen;
import com.tac.guns.client.model.BedrockAmmoModel;
import com.tac.guns.client.model.BedrockAttachmentModel;
import com.tac.guns.client.model.BedrockGunModel;
import com.tac.guns.client.model.bedrock.BedrockPart;
import com.tac.guns.client.renderer.item.GunItemRenderer;
import com.tac.guns.client.resource.index.ClientAttachmentIndex;
import com.tac.guns.client.resource.index.ClientGunIndex;
import com.tac.guns.client.resource.pojo.CommonTransformObject;
import com.tac.guns.client.resource.pojo.display.gun.ShellEjection;
import com.tac.guns.duck.KeepingItemRenderer;
import com.tac.guns.resource.DefaultAssets;
import com.tac.guns.util.math.Easing;
import com.tac.guns.util.math.MathUtil;
import com.tac.guns.util.math.PerlinNoise;
import com.tac.guns.util.math.SecondOrderDynamics;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransforms.TransformType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedDeque;

import static net.minecraft.client.renderer.block.model.ItemTransforms.TransformType.FIRST_PERSON_RIGHT_HAND;

/**
 * 负责第一人称的枪械模型渲染。其他人称参见 {@link GunItemRenderer}
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GunMod.MOD_ID)
public class FirstPersonRenderGunEvent {
    // 用于生成瞄准动作的运动曲线，使动作看起来更平滑
    private static final SecondOrderDynamics AIMING_DYNAMICS = new SecondOrderDynamics(1.2f, 1.2f, 0.5f, 0);
    // 用于打开改装界面时枪械运动的平滑
    private static final SecondOrderDynamics REFIT_OPENING_DYNAMICS = new SecondOrderDynamics(1f, 1.2f, 0.5f, 0);
    // 用于枪械后座的程序动画
    private static final PerlinNoise SHOOT_X_SWAY_NOISE = new PerlinNoise(-0.2f, 0.2f, 400);
    private static final PerlinNoise SHOOT_Y_ROTATION_NOISE = new PerlinNoise(-0.0136f, 0.0136f, 100);
    private static final float SHOOT_Y_SWAY = -0.1f;
    private static final float SHOOT_Z_SWAY = 0.2f;
    private static final float SHOOT_ANIMATION_TIME = 0.3f;
    private static long shootTimeStamp = -1;
    // 抛壳队列
    private static final ConcurrentLinkedDeque<Pair<Long, Vector3f>> SHELL_QUEUE = new ConcurrentLinkedDeque<>();

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        // TODO 先默认只实现主手的渲染
        if (event.getHand() == InteractionHand.OFF_HAND) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof IGun iGun)) {
            return;
        }

        // 获取 TransformType
        TransformType transformType;
        if (event.getHand() == InteractionHand.MAIN_HAND) {
            transformType = FIRST_PERSON_RIGHT_HAND;
        } else {
            transformType = TransformType.FIRST_PERSON_LEFT_HAND;
        }

        ResourceLocation gunId = iGun.getGunId(stack);
        TimelessAPI.getClientGunIndex(gunId).ifPresent(gunIndex -> {
            BedrockGunModel gunModel = gunIndex.getGunModel();
            GunAnimationStateMachine animationStateMachine = gunIndex.getAnimationStateMachine();
            if (gunModel == null) {
                return;
            }
            // 在渲染之前，先更新动画，让动画数据写入模型
            if (animationStateMachine != null) {
                animationStateMachine.update(event.getPartialTicks(), player);
            }
            PoseStack poseStack = event.getPoseStack();
            poseStack.pushPose();
            // 逆转原版施加在手上的延滞效果，改为写入模型动画数据中
            float f2 = Mth.lerp(event.getPartialTicks(), player.xBobO, player.xBob);
            float f3 = Mth.lerp(event.getPartialTicks(), player.yBobO, player.yBob);
            float xRot = player.getViewXRot(event.getPartialTicks()) - f2;
            float yRot = player.getViewYRot(event.getPartialTicks()) - f3;
            poseStack.mulPose(Vector3f.XP.rotationDegrees(xRot * -0.1F));
            poseStack.mulPose(Vector3f.YP.rotationDegrees(yRot * -0.1F));
            BedrockPart rootNode = gunModel.getRootNode();
            if (rootNode != null) {
                rootNode.offsetX += yRot * 0.1F / 16F / 3F;
                rootNode.offsetY += -xRot * 0.1F / 16F / 3F;
                rootNode.additionalQuaternion.mul(Vector3f.XP.rotationDegrees(xRot * 0.05F));
                rootNode.additionalQuaternion.mul(Vector3f.YP.rotationDegrees(yRot * 0.05F));
            }
            // 从渲染原点 (0, 24, 0) 移动到模型原点 (0, 0, 0)
            poseStack.translate(0, 1.5f, 0);
            // 基岩版模型是上下颠倒的，需要翻转过来。
            poseStack.mulPose(Vector3f.ZP.rotationDegrees(180f));
            // 应用持枪姿态变换，如第一人称摄像机定位
            applyFirstPersonGunTransform(player, stack, gunIndex, poseStack, gunModel, event.getPartialTicks());
            // 如果正在打开改装界面，则取消手臂渲染
            boolean renderHand = gunModel.getRenderHand();
            if (GunRefitScreen.getOpeningProgress() != 0) {
                gunModel.setRenderHand(false);
            }
            // 调用枪械模型渲染
            RenderType renderType = RenderType.itemEntityTranslucentCull(gunIndex.getModelTexture());
            gunModel.render(poseStack, stack, transformType, renderType, event.getPackedLight(), OverlayTexture.NO_OVERLAY);
            // 渲染抛壳
            renderShell(gunIndex, gunModel, poseStack, transformType, event.getPackedLight());
            // 恢复手臂渲染
            gunModel.setRenderHand(renderHand);
            // 渲染完成后，将动画数据从模型中清除，不对其他视角下的模型渲染产生影响
            poseStack.popPose();
            gunModel.cleanAnimationTransform();
            // 放这里，只有渲染了枪械，才取消后续（虽然一般来说也没有什么后续了）
            event.setCanceled(true);
        });
    }

    private static void checkShellQueue(long lifeTime) {
        if (!SHELL_QUEUE.isEmpty()) {
            long first = SHELL_QUEUE.peekFirst().left();
            if ((System.currentTimeMillis() - first) > lifeTime) {
                SHELL_QUEUE.pollFirst();
                checkShellQueue(lifeTime);
            }
        }
    }

    private static void renderShell(ClientGunIndex gunIndex, BedrockGunModel gunModel, PoseStack poseStack, TransformType transformType, int packLight) {
        ShellEjection shellEjection = gunIndex.getShellEjection();
        if (shellEjection == null) {
            SHELL_QUEUE.clear();
            return;
        }
        TimelessAPI.getClientAmmoIndex(DefaultAssets.DEFAULT_AMMO_ID).ifPresent(ammoIndex -> {
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
            // 将原点放置在抛壳定位点处
            applyPositioningNodeTransform(gunModel.getShellOriginPath(), poseStack);
            // 各种参数的获取
            Vector3f initialVelocity = shellEjection.getInitialVelocity();
            Vector3f acceleration = shellEjection.getAcceleration();
            Vector3f angularVelocity = shellEjection.getAngularVelocity();
            // 渲染抛壳
            for (Pair<Long, Vector3f> data : SHELL_QUEUE) {
                poseStack.pushPose();

                long remindTime = System.currentTimeMillis() - data.left();
                double time = remindTime / 1000.0;
                Vector3f right = data.right();
                double x = (initialVelocity.x() + right.x()) * time + 0.5 * acceleration.x() * time * time;
                double y = (initialVelocity.y() + right.y()) * time + 0.5 * acceleration.y() * time * time;
                double z = (initialVelocity.z() + right.z()) * time + 0.5 * acceleration.z() * time * time;
                poseStack.translate(-x, -y, z);

                double xw = time * angularVelocity.x();
                double yw = time * angularVelocity.y();
                double zw = time * angularVelocity.z();
                poseStack.translate(0, 1.5, 0);
                poseStack.mulPose(Vector3f.XN.rotationDegrees((float) xw));
                poseStack.mulPose(Vector3f.YN.rotationDegrees((float) yw));
                poseStack.mulPose(Vector3f.ZP.rotationDegrees((float) zw));
                poseStack.translate(0, -1.5, 0);

                model.render(poseStack, transformType, RenderType.itemEntityTranslucentCull(location), packLight, OverlayTexture.NO_OVERLAY);

                poseStack.popPose();
            }
        });
    }

    /**
     * 当主手拿着枪械物品的时候，取消应用在它上面的 viewBobbing，以便应用自定义的跑步/走路动画。
     */
    @SubscribeEvent
    public static void cancelItemInHandViewBobbing(RenderItemInHandBobEvent.BobView event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        ItemStack itemStack = ((KeepingItemRenderer)Minecraft.getInstance().getItemInHandRenderer()).getCurrentGunItem();
        if (IGun.getIGunOrNull(itemStack) != null) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onGunFire(GunShootEvent event) {
        if (event.getLogicalSide().isClient()) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }
            ItemStack mainhandItem = player.getMainHandItem();
            IGun iGun = IGun.getIGunOrNull(mainhandItem);
            if (iGun == null) {
                return;
            }
            TimelessAPI.getClientGunIndex(iGun.getGunId(mainhandItem)).ifPresent(gunIndex -> {
                // 抛壳
                if (gunIndex.getShellEjection() != null) {
                    addShell(gunIndex.getShellEjection().getRandomVelocity());
                }
                // 记录开火时间戳，用于后坐力程序动画
                shootTimeStamp = System.currentTimeMillis();
            });
        }
    }

    private static void applyFirstPersonGunTransform(LocalPlayer player, ItemStack gunItemStack, ClientGunIndex gunIndex,
                                                     PoseStack poseStack, BedrockGunModel model, float partialTicks) {
        // 配合运动曲线，计算改装枪口的打开进度
        float refitScreenOpeningProgress = REFIT_OPENING_DYNAMICS.update(GunRefitScreen.getOpeningProgress());
        // 配合运动曲线，计算瞄准进度
        float aimingProgress = AIMING_DYNAMICS.update(IClientPlayerGunOperator.fromLocalPlayer(player).getClientAimingProgress(partialTicks));
        // 应用枪械动态，如后坐力
        applyGunMovements(model, aimingProgress, partialTicks);
        // 应用各种摄像机定位组的变换（默认持枪、瞄准、改装界面等）
        applyFirstPersonPositioningTransform(poseStack, model, gunItemStack, aimingProgress, refitScreenOpeningProgress);
        // 应用动画约束变换
        CommonTransformObject ica = gunIndex.getAnimationInfluenceCoefficient().getIronView();
        applyAnimationConstraintTransform(poseStack, model.getConstraintPath(), aimingProgress * (1 - refitScreenOpeningProgress), ica);
    }

    private static void applyGunMovements(BedrockGunModel model, float aimingProgress, float partialTicks) {
        applyShootSwayAndRotation(model, aimingProgress);
    }

    /**
     * 应用瞄具摄像机定位组、机瞄摄像机定位组和 Idle 摄像机定位组的变换。会在几个摄像机定位之间插值。
     */
    private static void applyFirstPersonPositioningTransform(PoseStack poseStack, BedrockGunModel model, ItemStack stack, float aimingProgress,
                                                             float refitScreenOpeningProgress) {
        IGun iGun = IGun.getIGunOrNull(stack);
        if (iGun == null) {
            return;
        }
        Matrix4f transformMatrix = new Matrix4f();
        transformMatrix.setIdentity();
        // 应用瞄准定位
        List<BedrockPart> idleNodePath = model.getIdleSightPath();
        List<BedrockPart> aimingNodePath = null;
        ItemStack scopeItem = iGun.getAttachment(stack, AttachmentType.SCOPE);
        if (scopeItem.isEmpty()) {
            // 未安装瞄具，使用机瞄定位组
            aimingNodePath = model.getIronSightPath();
        } else {
            // 安装瞄具，组合瞄具定位组和瞄具视野定位组
            List<BedrockPart> scopeNodePath = model.getScopePosPath();
            if (scopeNodePath != null) {
                aimingNodePath = new ArrayList<>(scopeNodePath);
                IAttachment iAttachment = IAttachment.getIAttachmentOrNull(scopeItem);
                if (iAttachment != null) {
                    ResourceLocation scopeId = iAttachment.getAttachmentId(scopeItem);
                    Optional<ClientAttachmentIndex> indexOptional = TimelessAPI.getClientAttachmentIndex(scopeId);
                    if (indexOptional.isPresent()) {
                        BedrockAttachmentModel attachmentModel = indexOptional.get().getAttachmentModel();
                        if (attachmentModel.getScopeViewPath() != null) {
                            aimingNodePath.addAll(attachmentModel.getScopeViewPath());
                        }
                    }
                }
            }
        }
        MathUtil.applyMatrixLerp(transformMatrix, getPositioningNodeInverse(idleNodePath), transformMatrix, (1 - refitScreenOpeningProgress));
        MathUtil.applyMatrixLerp(transformMatrix, getPositioningNodeInverse(aimingNodePath), transformMatrix, (1 - refitScreenOpeningProgress) * aimingProgress);
        // 应用改装界面开启时的定位
        float refitTransformProgress = (float) Easing.easeOutCubic(GunRefitScreen.getTransformProgress());
        AttachmentType oldType = GunRefitScreen.getOldTransformType();
        AttachmentType currentType = GunRefitScreen.getCurrentTransformType();
        List<BedrockPart> fromNode = model.getRefitAttachmentViewPath(oldType);
        List<BedrockPart> toNode = model.getRefitAttachmentViewPath(currentType);
        MathUtil.applyMatrixLerp(transformMatrix, getPositioningNodeInverse(fromNode), transformMatrix, refitScreenOpeningProgress);
        MathUtil.applyMatrixLerp(transformMatrix, getPositioningNodeInverse(toNode), transformMatrix, refitScreenOpeningProgress * refitTransformProgress);
        // 应用变换到 PoseStack
        poseStack.translate(0, 1.5f, 0);
        poseStack.mulPoseMatrix(transformMatrix);
        poseStack.translate(0, -1.5f, 0);
    }

    /**
     * 获取摄像机定位组的反相矩阵
     */
    @Nonnull
    private static Matrix4f getPositioningNodeInverse(List<BedrockPart> nodePath) {
        Matrix4f matrix4f = new Matrix4f();
        matrix4f.setIdentity();
        if (nodePath != null) {
            for (int i = nodePath.size() - 1; i >= 0; i--) {
                BedrockPart part = nodePath.get(i);
                // 计算反向的旋转
                matrix4f.multiply(Vector3f.XN.rotation(part.xRot));
                matrix4f.multiply(Vector3f.YN.rotation(part.yRot));
                matrix4f.multiply(Vector3f.ZN.rotation(part.zRot));
                // 计算反向的位移
                if (part.getParent() != null) {
                    matrix4f.multiplyWithTranslation(-part.x / 16.0F, -part.y / 16.0F, -part.z / 16.0F);
                } else {
                    matrix4f.multiplyWithTranslation(-part.x / 16.0F, (1.5F - part.y / 16.0F), -part.z / 16.0F);
                }
            }
        }
        return matrix4f;
    }

    private static void applyShootSwayAndRotation(BedrockGunModel model, float aimingProgress) {
        BedrockPart rootNode = model.getRootNode();
        if (rootNode != null) {
            float progress = 1 - (System.currentTimeMillis() - shootTimeStamp) / (SHOOT_ANIMATION_TIME * 1000);
            if (progress < 0) {
                progress = 0;
            }
            progress = (float) Easing.easeOutCubic(progress);
            rootNode.offsetX += SHOOT_X_SWAY_NOISE.getValue() / 16 * progress * (1 - aimingProgress);
            rootNode.offsetY += SHOOT_Y_SWAY / 16 * progress * (1 - aimingProgress);
            rootNode.additionalQuaternion.mul(Vector3f.YP.rotation(SHOOT_Y_ROTATION_NOISE.getValue() * progress));
        }
    }

    /**
     * 获取动画约束点的变换数据。
     *
     * @param originTranslation   用于输出约束点的原坐标
     * @param animatedTranslation 用于输出约束点经过动画变换之后的坐标
     * @param rotation            用于输出约束点的旋转
     */
    private static void getAnimationConstraintTransform(List<BedrockPart> nodePath, @Nonnull Vector3f originTranslation, @Nonnull Vector3f animatedTranslation,
                                                        @Nonnull Vector3f rotation) {
        if (nodePath == null) {
            return;
        }
        // 约束点动画变换矩阵
        Matrix4f animeMatrix = new Matrix4f();
        // 约束点初始变换矩阵
        Matrix4f originMatrix = new Matrix4f();
        animeMatrix.setIdentity();
        originMatrix.setIdentity();
        for (BedrockPart part : nodePath) {
            // 乘动画位移
            animeMatrix.multiplyWithTranslation(part.offsetX, part.offsetY, part.offsetZ);
            // 乘组位移
            if (part.getParent() != null) {
                animeMatrix.multiplyWithTranslation(part.x / 16.0F, part.y / 16.0F, part.z / 16.0F);
            } else {
                animeMatrix.multiplyWithTranslation(part.x / 16.0F, (part.y / 16.0F - 1.5F), part.z / 16.0F);
            }
            // 乘动画旋转
            animeMatrix.multiply(part.additionalQuaternion);
            // 乘组旋转
            animeMatrix.multiply(Vector3f.ZP.rotation(part.zRot));
            animeMatrix.multiply(Vector3f.YP.rotation(part.yRot));
            animeMatrix.multiply(Vector3f.XP.rotation(part.xRot));

            // 乘组位移
            if (part.getParent() != null) {
                originMatrix.multiplyWithTranslation(
                        part.x / 16.0F,
                        part.y / 16.0F,
                        part.z / 16.0F
                );
            } else {
                originMatrix.multiplyWithTranslation(
                        part.x / 16.0F,
                        (part.y / 16.0F - 1.5F),
                        part.z / 16.0F
                );
            }
            // 乘组旋转
            originMatrix.multiply(Vector3f.ZP.rotation(part.zRot));
            originMatrix.multiply(Vector3f.YP.rotation(part.yRot));
            originMatrix.multiply(Vector3f.XP.rotation(part.xRot));

        }
        // 把变换数据写入输出
        animatedTranslation.set(animeMatrix.m03, animeMatrix.m13, animeMatrix.m23);
        originTranslation.set(originMatrix.m03, originMatrix.m13, originMatrix.m23);
        Vector3f animatedRotation = MathUtil.getEulerAngles(animeMatrix);
        Vector3f originRotation = MathUtil.getEulerAngles(originMatrix);
        animatedRotation.sub(originRotation);
        rotation.set(animatedRotation.x(), animatedRotation.y(), animatedRotation.z());
    }

    /**
     * 应用动画约束变换。
     *
     * @param multiplier 旋转、位移各轴的动画权重，范围皆为 0~1，0 则完全不受动画影响，1 则完全受到动画控制。
     * @param weight     控制约束变换的权重，用于插值。
     */
    public static void applyAnimationConstraintTransform(PoseStack poseStack, List<BedrockPart> nodePath, float weight,
                                                         CommonTransformObject multiplier) {
        // TODO 判断是否安装瞄具，
        // 获取动画约束点的变换信息
        Vector3f originTranslation = new Vector3f();
        Vector3f animatedTranslation = new Vector3f();
        Vector3f rotation = new Vector3f();
        getAnimationConstraintTransform(nodePath, originTranslation, animatedTranslation, rotation);
        // 配合约束系数，计算约束位移需要的反向位移
        Vector3f inverseTranslation = originTranslation.copy();
        inverseTranslation.sub(animatedTranslation);
        inverseTranslation.mul(1 - multiplier.getTranslation().x(), 1 - multiplier.getTranslation().y(), 1 - multiplier.getTranslation().z());
        // 计算约束旋转需要的反向旋转。因需要插值，获取的是欧拉角
        Vector3f inverseRotation = rotation.copy();
        inverseRotation.mul(multiplier.getRotation().x() - 1, multiplier.getRotation().y() - 1, multiplier.getRotation().z() - 1);
        // 约束旋转
        poseStack.translate(animatedTranslation.x(), animatedTranslation.y() + 1.5f, animatedTranslation.z());
        poseStack.mulPose(Vector3f.XP.rotation(inverseRotation.x() * weight));
        poseStack.mulPose(Vector3f.YP.rotation(inverseRotation.y() * weight));
        poseStack.mulPose(Vector3f.ZP.rotation(inverseRotation.z() * weight));
        poseStack.translate(-animatedTranslation.x(), -animatedTranslation.y() - 1.5f, -animatedTranslation.z());
        // 约束位移
        poseStack.last().pose().translate(new Vector3f(
                -inverseTranslation.x() * weight, -inverseTranslation.y() * weight, inverseTranslation.z() * weight
        ));
    }

    private static void addShell(Vector3f randomVelocity) {
        double xRandom = Math.random() * randomVelocity.x();
        double yRandom = Math.random() * randomVelocity.y();
        double zRandom = Math.random() * randomVelocity.z();
        Vector3f vector3f = new Vector3f((float) xRandom, (float) yRandom, (float) zRandom);
        SHELL_QUEUE.offerLast(Pair.of(System.currentTimeMillis(), vector3f));
    }

    private static void applyPositioningNodeTransform(List<BedrockPart> nodePath, PoseStack poseStack) {
        if (nodePath == null) {
            return;
        }
        // 抛壳原点为定位组
        for (int i = nodePath.size() - 1; i >= 0; i--) {
            BedrockPart t = nodePath.get(i);
            if (t.getParent() != null) {
                poseStack.translate(t.x / 16.0F, t.y / 16.0F, t.z / 16.0F);
            } else {
                poseStack.translate(t.x / 16.0F, (t.y / 16.0F - 1.5), t.z / 16.0F);
            }
        }
    }
}
