package com.tac.guns.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import com.tac.guns.GunMod;
import com.tac.guns.api.client.event.RenderItemInHandBobEvent;
import com.tac.guns.client.animation.internal.GunAnimationStateMachine;
import com.tac.guns.client.model.BedrockGunModel;
import com.tac.guns.client.model.bedrock.BedrockPart;
import com.tac.guns.client.resource.ClientGunLoader;
import com.tac.guns.client.resource.index.ClientGunIndex;
import com.tac.guns.init.ModItems;
import com.tac.guns.item.GunItem;
import com.tac.guns.util.math.MathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * 负责第一人称的枪械模型渲染。其他人称参见 {@link com.tac.guns.client.renderer.tileentity.TileEntityItemStackGunRenderer}
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GunMod.MOD_ID)
public class FirstPersonRenderGunEvent {
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        LocalPlayer player = mc.player;
        InteractionHand hand = event.getHand();
        ItemStack stack = event.getItemStack();
        ItemTransforms.TransformType transformType = hand == InteractionHand.MAIN_HAND ? ItemTransforms.TransformType.FIRST_PERSON_RIGHT_HAND : ItemTransforms.TransformType.FIRST_PERSON_LEFT_HAND;
        if (stack.is(ModItems.GUN.get())) {
            ResourceLocation gunId = GunItem.getData(player.getMainHandItem()).getGunId();
            ClientGunLoader.getGunIndex(gunId).ifPresent(gunIndex -> {
                BedrockGunModel gunModel = gunIndex.getGunModel();
                GunAnimationStateMachine animationStateMachine = gunIndex.getAnimationStateMachine();
                if (gunModel != null) {
                    // 在渲染之前，先更新动画，让动画数据写入模型
                    if (animationStateMachine != null) {
                        animationStateMachine.update();
                    }
                    PoseStack poseStack = event.getPoseStack();
                    poseStack.pushPose();
                    // 从渲染原点(0, 24, 0)移动到模型原点(0, 0, 0)
                    poseStack.translate(0, 1.5f, 0);
                    // 基岩版模型是上下颠倒的，需要翻转过来。
                    poseStack.mulPose(Vector3f.ZP.rotationDegrees(180f));
                    // 应用枪械动态，如第一人称摄像机定位、后坐力的位移等
                    applyFirstPersonGunTransform(player, stack, gunIndex, poseStack, gunModel);
                    // 调用模型渲染
                    gunModel.render(0, transformType, stack, player, poseStack, event.getMultiBufferSource(), event.getPackedLight(), OverlayTexture.NO_OVERLAY);
                    // 渲染完成后，将动画数据从模型中清除，不对其他视角下的模型渲染产生影响
                    poseStack.popPose();
                    gunModel.cleanAnimationTransform();
                }
            });
            event.setCanceled(true);
        }
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
        ItemStack mainHandItem = mc.player.getMainHandItem();
        if (mainHandItem.is(ModItems.GUN.get())) {
            event.setCanceled(true);
        }
    }

    private static void applyFirstPersonGunTransform(LocalPlayer player,
                                                     ItemStack gunItemStack,
                                                     ClientGunIndex gunIndex,
                                                     PoseStack poseStack,
                                                     BedrockGunModel model) {
        // 应用定位组的变换（位移和旋转，不包括缩放）
        applyFirstPersonPositioningTransform(poseStack, model);
    }

    /**
     * 应用瞄具摄像机定位组、机瞄摄像机定位组和 Idle 摄像机定位组的变换。
     */
    private static void applyFirstPersonPositioningTransform(PoseStack poseStack, BedrockGunModel model) {
        // todo v就是瞄准动作的进度
        float v = 0;
        // todo 判断是否安装瞄具，上半部分是未安装瞄具时，根据机瞄定位组"iron_sight"应用位移。下半部分是安装瞄具时，应用瞄具定位组和瞄具模型内定位组的位移。
        if (true) {
            applyPositioningNodeTransform(model.getIronSightPath(), poseStack, v);
        } else {
            applyPositioningNodeTransform(model.getScopePosPath(), poseStack, v);
        }
        applyPositioningNodeTransform(model.getIdleSightPath(), poseStack, 1 - v);
    }

    private static void applyPositioningNodeTransform(List<BedrockPart> nodePath, PoseStack poseStack, float weight) {
        if (nodePath == null) return;
        //应用定位组的反向位移、旋转，使定位组的位置就是渲染中心
        poseStack.translate(0, 1.5f, 0);
        for (int f = nodePath.size() - 1; f >= 0; f--) {
            BedrockPart t = nodePath.get(f);
            poseStack.mulPose(Vector3f.XN.rotation(t.xRot));
            poseStack.mulPose(Vector3f.YN.rotation(t.yRot));
            poseStack.mulPose(Vector3f.ZN.rotation(t.zRot));
            if (t.getParent() != null)
                poseStack.translate(-t.x / 16.0F * weight, -t.y / 16.0F * weight, -t.z / 16.0F * weight);
            else {
                poseStack.translate(-t.x / 16.0F * weight, (1.5F - t.y / 16.0F) * weight, -t.z / 16.0F * weight);
            }
        }
        poseStack.translate(0, -1.5f, 0);
    }
}
