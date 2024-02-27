package com.tac.guns.client.event;

import com.tac.guns.GunMod;
import com.tac.guns.client.input.CheckGunKey;
import com.tac.guns.client.model.BedrockAnimatedModel;
import com.tac.guns.client.model.BedrockGunModel;
import com.tac.guns.client.resource.BedrockAssetManager;
import com.tac.guns.init.ModItems;
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
            BedrockAnimatedModel model = BedrockAssetManager.INSTANCE.getModel(new ResourceLocation("tac", "ak47"));
            if (model instanceof BedrockGunModel gunModel) {
                if (CheckGunKey.AK47AC != null) {
                    CheckGunKey.AK47AC.update();
                }
                if (CheckGunKey.AK47_FIRE != null) {
                    CheckGunKey.AK47_FIRE.update();
                }
                gunModel.render(0, transformType, stack, player, event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), OverlayTexture.NO_OVERLAY);
                event.setCanceled(true);
            }
        }
    }
}
