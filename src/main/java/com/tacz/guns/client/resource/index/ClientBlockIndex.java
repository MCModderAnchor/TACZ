package com.tacz.guns.client.resource.index;

import com.google.common.base.Preconditions;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.resource.ClientAssetsManager;
import com.tacz.guns.client.resource.pojo.display.block.BlockDisplay;
import com.tacz.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tacz.guns.client.resource.pojo.model.BedrockVersion;
import com.tacz.guns.resource.pojo.BlockIndexPOJO;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.StringUtils;

public class ClientBlockIndex {
    private BedrockModel model;
    private ResourceLocation texture;
    private String name;
    private ItemTransforms transforms;
    private String tooltipKey;

    public static ClientBlockIndex getInstance(BlockIndexPOJO pojo) {
        ClientBlockIndex index = new ClientBlockIndex();
        checkIndex(pojo, index);
        BlockDisplay display = checkDisplay(pojo, index);
        checkModel(display, index);
        checkName(pojo, index);
        checkTransforms(display, index);
        return index;
    }

    private static void checkIndex(BlockIndexPOJO blockIndexPOJO, ClientBlockIndex index) {
        Preconditions.checkArgument(blockIndexPOJO != null, "index object file is empty");
        index.tooltipKey = blockIndexPOJO.getTooltip();
    }

    private static void checkName(BlockIndexPOJO blockIndexPOJO, ClientBlockIndex index) {
        index.name = blockIndexPOJO.getName();
        if (StringUtils.isBlank(index.name)) {
            index.name = "custom.tacz.error.no_name";
        }
    }

    private static BlockDisplay checkDisplay(BlockIndexPOJO pojo, ClientBlockIndex index) {
        ResourceLocation display = pojo.getDisplay();
        Preconditions.checkArgument(display != null, "index object missing display field");
        BlockDisplay blockDisplay = ClientAssetsManager.INSTANCE.getBlockDisplay(pojo.getDisplay());
        Preconditions.checkArgument(blockDisplay != null, "there is no corresponding display file");
        return blockDisplay;
    }

    private static void checkModel(BlockDisplay display, ClientBlockIndex index) {
        ResourceLocation modelLocation = display.getModelLocation();
        Preconditions.checkArgument(modelLocation != null, "display object missing model field");
        BedrockModelPOJO modelPOJO = ClientAssetsManager.INSTANCE.getBedrockModelPOJO(modelLocation);
        Preconditions.checkArgument(modelPOJO != null, "there is no corresponding model file");

        // 先判断是不是 1.10.0 版本基岩版模型文件
        if (BedrockVersion.isLegacyVersion(modelPOJO) && modelPOJO.getGeometryModelLegacy() != null) {
            index.model = new BedrockModel(modelPOJO, BedrockVersion.LEGACY);
        }
        // 判定是不是 1.12.0 版本基岩版模型文件
        if (BedrockVersion.isNewVersion(modelPOJO) && modelPOJO.getGeometryModelNew() != null) {
            index.model = new BedrockModel(modelPOJO, BedrockVersion.NEW);
        }
        Preconditions.checkArgument(index.model != null, "there is no model data in the model file");

        ResourceLocation textureLocation = display.getModelTexture();
        Preconditions.checkArgument(textureLocation != null, "missing default texture");
        index.texture = display.getModelTexture();
    }

    private static void checkTransforms(BlockDisplay display, ClientBlockIndex index) {
        Preconditions.checkArgument(display.getTransforms() != null, "missing transforms");
        index.transforms = display.getTransforms();
    }

    public BedrockModel getModel() {
        return model;
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    public String getName() {
        return name;
    }

    public ItemTransforms getTransforms() {
        return transforms;
    }

    public String getTooltipKey() {
        return tooltipKey;
    }
}
