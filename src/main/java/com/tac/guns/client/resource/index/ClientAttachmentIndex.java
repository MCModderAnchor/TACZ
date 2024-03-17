package com.tac.guns.client.resource.index;

import com.google.common.collect.Maps;
import com.tac.guns.client.model.BedrockAttachmentModel;
import com.tac.guns.client.resource.ClientAssetManager;
import com.tac.guns.client.resource.pojo.display.attachment.AttachmentDisplay;
import com.tac.guns.client.resource.pojo.skin.attachment.AttachmentSkin;
import com.tac.guns.resource.CommonAssetManager;
import com.tac.guns.resource.pojo.AttachmentIndexPOJO;
import com.tac.guns.resource.pojo.data.attachment.AttachmentData;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Objects;

public class ClientAttachmentIndex {
    private final Map<ResourceLocation, ClientAttachmentSkinIndex> skinIndexMap = Maps.newHashMap();
    private String name;
    private BedrockAttachmentModel attachmentModel;
    private ResourceLocation modelTexture;
    private ResourceLocation slotTexture;
    private AttachmentData data;
    private float fov = 70.0f;
    private float @Nullable [] zoom;

    private ClientAttachmentIndex() {
    }

    public static ClientAttachmentIndex getInstance(ResourceLocation registryName, AttachmentIndexPOJO indexPOJO) throws IllegalArgumentException {
        ClientAttachmentIndex index = new ClientAttachmentIndex();
        checkIndex(indexPOJO, index);
        AttachmentDisplay display = checkDisplay(indexPOJO, index);
        checkData(indexPOJO, index);
        checkName(indexPOJO, index);
        checkSlotTexture(display, index);
        checkTextureAndModel(display, index);
        checkSkins(registryName, index);
        return index;
    }

    private static void checkIndex(AttachmentIndexPOJO attachmentIndexPOJO, ClientAttachmentIndex index) {
        if (attachmentIndexPOJO == null) {
            throw new IllegalArgumentException("index object file is empty");
        }
    }

    @Nonnull
    private static AttachmentDisplay checkDisplay(AttachmentIndexPOJO indexPOJO, ClientAttachmentIndex index) {
        ResourceLocation pojoDisplay = indexPOJO.getDisplay();
        if (pojoDisplay == null) {
            throw new IllegalArgumentException("index object missing display field");
        }
        AttachmentDisplay display = ClientAssetManager.INSTANCE.getAttachmentDisplay(pojoDisplay);
        if (display == null) {
            throw new IllegalArgumentException("there is no corresponding display file");
        }
        if (display.getFov() <= 0) {
            throw new IllegalArgumentException("fov must > 0");
        }
        index.fov = display.getFov();
        index.zoom = display.getZoom();
        return display;
    }

    private static void checkData(AttachmentIndexPOJO indexPOJO, ClientAttachmentIndex index) {
        ResourceLocation dataId = indexPOJO.getData();
        if (dataId == null) {
            throw new IllegalArgumentException("index object missing pojoData field");
        }
        AttachmentData data = CommonAssetManager.INSTANCE.getAttachmentData(dataId);
        if (data == null) {
            throw new IllegalArgumentException("there is no corresponding data file");
        }
        // 剩下的不需要校验了，Common的读取逻辑中已经校验过了
        index.data = data;
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
        index.attachmentModel = ClientAssetManager.INSTANCE.getOrLoadAttachmentModel(modelLocation);
        if (index.attachmentModel == null) {
            throw new IllegalArgumentException("there is no model data in the model file");
        }
        // 检查默认材质
        ResourceLocation textureLocation = display.getTexture();
        if (textureLocation == null) {
            throw new IllegalArgumentException("missing default texture");
        }
        index.modelTexture = textureLocation;
    }

    private static void checkSkins(ResourceLocation registryName, ClientAttachmentIndex index) {
        Map<ResourceLocation, AttachmentSkin> skins = ClientAssetManager.INSTANCE.getAttachmentSkins(registryName);
        if (skins != null) {
            for (Map.Entry<ResourceLocation, AttachmentSkin> entry : skins.entrySet()) {
                ClientAttachmentSkinIndex skinIndex = ClientAttachmentSkinIndex.getInstance(entry.getValue());
                index.skinIndexMap.put(entry.getKey(), skinIndex);
            }
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

    public float getFov() {
        return fov;
    }

    public float @Nullable [] getZoom() {
        return zoom;
    }

    public AttachmentData getData() {
        return data;
    }

    @Nullable
    public ClientAttachmentSkinIndex getSkinIndex(@Nullable ResourceLocation skinName) {
        if (skinName == null) {
            return null;
        }
        return skinIndexMap.get(skinName);
    }
}
