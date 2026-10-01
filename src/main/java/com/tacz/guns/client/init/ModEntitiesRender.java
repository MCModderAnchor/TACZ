package com.tacz.guns.client.init;

import com.tacz.guns.block.entity.GunSmithTableBlockEntity;
import com.tacz.guns.block.entity.GunDisplayCaseBlockEntity;
import com.tacz.guns.block.entity.GunPegboardBlockEntity;
import com.tacz.guns.block.entity.StatueBlockEntity;
import com.tacz.guns.block.entity.TargetBlockEntity;
import com.tacz.guns.client.renderer.block.GunSmithTableRenderer;
import com.tacz.guns.client.renderer.block.GunDisplayCaseRenderer;
import com.tacz.guns.client.renderer.block.GunPegboardRenderer;
import com.tacz.guns.client.renderer.block.StatueRenderer;
import com.tacz.guns.client.renderer.block.TargetRenderer;
import com.tacz.guns.client.renderer.entity.EntityBulletRenderer;
import com.tacz.guns.client.renderer.entity.TargetMinecartRenderer;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.entity.TargetMinecart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class ModEntitiesRender {
    @SubscribeEvent
    public static void onEntityRenderers(EntityRenderersEvent.RegisterRenderers evt) {
        EntityRenderers.register(EntityKineticBullet.TYPE, EntityBulletRenderer::new);
        EntityRenderers.register(TargetMinecart.TYPE, TargetMinecartRenderer::new);
        BlockEntityRenderers.register(GunSmithTableBlockEntity.TYPE, GunSmithTableRenderer::new);
        BlockEntityRenderers.register(TargetBlockEntity.TYPE, TargetRenderer::new);
        BlockEntityRenderers.register(StatueBlockEntity.TYPE, StatueRenderer::new);
        BlockEntityRenderers.register(GunPegboardBlockEntity.TYPE, GunPegboardRenderer::new);
        BlockEntityRenderers.register(GunDisplayCaseBlockEntity.TYPE, GunDisplayCaseRenderer::new);
    }
}
