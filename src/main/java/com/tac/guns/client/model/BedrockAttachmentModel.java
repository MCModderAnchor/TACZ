package com.tac.guns.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tac.guns.client.model.bedrock.BedrockPart;
import com.tac.guns.client.model.bedrock.ModelRendererWrapper;
import com.tac.guns.client.resource.pojo.TransformScale;
import com.tac.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tac.guns.client.resource.pojo.model.BedrockVersion;
import net.minecraft.client.renderer.block.model.ItemTransforms;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class BedrockAttachmentModel extends BedrockAnimatedModel {
    public static final String SCOPE_VIEW_NODE = "scope_view";
    protected final List<BedrockPart> scopeViewPath = new ArrayList<>();

    public BedrockAttachmentModel(BedrockModelPOJO pojo, BedrockVersion version) {
        super(pojo, version);
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
}