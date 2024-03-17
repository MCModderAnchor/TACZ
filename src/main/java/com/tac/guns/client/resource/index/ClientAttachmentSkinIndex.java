package com.tac.guns.client.resource.index;

import com.tac.guns.client.model.BedrockAttachmentModel;
import com.tac.guns.client.resource.ClientAssetManager;
import com.tac.guns.client.resource.pojo.skin.attachment.AttachmentSkin;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.StringUtils;

public class ClientAttachmentSkinIndex {
    private BedrockAttachmentModel model;
    private ResourceLocation texture;
    private String name;

    private ClientAttachmentSkinIndex() {
    }

    public static ClientAttachmentSkinIndex getInstance(AttachmentSkin skinPojo) {
        ClientAttachmentSkinIndex index = new ClientAttachmentSkinIndex();
        checkIndex(skinPojo, index);
        checkTextureAndModel(skinPojo, index);
        return index;
    }

    private static void checkIndex(AttachmentSkin skinPojo, ClientAttachmentSkinIndex index) {
        if (skinPojo == null) {
            throw new IllegalArgumentException("skin index file is empty");
        }
    }

    private static void checkName(AttachmentSkin skinPojo, ClientAttachmentSkinIndex index) {
        index.name = skinPojo.getName();
        if (StringUtils.isBlank(index.name)) {
            index.name = "custom.tac.error.no_name";
        }
    }

    private static void checkTextureAndModel(AttachmentSkin skinPojo, ClientAttachmentSkinIndex index) {
        // 检查模型
        ResourceLocation modelLocation = skinPojo.getModel();
        if (modelLocation == null) {
            throw new IllegalArgumentException("display object missing model field");
        }
        index.model = ClientAssetManager.INSTANCE.getOrLoadAttachmentModel(modelLocation);
        if (index.model == null) {
            throw new IllegalArgumentException("there is no model data in the model file");
        }
        // 检查默认材质
        ResourceLocation textureLocation = skinPojo.getTexture();
        if (textureLocation == null) {
            throw new IllegalArgumentException("missing default texture");
        }
        index.texture = textureLocation;
    }

    public BedrockAttachmentModel getModel() {
        return model;
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    public String getName() {
        return name;
    }
}
