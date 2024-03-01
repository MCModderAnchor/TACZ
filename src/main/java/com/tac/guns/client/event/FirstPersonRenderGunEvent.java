package com.tac.guns.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tac.guns.GunMod;
import com.tac.guns.api.client.event.RenderItemInHandBobEvent;
import com.tac.guns.client.animation.AnimationController;
import com.tac.guns.client.model.BedrockGunModel;
import com.tac.guns.client.resource.ClientGunLoader;
import com.tac.guns.client.resource.cache.data.ClientGunIndex;
import com.tac.guns.init.ModItems;
import com.tac.guns.item.GunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

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
            ClientGunIndex gunIndex = ClientGunLoader.getGunIndex(GunItem.DEFAULT);
            BedrockGunModel gunModel = gunIndex.getGunModel();
            AnimationController controller = gunIndex.getController();
            if (gunModel != null) {
                // 在渲染之前，先更新动画，让动画数据写入模型
                if (controller != null) {
                    controller.update();
                }
                PoseStack poseStack = event.getPoseStack();
                // 应用枪械动态，如取消原版的bobbing、瞄准时的位移、后坐力的位移等
                applyFirstPersonGunMoving(player, stack, gunIndex, poseStack);
                // 调用模型渲染
                gunModel.render(0, transformType, stack, player, poseStack, event.getMultiBufferSource(), event.getPackedLight(), OverlayTexture.NO_OVERLAY);
                // 渲染完成后，将动画数据从模型中清除，不对其他视角下的模型渲染产生影响
                gunModel.cleanAnimationTransform();
                event.setCanceled(true);
            }
        }
    }

    /**
     * 当主手拿着枪械物品的时候，取消应用在它上面的 viewBobbing。
     */
    @SubscribeEvent
    public static void cancelItemInHandViewBobbing(RenderItemInHandBobEvent.BobView event){
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        ItemStack mainHandItem = mc.player.getMainHandItem();
        if (mainHandItem.is(ModItems.GUN.get())) {
            event.setCanceled(true);
        }
    }

    private static void applyFirstPersonGunMoving(LocalPlayer player, ItemStack gunItemStack, ClientGunIndex gunIndex, PoseStack poseStack){

    }
}
