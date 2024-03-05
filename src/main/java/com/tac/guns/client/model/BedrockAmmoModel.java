package com.tac.guns.client.model;

import com.tac.guns.client.model.bedrock.BedrockModel;
import com.tac.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tac.guns.client.resource.pojo.model.BedrockVersion;
import net.minecraft.client.renderer.RenderType;

public class BedrockAmmoModel extends BedrockModel {
    public BedrockAmmoModel(BedrockModelPOJO pojo, BedrockVersion version, RenderType renderType) {
        super(pojo, version, renderType);
    }
}
