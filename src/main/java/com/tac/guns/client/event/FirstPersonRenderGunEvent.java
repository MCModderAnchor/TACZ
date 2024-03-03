package com.tac.guns.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.tac.guns.GunMod;
import com.tac.guns.api.client.event.RenderItemInHandBobEvent;
import com.tac.guns.api.client.player.IClientPlayerGunOperator;
import com.tac.guns.api.item.IGun;
import com.tac.guns.client.animation.internal.GunAnimationStateMachine;
import com.tac.guns.client.model.BedrockGunModel;
import com.tac.guns.client.model.bedrock.BedrockPart;
import com.tac.guns.client.resource.ClientGunPackLoader;
import com.tac.guns.client.resource.index.ClientGunIndex;
import com.tac.guns.item.GunItem;
import com.tac.guns.item.nbt.GunItemData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.block.model.ItemTransforms.TransformType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

import static net.minecraft.client.renderer.block.model.ItemTransforms.TransformType.FIRST_PERSON_RIGHT_HAND;

/**
 * 负责第一人称的枪械模型渲染。其他人称参见 {@link com.tac.guns.client.renderer.tileentity.TileEntityItemStackGunRenderer}
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GunMod.MOD_ID)
public class FirstPersonRenderGunEvent {
    private static int hotbarSelected = -1;
    private static ItemStack hotbarSelectedStack = ItemStack.EMPTY;

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
        TransformType transformType;
        if (event.getHand() == InteractionHand.MAIN_HAND) {
            transformType = FIRST_PERSON_RIGHT_HAND;
        } else {
            transformType = TransformType.FIRST_PERSON_LEFT_HAND;
        }
        if (!IGun.isGun(stack)) {
            return;
        }

        ResourceLocation gunId = GunItem.getData(player.getItemInHand(event.getHand())).getGunId();
        ClientGunPackLoader.getGunIndex(gunId).ifPresent(gunIndex -> {
            BedrockGunModel gunModel = gunIndex.getGunModel();
            GunAnimationStateMachine animationStateMachine = gunIndex.getAnimationStateMachine();
            if (gunModel == null) {
                return;
            }
            Inventory inventory = player.getInventory();
            ItemStack inventorySelected = inventory.getSelected();
            // FIXME 未来切枪，NBT 变了可能有问题
            if (hotbarSelected != inventory.selected || !isSame(inventorySelected, hotbarSelectedStack)) {
                hotbarSelected = inventory.selected;
                hotbarSelectedStack = inventorySelected;
                IClientPlayerGunOperator.fromLocalPlayer(player).draw();
            }
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

    private static boolean isSame(ItemStack gunA, ItemStack gunB) {
        if (IGun.isGun(gunA) && IGun.isGun(gunB)) {
            GunItemData dataA = GunItem.getData(gunA);
            GunItemData dataB = GunItem.getData(gunB);
            return dataA.getGunId().equals(dataB.getGunId());
        }
        return gunA.sameItem(gunB);
    }
}
