package com.tacz.guns.client.renderer.item;

import com.github.mcmodderanchor.simplebedrockmodel.v2.client.mesh.MeshDrawResult;
import com.github.mcmodderanchor.simplebedrockmodel.v2.client.mesh.StaticMeshRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.renderer.block.DisplayGunGeometry;
import com.tacz.guns.client.renderer.external.ExternalGunAppearance;
import com.tacz.guns.config.client.RenderConfig;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.time.Duration;

/** Immediate VBO path for static third-person, ground and fixed gun appearances. */
public class ExternalGunItemVboRenderer {
    private static final ResourceLocation OWNER = new ResourceLocation("tacz", "external_item_guns");
    private static boolean wasEnabled;
    private static final Duration IDLE_RETENTION = Duration.ofSeconds(30);

    private ExternalGunItemVboRenderer() {}

    /** Each item context bakes a different fixed transform into its own static VBO. */
    private record ItemGeometryKey(ExternalGunAppearance.GeometryKey appearance, ItemDisplayContext context) {}

    public static StaticGunRenderResult tryRender(ItemStack stack, ItemDisplayContext context,
                                                   boolean useLod, PoseStack pose, MultiBufferSource buffers,
                                                   int packedLight, int packedOverlay) {
        if (!RenderConfig.STATIC_ITEM_GUN_VBO.get()) {
            if (wasEnabled) StaticMeshRenderer.invalidateOwner(OWNER);
            wasEnabled = false;
            return StaticGunRenderResult.UNSUPPORTED;
        }
        wasEnabled = true;
        if (!StaticMeshRenderer.isOwnerEnabled(OWNER)) return StaticGunRenderResult.UNSUPPORTED;
        if (ItemPreviewRenderContext.isPreview() || DisplayItemRenderContext.isActive()
                || context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                && context != ItemDisplayContext.GROUND
                && (context != ItemDisplayContext.FIXED || ScreenItemVboContext.isActive())) {
            return StaticGunRenderResult.UNSUPPORTED;
        }
        // Permanent backend/parameter limitations must escape the recognized-appearance PENDING policy.
        if (!StaticMeshRenderer.supportsInstanceAttributes(packedLight, packedOverlay)) {
            return StaticGunRenderResult.UNSUPPORTED;
        }
        ExternalGunAppearance.Variants appearances = ExternalGunAppearance.resolve(stack, false);
        if (appearances == null) return StaticGunRenderResult.UNSUPPORTED;
        ExternalGunAppearance.Snapshot appearance = appearances.select(useLod);
        ExternalGunAppearance.Snapshot previous = appearances.select(!useLod);
        MeshDrawResult result = StaticMeshRenderer.tryDraw(OWNER,
                new ItemGeometryKey(appearance.key(), context), new ItemGeometryKey(previous.key(), context),
                collector -> DisplayGunGeometry.capture(appearance, context, true, collector),
                pose, packedLight, packedOverlay, IDLE_RETENTION);
        if (result.drawn()) {
            ExternalGunAppearance.Snapshot selected = result.usedPrevious() ? previous : appearance;
            return new StaticGunRenderResult(StaticGunRenderResult.Status.SUBMITTED, selected.useLod());
        }
        // 正常准备或可能部分绘制的调用继续接管；明确不支持／已禁用时走旧路径。
        return result == MeshDrawResult.UNSUPPORTED ? StaticGunRenderResult.UNSUPPORTED : StaticGunRenderResult.PENDING;
    }
}
