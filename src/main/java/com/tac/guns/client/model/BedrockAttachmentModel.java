package com.tac.guns.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tac.guns.client.model.bedrock.BedrockPart;
import com.tac.guns.client.model.bedrock.ModelRendererWrapper;
import com.tac.guns.client.render.item.IOverrideModel;
import com.tac.guns.client.resource.model.bedrock.BedrockVersion;
import com.tac.guns.client.resource.model.bedrock.pojo.BedrockModelPOJO;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class BedrockAttachmentModel extends BedrockAnimatedModel implements IOverrideModel {
    public static final String SCOPE_VIEW_NODE = "scope_view";
    protected final List<BedrockPart> scopeViewPath = new ArrayList<>();

    public BedrockAttachmentModel(BedrockModelPOJO pojo, BedrockVersion version, RenderType renderType) {
        super(pojo, version, renderType);
        {
            ModelRendererWrapper rendererWrapper = modelMap.get(SCOPE_VIEW_NODE);
            if (rendererWrapper != null) {
                BedrockPart it = rendererWrapper.getModelRenderer();
                Stack<BedrockPart> stack = new Stack<>();
                do {
                    stack.push(it);
                    it = it.getParent();
                } while (it != null);
                while (!stack.isEmpty()) {
                    it = stack.pop();
                    scopeViewPath.add(it);
                }
            }
        }
        this.setFunctionalRenderer(SCOPE_VIEW_NODE, bedrockPart -> {
            bedrockPart.visible = false;
            return null;
        });
    }

    @Override
    public void render(float partialTicks, ItemTransforms.TransformType transformType, ItemStack stack, ItemStack parent, LivingEntity entity, PoseStack matrixStack, MultiBufferSource buffer, int light, int overlay) {
        render(transformType, matrixStack, buffer, light, overlay);
    }
}