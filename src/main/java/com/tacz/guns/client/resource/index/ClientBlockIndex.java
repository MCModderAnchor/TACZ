package com.tacz.guns.client.resource.index;

import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.resource.ClientAssetManager;
import com.tacz.guns.client.resource.pojo.display.block.BlockDisplay;
import com.tacz.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tacz.guns.client.resource.pojo.model.BedrockVersion;
import com.tacz.guns.resource.pojo.BlockIndexPOJO;

public class ClientBlockIndex {
    private BedrockModel model;

    public static ClientBlockIndex getInstance(BlockIndexPOJO pojo) {
        ClientBlockIndex index = new ClientBlockIndex();
        BlockDisplay display = ClientAssetManager.INSTANCE.getBlockDisplays(pojo.getDisplay());
        BedrockModelPOJO modelPOJO = ClientAssetManager.INSTANCE.getModels(display.getModelLocation());
        // 先判断是不是 1.10.0 版本基岩版模型文件
        if (BedrockVersion.isLegacyVersion(modelPOJO) && modelPOJO.getGeometryModelLegacy() != null) {
            index.model = new BedrockModel(modelPOJO, BedrockVersion.LEGACY);
        }
        // 判定是不是 1.12.0 版本基岩版模型文件
        if (BedrockVersion.isNewVersion(modelPOJO) && modelPOJO.getGeometryModelNew() != null) {
            index.model = new BedrockModel(modelPOJO, BedrockVersion.NEW);
        }
        return index;
    }

    public BedrockModel getModel() {
        return model;
    }
}
