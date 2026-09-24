package com.tacz.guns.client.renderer.item;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.handler.FirstPersonRenderHandler;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.resource.pojo.ParticleEffectData;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.firstperson.FirstPersonParticleSystem;
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.render.CameraStateCache;
import com.github.mcmodderanchor.simplebedrockmodel.v2.client.world.ImmediateStaticMeshRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.animation.statemachine.LuaAnimationStateMachine;
import com.tacz.guns.api.client.event.BeforeRenderHandEvent;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.animation.screen.RefitTransform;
import com.tacz.guns.client.animation.statemachine.GunAnimationConstant;
import com.tacz.guns.client.animation.statemachine.GunAnimationStateContext;
import com.tacz.guns.client.event.CameraSetupEvent;
import com.tacz.guns.client.event.FirstPersonRenderGunEvent;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.IFunctionalRenderer;
import com.tacz.guns.client.model.SlotModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.client.model.functional.MuzzleFlashRender;
import com.tacz.guns.client.model.functional.ShellRender;
import com.tacz.guns.client.particle.MuzzleParticleManager;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.util.RenderDistance;
import com.tacz.guns.util.math.MathUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ViewportEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

import static com.tacz.guns.client.particle.ThirdPersonMuzzleParticleManager.currentEntity;
import static com.tacz.guns.client.particle.ThirdPersonMuzzleParticleManager.onMuzzleRendered;
import static net.minecraft.world.item.ItemDisplayContext.*;

/**
 * 负责主要的枪械动画模型渲染。额外的效果见 {@link com.tacz.guns.client.event.FirstPersonRenderGunEvent}
 */
public class GunItemRendererWrapper extends AnimateGeoItemRenderer<BedrockGunModel, GunAnimationStateContext> {
    private static final SlotModel SLOT_GUN_MODEL = new SlotModel();
    private static BedrockGunModel lastModel = null;
    public static final Vector3f muzzleRenderOffset = new Vector3f();

    public GunItemRendererWrapper() {
        super();
    }

    @Override
    public GunAnimationStateContext initContext(ItemStack stack, Player player, float partialTick) {
        GunAnimationStateContext context = new GunAnimationStateContext();
        this.updateContext(context, stack, player, partialTick);
        return context;
    }

    @Override
    public void updateContext(GunAnimationStateContext context, ItemStack stack, Player player, float partialTick) {
        context.setPartialTicks(partialTick);
        context.setCurrentGunItem(stack);
    }

    @Override
    public void tryInit(ItemStack stack, Player player, float partialTick) {
        super.tryInit(stack, player, partialTick);
    }

    @Override
    public void tryExit(ItemStack stack, long putAwayTime) {
        var stateMachine = getStateMachine(stack);
        if (stateMachine == null) {
            return;
        }
        stateMachine.processContextIfExist(context -> {
            context.setPutAwayTime(putAwayTime / 1000F);
            context.setCurrentGunItem(stack);
        });
        if(stateMachine.isInitialized()) {
            stateMachine.trigger(GunAnimationConstant.INPUT_PUT_AWAY);
//            KeepingItemRenderer.getRenderer().keep(stack, putAwayTime);
            stateMachine.exit();
            stateMachine.setExitingTime(putAwayTime + 50);
        }
    }

    @Override
    public long getPutAwayTime(ItemStack stack) {
        if (stack.getItem() instanceof IGun iGun) {
            return TimelessAPI.getCommonGunIndex(iGun.getGunId(stack))
                    .map(index -> (long) (index.getGunData().getPutAwayTime() * 1000L))
                    .orElse(0L);
        }
        return 0;
    }

    @Nullable
    @Override
    public LuaAnimationStateMachine<GunAnimationStateContext> getStateMachine(ItemStack stack) {
        return TimelessAPI.getGunDisplay(stack).map(GunDisplayInstance::getAnimationStateMachine).orElse(null);
    }

    @Override
    public BedrockGunModel getModel(ItemStack stack) {
        return TimelessAPI.getGunDisplay(stack).map(GunDisplayInstance::getGunModel).orElse(null);
    }

    @Override
    public ResourceLocation getTextureLocation(ItemStack stack) {
        return TimelessAPI.getGunDisplay(stack).map(GunDisplayInstance::getModelTexture).orElse(null);
    }

    @Override
    public void applyLevelCameraAnimation(ViewportEvent.ComputeCameraAngles event, ItemStack stack, LocalPlayer player) {
        if (!(stack.getItem() instanceof IGun iGun)) {
            return;
        }
        Optional.ofNullable(getModel(stack)).ifPresent(model -> {
            if (lastModel != model) {
                // 切换枪械模型的时候清理一下摄像机动画数据，以避免上一次播放到一半的摄像机动画影响观感。
                model.cleanCameraAnimationTransform();
                lastModel = model;
            }
            IClientPlayerGunOperator clientPlayerGunOperator = IClientPlayerGunOperator.fromLocalPlayer(player);
            float partialTicks = Minecraft.getInstance().getFrameTime();
            float aimingProgress = clientPlayerGunOperator.getClientAimingProgress(partialTicks);
            float zoom = iGun.getAimingZoom(stack);
            float multiplier = 1 - aimingProgress + aimingProgress / (float) Math.sqrt(zoom);
            this.applyLevelCameraAnimation(event, stack, multiplier);
        });
    }

    @Override
    public void applyItemInHandCameraAnimation(BeforeRenderHandEvent event, ItemStack stack, LocalPlayer player) {
        if (!(stack.getItem() instanceof IGun iGun)) {
            return;
        }
        Optional.ofNullable(getModel(stack)).ifPresent(model -> {
            PoseStack poseStack = event.getPoseStack();
            IClientPlayerGunOperator clientPlayerGunOperator = IClientPlayerGunOperator.fromLocalPlayer(player);
            float partialTicks = Minecraft.getInstance().getFrameTime();
            float aimingProgress = clientPlayerGunOperator.getClientAimingProgress(partialTicks);
            float zoom = iGun.getAimingZoom(stack);
            float multiplier = 1 - aimingProgress + aimingProgress / (float) Math.sqrt(zoom);
            Quaternionf quaternion = MathUtil.multiplyQuaternion(model.getCameraAnimationObject().rotationQuaternion, multiplier);
            poseStack.mulPose(quaternion);
            // 截至目前，摄像机动画数据已消费完毕。是否有更好的清理动画数据的方法？
            model.cleanCameraAnimationTransform();
        });
    }

    @Override
    public void renderFirstPerson(LocalPlayer player, ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack, MultiBufferSource bufferSource,
                                  int light, float partialTick) {
        if (!(stack.getItem() instanceof IGun)) {
            return;
        }

        TimelessAPI.getGunDisplay(stack).ifPresent(display -> {
            BedrockGunModel gunModel = display.getGunModel();
            var animationStateMachine = display.getAnimationStateMachine();
            if (gunModel == null) {
                return;
            }

            // 在渲染之前，先更新动画，让动画数据写入模型
            List<ParticleEffectData> pendingParticles = null;
            if (animationStateMachine != null) {
                animationStateMachine.processContextIfExist(context -> {
                    updateContext(context, stack, player, partialTick);
                });
                animationStateMachine.update();
                // 消费本帧动画 particle_effects 关键帧触发的粒子请求
                pendingParticles = animationStateMachine.getAnimationController().consumePendingParticles();
            }

            poseStack.pushPose();
            // 逆转原版施加在手上的延滞效果，改为写入模型动画数据中
            float xRotOffset = Mth.lerp(partialTick, player.xBobO, player.xBob);
            float yRotOffset = Mth.lerp(partialTick, player.yBobO, player.yBob);
            float xRot = player.getViewXRot(partialTick) - xRotOffset;
            float yRot = player.getViewYRot(partialTick) - yRotOffset;
            poseStack.mulPose(Axis.XP.rotationDegrees(xRot * -0.1F));
            poseStack.mulPose(Axis.YP.rotationDegrees(yRot * -0.1F));
            BedrockPart rootNode = gunModel.getRootNode();
            if (rootNode != null) {
                xRot = (float) Math.tanh(xRot / 25) * 25;
                yRot = (float) Math.tanh(yRot / 25) * 25;
                rootNode.offsetX += yRot * 0.1F / 16F / 3F;
                rootNode.offsetY += -xRot * 0.1F / 16F / 3F;
                rootNode.additionalQuaternion.mul(Axis.XP.rotationDegrees(xRot * 0.05F));
                rootNode.additionalQuaternion.mul(Axis.YP.rotationDegrees(yRot * 0.05F));
            }
            // 从渲染原点 (0, 24, 0) 移动到模型原点 (0, 0, 0)
            poseStack.translate(0, 1.5f, 0);
            // 基岩版模型是上下颠倒的，需要翻转过来。
            poseStack.mulPose(Axis.ZP.rotationDegrees(180f));
            // 应用持枪姿态变换，如第一人称摄像机定位
            FirstPersonRenderGunEvent.applyFirstPersonGunTransform(player, stack, poseStack, gunModel, partialTick);

            InteractionHand particleHand = ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                    ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            gunModel.setParticleWindowRenderer(new IFunctionalRenderer() {
                @Override
                public void render(PoseStack poseStack1, VertexConsumer vertexBuffer1, ItemDisplayContext transformType1, int light1, int overlay1) {
                }

                @Override
                public void render(PoseStack poseStack1, VertexConsumer vertexBuffer1, ItemDisplayContext transformType1, int light1, int overlay1, MultiBufferSource particleBuffer) {
                    Matrix4f basePose = MuzzleParticleManager.getBasePose(particleHand);
                    if (basePose == null) {
                        return;
                    }
                    poseStack1.pushPose();
                    poseStack1.last().pose().set(basePose);
                    FirstPersonRenderHandler.renderParticlesNow(particleHand, poseStack1, particleBuffer, light1, partialTick);
                    poseStack1.popPose();
                }
            });

            // 开启第一人称弹壳和火焰渲染
            MuzzleFlashRender.isSelf = true;
            ShellRender.isSelf = true;
            // 如果正在打开改装界面，则取消手臂渲染
            boolean renderHand = gunModel.getRenderHand();
            if (RefitTransform.getOpeningProgress() != 0) {
                gunModel.setRenderHand(false);
            }
            InteractionHand hand = ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                    ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            MuzzleParticleManager.spawnAndBind(FirstPersonRenderHandler.getParticleSystem(), poseStack, gunModel, hand, pendingParticles);
            // 调用枪械模型渲染
            RenderType renderType = display.enablesTransparency()
                    ? RenderType.entityTranslucent(display.getModelTexture())
                    : RenderType.entityCutout(display.getModelTexture());
            gunModel.render(poseStack, stack, ctx, renderType, light, OverlayTexture.NO_OVERLAY, bufferSource);
            // 缓存枪口位置，为第一人称曳光弹渲染作准备
            cacheMuzzlePosition(poseStack, gunModel);
            // 创建并绑定第一人称枪口 snowstorm 粒子（火光/烟雾）

            // 恢复手臂渲染
            gunModel.setRenderHand(renderHand);
            // 渲染完成后，将动画数据从模型中清除，不对其他视角下的模型渲染产生影响
            poseStack.popPose();
            gunModel.cleanAnimationTransform();
            // 关闭第一人称弹壳和火焰渲染
            MuzzleFlashRender.isSelf = false;
            ShellRender.isSelf = false;
        });
    }

    /**
     * 第一人称粒子由本渲染器在枪体渲染的模板窗口内自行渲染（让粒子被镜内剔除），
     * SBM 因此跳过事件末尾的自动粒子渲染，避免双重绘制。
     */
    @Override
    public boolean renderParticlesInModelWindow() {
        return true;
    }

    private static void cacheMuzzlePosition(PoseStack poseStack, BedrockGunModel gunModel) {
        if (gunModel.getMuzzleFlashPosPath() != null) {
            // 计算出枪口相对于摄像机中心的坐标
            poseStack.pushPose();
            for (BedrockPart bedrockPart : gunModel.getMuzzleFlashPosPath()) {
                bedrockPart.translateAndRotateAndScale(poseStack);
            }
            Matrix4f pose = poseStack.last().pose();
            double itemRenderFov = CameraSetupEvent.ITEM_MODEL_FOV_DYNAMICS.get();
            double levelRenderFov = CameraSetupEvent.WORLD_FOV_DYNAMICS.get();
            poseStack.popPose();
            // 缓存转换后的偏移坐标。手部与世界的投影 FOV 不同，这里与枪口粒子使用同一套补偿：
            // 横向偏移 (x, y) 乘 tan(worldFov/2)/tan(modelFov/2)，让曳光弹起点落在模型 FOV 下枪口的
            // 屏幕位置上，而深度 z 保持真实，不再把起点搬到更近/更远的深度上。
            double itemTan = Math.tan(itemRenderFov / 2 * Math.PI / 180);
            double levelTan = Math.tan(levelRenderFov / 2 * Math.PI / 180);
            float lateral = itemTan > 1.0e-6 ? (float) (levelTan / itemTan) : 1.0f;
            muzzleRenderOffset.set(
                    pose.m30() * lateral,
                    pose.m31() * lateral,
                    pose.m32()
            );
        }
    }

    /**
     * SBM 第一人称粒子系统在渲染枪体前调用：捕获当前 pass 的基准矩阵，供粒子绑定变换使用。
     */
    @Override
    public void updateParticleEmitterTransforms(FirstPersonParticleSystem system, PoseStack poseStack, InteractionHand hand) {
        MuzzleParticleManager.captureBasePose(hand, poseStack.last().pose());
    }

    @Override
    public void renderByItem(@Nonnull ItemStack stack, @Nonnull ItemDisplayContext transformType, @Nonnull PoseStack poseStack, @Nonnull MultiBufferSource pBuffer,
                             int pPackedLight, int pPackedOverlay) {
        if (!(stack.getItem() instanceof IGun)) {
            return;
        }
        poseStack.pushPose();

        TimelessAPI.getGunDisplay(stack).ifPresentOrElse(gunIndex -> {
            // 第一人称就不渲染了，交给别的地方
            if (transformType == FIRST_PERSON_LEFT_HAND || transformType == FIRST_PERSON_RIGHT_HAND) {
                return;
            }
            // 第三人称副手也不渲染了
            if (transformType == THIRD_PERSON_LEFT_HAND) {
                return;
            }
            // GUI 特殊渲染
            if (transformType == GUI) {
                renderSlotTexture(poseStack, pBuffer, pPackedLight, pPackedOverlay, gunIndex.getSlotTexture());
                return;
            }
            // Static external appearances own the call even while capture/upload is pending.
            boolean outsideHighPolyDistance = !RenderDistance.inRenderHighPolyModelDistance(poseStack);
            ImmediateStaticMeshRenderer.DrawResult vbo = ExternalGunItemVboRenderer.tryRender(
                    stack, transformType, outsideHighPolyDistance, poseStack, pBuffer, pPackedLight, pPackedOverlay);
            if (vbo == ImmediateStaticMeshRenderer.DrawResult.PENDING
                    || vbo == ImmediateStaticMeshRenderer.DrawResult.DRAWN && transformType == GROUND) return;

            // The old model is needed only for fallback or third-person dynamic effects.
            BedrockGunModel gunModel;
            ResourceLocation gunTexture;
            Pair<BedrockGunModel, ResourceLocation> lodModel = gunIndex.getLodModel();
            boolean useLod = lodModel != null && outsideHighPolyDistance;
            if (!useLod) {
                gunModel = gunIndex.getGunModel();
                gunTexture = gunIndex.getModelTexture();
            } else {
                gunModel = lodModel.getLeft();
                gunTexture = lodModel.getRight();
            }
            if (gunModel == null) {
                renderSlotTexture(poseStack, pBuffer, pPackedLight, pPackedOverlay, gunIndex.getSlotTexture());
                return;
            }
            if (vbo == ImmediateStaticMeshRenderer.DrawResult.DRAWN) {
                if (transformType == THIRD_PERSON_RIGHT_HAND) {
                    poseStack.pushPose();
                    try {
                        GunItemTransforms.apply(transformType, gunIndex.getTransform().getScale(), gunModel, poseStack);
                        
                        gunModel.renderExternalLaser(poseStack, stack, transformType, pBuffer);
                        renderExternalAttachmentLasers(stack, gunModel, poseStack, transformType, pBuffer);
                        captureThirdPersonMuzzle(poseStack, gunModel);
                    } finally {
                        poseStack.popPose();
                    }
                }
                return;
            }
            GunItemTransforms.apply(transformType, gunIndex.getTransform().getScale(), gunModel, poseStack);
            // 渲染枪械模型
            float alpha = ItemPreviewRenderContext.alpha();
            RenderType renderType = ItemPreviewRenderContext.isPreview() ? RenderType.entityTranslucent(gunTexture) : RenderType.entityCutout(gunTexture);
            
            gunModel.render(poseStack, stack, transformType, renderType, pPackedLight, pPackedOverlay, 1.0F, 1.0F, 1.0F, alpha, pBuffer);
            // 第三人称持枪渲染：捕获枪口世界坐标，触发第三人称枪口粒子
            if (transformType == THIRD_PERSON_RIGHT_HAND) captureThirdPersonMuzzle(poseStack, gunModel);
        }, () -> {
            // 没有这个 gunID，渲染个错误材质提醒别人
            renderSlotTexture(poseStack, pBuffer, pPackedLight, pPackedOverlay, MissingTextureAtlasSprite.getLocation());
        });

        poseStack.popPose();
    }

    public static void captureThirdPersonMuzzle(PoseStack poseStack, BedrockGunModel gunModel) {
        if (currentEntity == null) return;
        List<BedrockPart> muzzlePath = gunModel.getMuzzleFlashPosPath();
        if (muzzlePath == null) return;
        poseStack.pushPose();
        try {
            for (BedrockPart part : muzzlePath) part.translateAndRotateAndScale(poseStack);
            Matrix4f pose = poseStack.last().pose();
            // Undo the camera rotation in the entity pose and restore the camera position.
            Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
            Vec3 camPos = camera.getPosition();
            Matrix4f viewRotationInv = new Matrix4f()
                    .rotationY((float) Math.toRadians(-(camera.getYRot() + 180f)))
                    .rotateX((float) Math.toRadians(-camera.getXRot()))
                    .rotateZ(-CameraStateCache.getCameraRollRadians());
            Matrix4f muzzleWorld = new Matrix4f(viewRotationInv).mul(pose);
            muzzleWorld.m30(muzzleWorld.m30() + (float) camPos.x);
            muzzleWorld.m31(muzzleWorld.m31() + (float) camPos.y);
            muzzleWorld.m32(muzzleWorld.m32() + (float) camPos.z);
            onMuzzleRendered(currentEntity, muzzleWorld);
        } finally {
            poseStack.popPose();
        }
    }

    public static void renderExternalAttachmentLasers(ItemStack stack, BedrockGunModel gunModel,
                                                       PoseStack poseStack, ItemDisplayContext context,
                                                       MultiBufferSource bufferSource) {
        IGun gun = IGun.getIGunOrNull(stack);
        if (gun == null) return;
        for (AttachmentType type : AttachmentType.values()) {
            if (type == AttachmentType.NONE) continue;
            ItemStack attached = gun.getAttachment(stack, type);
            if (attached.isEmpty()) attached = gun.getBuiltinAttachment(stack, type);
            IAttachment item = IAttachment.getIAttachmentOrNull(attached);
            if (item == null) continue;
            var index = TimelessAPI.getClientAttachmentIndex(item.getAttachmentId(attached)).orElse(null);
            List<BedrockPart> mountPath = gunModel.getAttachmentPositionPath(type);
            if (index == null || mountPath == null) continue;
            poseStack.pushPose();
            try {
                for (BedrockPart part : mountPath) part.translateAndRotateAndScale(poseStack);
                poseStack.translate(0, -1.5, 0);
                var model = index.getAttachmentModel();
                var lod = index.getLodModel();
                if (lod != null && !RenderDistance.inRenderHighPolyModelDistance(poseStack)) model = lod.getLeft();
                if (model != null) model.renderExternalLasers(attached, poseStack, context, bufferSource);
            } finally {
                poseStack.popPose();
            }
        }
    }

    private static void renderSlotTexture(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, ResourceLocation texture) {
        poseStack.translate(0.5, 1.5, 0.5);
        poseStack.mulPose(Axis.ZN.rotationDegrees(180));
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityTranslucent(texture));
        SLOT_GUN_MODEL.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, ItemPreviewRenderContext.alpha());
    }

}
