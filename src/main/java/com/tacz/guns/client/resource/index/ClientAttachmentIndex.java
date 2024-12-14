package com.tacz.guns.client.resource.index;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.tacz.guns.client.model.BedrockAttachmentModel;
import com.tacz.guns.client.resource.ClientAssetsManager;
import com.tacz.guns.client.resource.pojo.display.attachment.AttachmentDisplay;
import com.tacz.guns.client.resource.pojo.display.attachment.AttachmentLod;
import com.tacz.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tacz.guns.client.resource.pojo.model.BedrockVersion;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.pojo.AttachmentIndexPOJO;
import com.tacz.guns.resource.pojo.data.attachment.AttachmentData;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Objects;

public class ClientAttachmentIndex {
    private final Map<ResourceLocation, ClientAttachmentSkinIndex> skinIndexMap = Maps.newHashMap();
    private String name;
    private @Nullable BedrockAttachmentModel attachmentModel;
    private @Nullable ResourceLocation modelTexture;
    private @Nullable Pair<BedrockAttachmentModel, ResourceLocation> lodModel;
    private ResourceLocation slotTexture;
    private AttachmentData data;
    private float fov = 70.0f;
    private float @Nullable [] zoom;
    private boolean isScope;
    private boolean isSight;
    private boolean showMuzzle;
    private @Nullable String adapterNodeName;
    private @Nullable String tooltipKey;
    private Map<String, ResourceLocation> sounds;

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
        checkLod(display, index);
//        checkSkins(registryName, index);
        checkSounds(display, index);
        return index;
    }

    private static void checkIndex(AttachmentIndexPOJO attachmentIndexPOJO, ClientAttachmentIndex index) {
        Preconditions.checkArgument(attachmentIndexPOJO != null, "index object file is empty");
        index.tooltipKey = attachmentIndexPOJO.getTooltip();
    }

    @Nonnull
    private static AttachmentDisplay checkDisplay(AttachmentIndexPOJO indexPOJO, ClientAttachmentIndex index) {
        ResourceLocation pojoDisplay = indexPOJO.getDisplay();
        Preconditions.checkArgument(pojoDisplay != null, "index object missing display field");
        AttachmentDisplay display = ClientAssetsManager.INSTANCE.getAttachmentDisplay(pojoDisplay);
        Preconditions.checkArgument(display != null, "there is no corresponding display file");
        Preconditions.checkArgument(display.getFov() > 0, "fov must > 0");
        index.fov = display.getFov();
        index.zoom = display.getZoom();
        if (index.zoom != null) {
            for (int i = 0; i < index.zoom.length; i++) {
                if (index.zoom[i] < 1) {
                    throw new IllegalArgumentException("zoom must >= 1");
                }
            }
        }
        index.isScope = display.isScope();
        index.isSight = display.isSight();
        index.adapterNodeName = display.getAdapterNodeName();
        index.showMuzzle = display.isShowMuzzle();
        return display;
    }

    private static void checkData(AttachmentIndexPOJO indexPOJO, ClientAttachmentIndex index) {
        ResourceLocation dataId = indexPOJO.getData();
        Preconditions.checkArgument(dataId != null, "index object missing pojoData field");
        AttachmentData data = CommonAssetsManager.get().getAttachmentData(dataId);
        Preconditions.checkArgument(data != null, "there is no corresponding data file");
        // 剩下的不需要校验了，Common的读取逻辑中已经校验过了
        index.data = data;
    }

    private static void checkName(AttachmentIndexPOJO indexPOJO, ClientAttachmentIndex index) {
        index.name = indexPOJO.getName();
        if (StringUtils.isBlank(index.name)) {
            index.name = "custom.tacz.error.no_name";
        }
    }

    private static void checkSlotTexture(AttachmentDisplay display, ClientAttachmentIndex index) {
        index.slotTexture = Objects.requireNonNullElseGet(display.getSlotTextureLocation(), MissingTextureAtlasSprite::getLocation);
    }

    private static void checkTextureAndModel(AttachmentDisplay display, ClientAttachmentIndex index) {
        // 不检查模型/材质是否为 null，模型/材质可以为 null
        index.attachmentModel = getOrLoadAttachmentModel(display.getModel());
        if (index.attachmentModel != null) {
            index.attachmentModel.setIsScope(display.isScope());
            index.attachmentModel.setIsSight(display.isSight());
        }
        index.modelTexture = display.getTexture();
    }

    @Nullable
    public static BedrockAttachmentModel getOrLoadAttachmentModel(@Nullable ResourceLocation modelLocation) {
        if (modelLocation == null) {
            return null;
        }
        BedrockModelPOJO modelPOJO = ClientAssetsManager.INSTANCE.getBedrockModelPOJO(modelLocation);
        if (modelPOJO == null) {
            return null;
        }
        return getAttachmentModel(modelPOJO);
    }

    @Nullable
    public static BedrockAttachmentModel getAttachmentModel(BedrockModelPOJO modelPOJO) {
        BedrockAttachmentModel attachmentModel = null;
        // 先判断是不是 1.10.0 版本基岩版模型文件
        if (BedrockVersion.isLegacyVersion(modelPOJO) && modelPOJO.getGeometryModelLegacy() != null) {
            attachmentModel = new BedrockAttachmentModel(modelPOJO, BedrockVersion.LEGACY);
        }
        // 判定是不是 1.12.0 版本基岩版模型文件
        if (BedrockVersion.isNewVersion(modelPOJO) && modelPOJO.getGeometryModelNew() != null) {
            attachmentModel = new BedrockAttachmentModel(modelPOJO, BedrockVersion.NEW);
        }
        return attachmentModel;
    }

    private static void checkLod(AttachmentDisplay display, ClientAttachmentIndex index) {
        AttachmentLod gunLod = display.getAttachmentLod();
        if (gunLod != null) {
            ResourceLocation texture = gunLod.getModelTexture();
            if (gunLod.getModelLocation() == null) {
                return;
            }
            if (texture == null) {
                return;
            }
            BedrockModelPOJO modelPOJO = ClientAssetsManager.INSTANCE.getBedrockModelPOJO(gunLod.getModelLocation());
            if (modelPOJO == null) {
                return;
            }
            // 先判断是不是 1.10.0 版本基岩版模型文件
            if (BedrockVersion.isLegacyVersion(modelPOJO) && modelPOJO.getGeometryModelLegacy() != null) {
                BedrockAttachmentModel model = new BedrockAttachmentModel(modelPOJO, BedrockVersion.LEGACY);
                index.lodModel = Pair.of(model, texture);
            }
            // 判定是不是 1.12.0 版本基岩版模型文件
            if (BedrockVersion.isNewVersion(modelPOJO) && modelPOJO.getGeometryModelNew() != null) {
                BedrockAttachmentModel model = new BedrockAttachmentModel(modelPOJO, BedrockVersion.NEW);
                index.lodModel = Pair.of(model, texture);
            }
        }
    }

//    private static void checkSkins(ResourceLocation registryName, ClientAttachmentIndex index) {
//        Map<ResourceLocation, AttachmentSkin> skins = ClientAssetManager.INSTANCE.getAttachmentSkins(registryName);
//        if (skins != null) {
//            for (Map.Entry<ResourceLocation, AttachmentSkin> entry : skins.entrySet()) {
//                ClientAttachmentSkinIndex skinIndex = ClientAttachmentSkinIndex.getInstance(entry.getValue());
//                index.skinIndexMap.put(entry.getKey(), skinIndex);
//            }
//        }
//    }

    private static void checkSounds(AttachmentDisplay display, ClientAttachmentIndex index) {
        Map<String, ResourceLocation> displaySounds = display.getSounds();
        if (displaySounds == null) {
            index.sounds = Maps.newHashMap();
            return;
        }
        index.sounds = displaySounds;
    }

    public String getName() {
        return name;
    }

    @Nullable
    public String getTooltipKey() {
        return tooltipKey;
    }

    @Nullable
    public BedrockAttachmentModel getAttachmentModel() {
        return attachmentModel;
    }

    @Nullable
    public ResourceLocation getModelTexture() {
        return modelTexture;
    }

    @Nullable
    public Pair<BedrockAttachmentModel, ResourceLocation> getLodModel() {
        return lodModel;
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

    public boolean isScope() {
        return isScope;
    }

    public boolean isSight() {
        return isSight;
    }

    @Nullable
    public String getAdapterNodeName() {
        return adapterNodeName;
    }

    public boolean isShowMuzzle() {
        return showMuzzle;
    }

    public Map<String, ResourceLocation> getSounds() {
        return sounds;
    }
}
