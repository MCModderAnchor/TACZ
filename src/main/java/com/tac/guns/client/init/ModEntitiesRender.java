package com.tac.guns.client.init;

import com.tac.guns.block.entity.GunSmithTableBlockEntity;
import com.tac.guns.block.entity.TargetBlockEntity;
import com.tac.guns.client.renderer.block.GunSmithTableRenderer;
import com.tac.guns.client.renderer.block.TargetRenderer;
import com.tac.guns.client.renderer.entity.EntityBulletRenderer;
import com.tac.guns.client.renderer.entity.TargetMinecartRenderer;
import com.tac.guns.entity.EntityBullet;
import com.tac.guns.entity.TargetMinecart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntitiesRender {
    @SubscribeEvent
    public static void onEntityRenderers(EntityRenderersEvent.RegisterRenderers evt) {
        EntityRenderers.register(EntityBullet.TYPE, EntityBulletRenderer::new);
        EntityRenderers.register(TargetMinecart.TYPE, TargetMinecartRenderer::new);
        BlockEntityRenderers.register(GunSmithTableBlockEntity.TYPE, GunSmithTableRenderer::new);
        BlockEntityRenderers.register(TargetBlockEntity.TYPE, TargetRenderer::new);
    }
}
