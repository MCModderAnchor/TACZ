package com.tac.guns.client.resource.index;

import com.tac.guns.client.model.BedrockAttachmentModel;
import com.tac.guns.client.resource.ClientAssetManager;
import com.tac.guns.client.resource.pojo.display.attachment.AttachmentDisplay;
import com.tac.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tac.guns.client.resource.pojo.model.BedrockVersion;
import com.tac.guns.resource.pojo.AttachmentIndexPOJO;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ClientAttachmentIndex {
    private String name;
    private BedrockAttachmentModel attachmentModel;
    private ResourceLocation modelTexture;
    private ResourceLocation slotTexture;

    private ClientAttachmentIndex() {
    }

    public static ClientAttachmentIndex getInstance(AttachmentIndexPOJO indexPOJO) throws IllegalArgumentException {
        ClientAttachmentIndex index = new ClientAttachmentIndex();
        checkIndex(indexPOJO, index);
        AttachmentDisplay display = checkDisplay(indexPOJO);
        checkName(indexPOJO, index);
        checkSlotTexture(display, index);
        checkTextureAndModel(display, index);
        return index;
    }

    private static void checkIndex(AttachmentIndexPOJO attachmentIndexPOJO, ClientAttachmentIndex index) {
        if (attachmentIndexPOJO == null) {
            throw new IllegalArgumentException("index object file is empty");
        }
    }

    @NotNull
    private static AttachmentDisplay checkDisplay(AttachmentIndexPOJO indexPOJO) {
        ResourceLocation pojoDisplay = indexPOJO.getDisplay();
        if (pojoDisplay == null) {
            throw new IllegalArgumentException("index object missing display field");
        }
        AttachmentDisplay display = ClientAssetManager.INSTANCE.getAttachmentDisplay(pojoDisplay);
        if (display == null) {
            throw new IllegalArgumentException("there is no corresponding display file");
        }
        return display;
    }

    private static void checkName(AttachmentIndexPOJO indexPOJO, ClientAttachmentIndex index) {
        index.name = indexPOJO.getName();
        if (StringUtils.isBlank(index.name)) {
            index.name = "custom.tac.error.no_name";
        }
    }

    private static void checkSlotTexture(AttachmentDisplay display, ClientAttachmentIndex index) {
        index.slotTexture = Objects.requireNonNullElseGet(display.getSlotTextureLocation(), MissingTextureAtlasSprite::getLocation);
    }

    private static void checkTextureAndModel(AttachmentDisplay display, ClientAttachmentIndex index) {
        // 检查模型
        ResourceLocation modelLocation = display.getModel();
        if (modelLocation == null) {
            throw new IllegalArgumentException("display object missing model field");
        }
        BedrockModelPOJO modelPOJO = ClientAssetManager.INSTANCE.getModels(modelLocation);
        if (modelPOJO == null) {
            throw new IllegalArgumentException("there is no corresponding model file");
        }
        // 检查默认材质是否存在
        ResourceLocation textureLocation = display.getTexture();
        if (textureLocation == null) {
            throw new IllegalArgumentException("missing default texture");
        }
        index.modelTexture = textureLocation;
        // 先判断是不是 1.10.0 版本基岩版模型文件
        if (modelPOJO.getFormatVersion().equals(BedrockVersion.LEGACY.getVersion()) && modelPOJO.getGeometryModelLegacy() != null) {
            index.attachmentModel = new BedrockAttachmentModel(modelPOJO, BedrockVersion.LEGACY);
        }
        // 判定是不是 1.12.0 版本基岩版模型文件
        if (modelPOJO.getFormatVersion().equals(BedrockVersion.NEW.getVersion()) && modelPOJO.getGeometryModelNew() != null) {
            index.attachmentModel = new BedrockAttachmentModel(modelPOJO, BedrockVersion.NEW);
        }

        if (index.attachmentModel == null) {
            throw new IllegalArgumentException("there is no model data in the model file");
        }
    }

    public String getName() {
        return name;
    }

    public BedrockAttachmentModel getAttachmentModel() {
        return attachmentModel;
    }

    public ResourceLocation getModelTexture() {
        return modelTexture;
    }

    public ResourceLocation getSlotTexture() {
        return slotTexture;
    }
}
