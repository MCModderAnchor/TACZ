package com.tac.guns.client.event;

import com.tac.guns.GunMod;
import com.tac.guns.client.input.CheckGunKey;
import com.tac.guns.client.model.BedrockGunModel;
import com.tac.guns.client.resource.GunLoader;
import com.tac.guns.init.ModItems;
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
public class RenderArmGunEvent {
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
            BedrockGunModel model = GunLoader.getGunModel("ak47");
            if (CheckGunKey.AK47AC != null) {
                CheckGunKey.AK47AC.update();
            }
            model.render(0, transformType, stack, player, event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), OverlayTexture.NO_OVERLAY);
            event.setCanceled(true);
        }
    }
}
