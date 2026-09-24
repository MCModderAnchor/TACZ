package com.tacz.guns.client.renderer.item;

import com.github.mcmodderanchor.simplebedrockmodel.v2.client.world.ImmediateStaticMeshRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.renderer.block.DisplayGunGeometry;
import com.tacz.guns.client.renderer.external.ExternalGunAppearance;
import com.tacz.guns.config.client.RenderConfig;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Immediate VBO path for static third-person and ground gun appearances. */
public class ExternalGunItemVboRenderer {
    private static boolean wasEnabled;

    private ExternalGunItemVboRenderer() {}

    /** The fixed display transform differs between ground and hand, so each has its own static VBO. */
    private record ItemGeometryKey(ExternalGunAppearance.GeometryKey appearance, ItemDisplayContext context) {}

    public static ImmediateStaticMeshRenderer.DrawResult tryRender(ItemStack stack, ItemDisplayContext context,
                                                                    boolean useLod, PoseStack pose,
                                                                    MultiBufferSource buffers,
                                                                    int packedLight, int packedOverlay) {
        if (!RenderConfig.EXTERNAL_ITEM_GUN_VBO.get()) {
            if (wasEnabled) ImmediateStaticMeshRenderer.clear();
            wasEnabled = false;
            return ImmediateStaticMeshRenderer.DrawResult.UNSUPPORTED;
        }
        wasEnabled = true;
        if (ItemPreviewRenderContext.isPreview()
                || context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND && context != ItemDisplayContext.GROUND) {
            return ImmediateStaticMeshRenderer.DrawResult.UNSUPPORTED;
        }
        ExternalGunAppearance.Variants appearances = ExternalGunAppearance.resolve(stack, false);
        if (appearances == null) return ImmediateStaticMeshRenderer.DrawResult.UNSUPPORTED;
        ExternalGunAppearance.Snapshot appearance = appearances.select(useLod);
        return ImmediateStaticMeshRenderer.tryDraw(new ItemGeometryKey(appearance.key(), context),
                collector -> {
                    return DisplayGunGeometry.capture(appearance, context, collector);
                },
                pose, packedLight, packedOverlay, () -> {
                });
    }
}
