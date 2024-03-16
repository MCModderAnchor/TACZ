package com.tac.guns.client.model;

import com.tac.guns.client.model.bedrock.BedrockModel;
import com.tac.guns.client.model.bedrock.BedrockPart;
import com.tac.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tac.guns.client.resource.pojo.model.BedrockVersion;

import javax.annotation.Nullable;
import java.util.List;

public class BedrockAmmoModel extends BedrockModel {
    public static final String FIXED_ORIGIN_NODE = "fixed";
    public static final String GROUND_ORIGIN_NODE = "ground";

    // 展示框渲染原点定位组的路径
    protected @Nullable List<BedrockPart> fixedOriginPath;
    // 地面实体渲染原点定位组的路径
    protected @Nullable List<BedrockPart> groundOriginPath;

    public BedrockAmmoModel(BedrockModelPOJO pojo, BedrockVersion version) {
        super(pojo, version);
        fixedOriginPath = getPath(modelMap.get(FIXED_ORIGIN_NODE));
        groundOriginPath = getPath(modelMap.get(GROUND_ORIGIN_NODE));
    }

    @Nullable
    public List<BedrockPart> getFixedOriginPath() {
        return fixedOriginPath;
    }

    @Nullable
    public List<BedrockPart> getGroundOriginPath() {
        return groundOriginPath;
    }
}
