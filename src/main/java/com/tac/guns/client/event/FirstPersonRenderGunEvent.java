package com.tac.guns.client.event;

import com.tac.guns.GunMod;
import com.tac.guns.client.model.BedrockGunModel;
import com.tac.guns.client.resource.cache.ClientAssetManager;
import com.tac.guns.client.resource.cache.data.BedrockAnimatedAsset;
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
            BedrockAnimatedAsset asset = ClientAssetManager.INSTANCE.getBedrockAnimatedAsset(GunItem.DEFAULT);
            if (asset != null && asset.model() instanceof BedrockGunModel gunModel) {
                if (asset.defaultController() != null) {
                    asset.defaultController().update();
                }
                gunModel.render(0, transformType, stack, player, event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), OverlayTexture.NO_OVERLAY);
                event.setCanceled(true);
            }
        }
    }
}
