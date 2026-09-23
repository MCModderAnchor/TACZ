package com.tacz.guns.client.renderer.block;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/** 采用原版无方向光、带 alpha 裁剪的 shader；独立配置实体材质的深度与混合状态。 */
public final class DisplayGunRenderType extends RenderStateShard {
    private static final Map<ResourceLocation, RenderType> CACHE = new HashMap<>();

    private DisplayGunRenderType(String name, Runnable setup, Runnable clear) {
        super(name, setup, clear);
    }

    public static RenderType emissive(ResourceLocation texture) {
        return CACHE.computeIfAbsent(texture, key -> RenderType.create(
                "tacz_display_gun_emissive", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, false,
                RenderType.CompositeState.builder()
                        .setShaderState(RENDERTYPE_ENERGY_SWIRL_SHADER)
                        .setTextureState(new TextureStateShard(texture, false, false))
                        .setTransparencyState(NO_TRANSPARENCY)
                        .setCullState(CULL)
                        .setTexturingState(new TexturingStateShard("tacz_display_gun_identity_uv",
                                RenderSystem::resetTextureMatrix, RenderSystem::resetTextureMatrix))
                        .setLightmapState(LIGHTMAP)
                        .setOverlayState(OVERLAY)
                        .setOutputState(ITEM_ENTITY_TARGET)
                        .setWriteMaskState(COLOR_DEPTH_WRITE)
                        .createCompositeState(true)));
    }
}
