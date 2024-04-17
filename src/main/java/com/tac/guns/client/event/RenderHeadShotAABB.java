package com.tac.guns.client.event;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tac.guns.config.client.RenderConfig;
import com.tac.guns.event.HeadShotAABBConfigRead;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class RenderHeadShotAABB {
    @SubscribeEvent
    public static void onRenderEntity(RenderLivingEvent.Post<?, ?> event) {
        boolean canRender = Minecraft.getInstance().getEntityRenderDispatcher().shouldRenderHitBoxes();
        if (!canRender) {
            return;
        }
        if (!RenderConfig.HEAD_SHOT_DEBUG_HITBOX.get()) {
            return;
        }
        LivingEntity entity = event.getEntity();
        ResourceLocation entityId = ForgeRegistries.ENTITIES.getKey(entity.getType());
        if (entityId == null) {
            return;
        }
        AABB aabb = HeadShotAABBConfigRead.getAABB(entityId);
        if (aabb == null) {
            return;
        }
        VertexConsumer buffer = event.getMultiBufferSource().getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(event.getPoseStack(), buffer, aabb, 1.0F, 1.0F, 0.0F, 1.0F);
    }
}
