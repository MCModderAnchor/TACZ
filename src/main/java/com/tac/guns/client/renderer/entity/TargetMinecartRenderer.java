package com.tac.guns.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.tac.guns.client.renderer.block.TargetRenderer;
import com.tac.guns.entity.TargetMinecart;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MinecartRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TargetMinecartRenderer extends MinecartRenderer<TargetMinecart> {
   public TargetMinecartRenderer(EntityRendererProvider.Context ctx) {
      super(ctx, ModelLayers.TNT_MINECART);
   }

   @Override
   protected void renderMinecartContents(TargetMinecart pEntity, float pPartialTicks, BlockState pState, PoseStack stack, MultiBufferSource pBuffer, int pPackedLight) {
      stack.pushPose();
      {
         super.renderMinecartContents(pEntity, pPartialTicks, pState, stack, pBuffer, pPackedLight);
      }
      stack.popPose();

      stack.pushPose();
      {
         stack.translate(0.5, 1.5, 0.5);
         stack.mulPose(Vector3f.ZN.rotationDegrees(180));
         stack.mulPose(Vector3f.YN.rotationDegrees(90));

         RenderType renderType = RenderType.entityTranslucent(TargetRenderer.getTextureLocation());
         if (TargetRenderer.getModel() != null) {
            TargetRenderer.getModel().render(stack, ItemTransforms.TransformType.NONE, renderType, pPackedLight, OverlayTexture.NO_OVERLAY);
         }
      }
      stack.popPose();
   }
}