package com.tac.guns.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import com.tac.guns.GunMod;
import com.tac.guns.api.client.event.RenderItemInHandBobEvent;
import com.tac.guns.api.client.player.IClientPlayerGunOperator;
import com.tac.guns.api.item.IGun;
import com.tac.guns.client.animation.internal.GunAnimationStateMachine;
import com.tac.guns.client.model.BedrockGunModel;
import com.tac.guns.client.model.bedrock.BedrockPart;
import com.tac.guns.client.renderer.item.GunItemRenderer;
import com.tac.guns.client.resource.ClientGunPackLoader;
import com.tac.guns.client.resource.index.ClientGunIndex;
import com.tac.guns.client.resource.pojo.display.CommonTransformObject;
import com.tac.guns.util.math.SecondOrderDynamics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.block.model.ItemTransforms.TransformType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.List;

import static net.minecraft.client.renderer.block.model.ItemTransforms.TransformType.FIRST_PERSON_RIGHT_HAND;

/**
 * 负责第一人称的枪械模型渲染。其他人称参见 {@link GunItemRenderer}
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GunMod.MOD_ID)
public class FirstPersonRenderGunEvent {
    // 用于生成瞄准动作的运动曲线，使动作看起来更平滑
    private static final SecondOrderDynamics AIMING_DYNAMICS = new SecondOrderDynamics(0.75f, 1.2f, 0.5f, 0);
    private static ItemStack oldHotbarSelectedStack = ItemStack.EMPTY;
    private static int oldHotbarSelected = -1;
    private static float oldAimingProgress = 0;
    private static float aimingProgress = 0;

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
        ClientGunPackLoader.getGunIndex(gunId).ifPresent(gunIndex -> {
            BedrockGunModel gunModel = gunIndex.getGunModel();
            GunAnimationStateMachine animationStateMachine = gunIndex.getAnimationStateMachine();
            if (gunModel == null) {
                return;
            }
            Inventory inventory = player.getInventory();
            ItemStack inventorySelected = inventory.getSelected();
            if (oldHotbarSelected != inventory.selected || !isSame(inventorySelected, oldHotbarSelectedStack)) {
                oldHotbarSelected = inventory.selected;
                oldHotbarSelectedStack = inventorySelected;
                IClientPlayerGunOperator.fromLocalPlayer(player).draw();
            }
            // 在渲染之前，先更新动画，让动画数据写入模型
            if (animationStateMachine != null) {
                animationStateMachine.update();
            }
            PoseStack poseStack = event.getPoseStack();
            poseStack.pushPose();
            // 从渲染原点 (0, 24, 0) 移动到模型原点 (0, 0, 0)
            poseStack.translate(0, 1.5f, 0);
            // 基岩版模型是上下颠倒的，需要翻转过来。
            poseStack.mulPose(Vector3f.ZP.rotationDegrees(180f));
            // 应用枪械动态，如第一人称摄像机定位、后坐力的位移等
            applyFirstPersonGunTransform(player, stack, gunIndex, poseStack, gunModel, event.getPartialTicks());
            // 调用模型渲染
            gunModel.render(0, transformType, stack, player, poseStack, event.getMultiBufferSource(), event.getPackedLight(), OverlayTexture.NO_OVERLAY);
            // 渲染完成后，将动画数据从模型中清除，不对其他视角下的模型渲染产生影响
            poseStack.popPose();
            gunModel.cleanAnimationTransform();
        });
        event.setCanceled(true);
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
        if (IGun.mainhandHoldGun(mc.player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void tickAimingLerp(TickEvent.ClientTickEvent tickEvent) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        oldAimingProgress = aimingProgress;
        aimingProgress = IClientPlayerGunOperator.fromLocalPlayer(player).getClientAimingProgress();
    }

    private static void applyFirstPersonGunTransform(LocalPlayer player, ItemStack gunItemStack, ClientGunIndex gunIndex,
                                                     PoseStack poseStack, BedrockGunModel model, float partialTicks) {
        // 应用定位组的变换（位移和旋转，不包括缩放）
        applyFirstPersonPositioningTransform(poseStack, model, gunIndex, partialTicks);
    }

    /**
     * 应用瞄具摄像机定位组、机瞄摄像机定位组和 Idle 摄像机定位组的变换。
     */
    private static void applyFirstPersonPositioningTransform(PoseStack poseStack, BedrockGunModel model, ClientGunIndex gunIndex,
                                                             float partialTicks) {
        float weight = Mth.lerp(partialTicks, oldAimingProgress, aimingProgress);
        weight = AIMING_DYNAMICS.update(0, weight);
        // TODO 判断是否安装瞄具，上半部分是未安装瞄具时，根据机瞄定位组应用位移。下半部分是安装瞄具时，应用瞄具定位组和瞄具模型内定位组的位移。
        if (true) {
            applyPositioningNodeTransform(model.getIronSightPath(), poseStack, weight, gunIndex.getAnimationInfluenceCoefficient().getIronView());
        } else {
            applyPositioningNodeTransform(model.getScopePosPath(), poseStack, weight, gunIndex.getAnimationInfluenceCoefficient().getScopeView());
        }
        applyPositioningNodeTransform(model.getIdleSightPath(), poseStack, 1 - weight, null);
    }

    /**
     * 应用定位组的 Transform。这个方法是为客户端定制的，它能指定 Transform 的权重（用于在复数个摄像机位置间插值），
     * 以及削弱第一人称动画的效果（在第一人称动画播放时尽可能地保证摄像机与模型的相对位置不变，如枪械开火时，不让枪身抖动得太厉害）。
     */
    private static void applyPositioningNodeTransform(List<BedrockPart> nodePath, PoseStack poseStack, float weight,
                                                      @Nullable CommonTransformObject ica) {
        if (nodePath == null) {
            return;
        }
        // 应用定位组的反向位移、旋转，使定位组的位置就是渲染中心
        poseStack.translate(0, 1.5f, 0);
        for (int i = nodePath.size() - 1; i >= 0; i--) {
            BedrockPart part = nodePath.get(i);
            // 如果需要削弱动画 则应用反相的额外旋转参数
            if (ica != null) {
                Quaternion q = part.additionalQuaternion;
                double pitch = Math.atan2(2 * (q.r() * q.i() + q.j() * q.k()), 1 - 2 * (q.i() * q.i() + q.j() * q.j()));
                double yaw = Math.asin(2 * (q.r() * q.j() - q.i() * q.k()));
                double roll = Math.atan2(2 * (q.r() * q.k() + q.i() * q.j()), 1 - 2 * (q.j() * q.j() + q.k() * q.k()));
                poseStack.mulPose(Vector3f.XN.rotation((float) (pitch * weight * (1 - ica.getRotation().x()))));
                poseStack.mulPose(Vector3f.YN.rotation((float) (yaw * weight * (1 - ica.getRotation().y()))));
                poseStack.mulPose(Vector3f.ZN.rotation((float) (roll * weight * (1 - ica.getRotation().z()))));
            }
            // 应用反向的旋转
            poseStack.mulPose(Vector3f.XN.rotation(part.xRot * weight));
            poseStack.mulPose(Vector3f.YN.rotation(part.yRot * weight));
            poseStack.mulPose(Vector3f.ZN.rotation(part.zRot * weight));
            // 如果需要削弱动画，则应用反向的额外位移参数
            if (ica != null) {
                poseStack.translate(
                        -part.offsetX * weight * (1 - ica.getTranslation().x()),
                        -part.offsetY * weight * (1 - ica.getTranslation().y()),
                        -part.offsetZ * weight * (1 - ica.getTranslation().z())
                );
            }
            // 应用反向的位移
            if (part.getParent() != null) {
                poseStack.translate(-part.x / 16.0F * weight, -part.y / 16.0F * weight, -part.z / 16.0F * weight);
            } else {
                poseStack.translate(-part.x / 16.0F * weight, (1.5F - part.y / 16.0F) * weight, -part.z / 16.0F * weight);
            }
        }
        poseStack.translate(0, -1.5f, 0);
    }

    /**
     * 判断两个枪械 ID 是否相同
     */
    private static boolean isSame(ItemStack gunA, ItemStack gunB) {
        if (gunA.getItem() instanceof IGun iGunA && gunB.getItem() instanceof IGun iGunB) {
            return iGunA.getGunId(gunA).equals(iGunB.getGunId(gunB));
        }
        return gunA.sameItem(gunB);
    }
}
