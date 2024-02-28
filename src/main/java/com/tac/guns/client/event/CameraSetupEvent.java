package com.tac.guns.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Quaternion;
import com.tac.guns.GunMod;
import com.tac.guns.api.client.event.BeforeRenderHandEvent;
import com.tac.guns.client.model.BedrockAnimatedModel;
import com.tac.guns.client.resource.cache.ClientAssetManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = GunMod.MOD_ID)
public class CameraSetupEvent {
    @SubscribeEvent
    public static void applyCameraAnimation(EntityViewRenderEvent.CameraSetup event){
        if(Minecraft.getInstance().player == null) return;
        if(!Minecraft.getInstance().options.bobView) return;
        // todo 把硬编码改掉
        BedrockAnimatedModel bedrockAnimatedModel = ClientAssetManager.INSTANCE.getBedrockAnimatedAsset(new ResourceLocation("tac", "ak47")).model();
        Quaternion q = bedrockAnimatedModel.getCameraAnimationObject().rotationQuaternion;
        double yaw = Math.asin(2 * (q.r() * q.j() - q.i() * q.k()));
        double pitch = Math.atan2(2 * (q.r() * q.i() + q.j() * q.k()), 1 - 2 * (q.i() * q.i() + q.j() * q.j()));
        double roll = Math.atan2(2 * (q.r() * q.k() + q.i() * q.j()), 1 - 2 * (q.j() * q.j() + q.k() * q.k()));
        yaw = Math.toDegrees(yaw);
        pitch = Math.toDegrees(pitch);
        roll = Math.toDegrees(roll);
        event.setYaw((float) yaw + event.getYaw());
        event.setPitch((float) pitch + event.getPitch());
        event.setRoll((float) roll + event.getRoll());
    }

    @SubscribeEvent
    public static void applyItemLayerCameraAnimation(BeforeRenderHandEvent event){
        if(!Minecraft.getInstance().options.bobView) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if(player == null) return;
        // todo 把硬编码改掉
        BedrockAnimatedModel animatedModel = ClientAssetManager.INSTANCE.getBedrockAnimatedAsset(new ResourceLocation("tac", "ak47")).model();
        PoseStack poseStack = event.getPoseStack();
        poseStack.mulPose(animatedModel.getCameraAnimationObject().rotationQuaternion);
    }
}
