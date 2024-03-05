package com.tac.guns.client.resource.index;

import com.tac.guns.client.model.BedrockAmmoModel;
import com.tac.guns.client.resource.ClientAssetManager;
import com.tac.guns.client.resource.pojo.display.AmmoDisplay;
import com.tac.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tac.guns.client.resource.pojo.model.BedrockVersion;
import com.tac.guns.resource.pojo.AmmoIndexPOJO;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ClientAmmoIndex {
    private String name;
    private BedrockAmmoModel ammoModel;
    private RenderType slotRenderType;
    private int stackSize;

    private ClientAmmoIndex() {
    }

    public static ClientAmmoIndex getInstance(AmmoIndexPOJO clientPojo) throws IllegalArgumentException {
        ClientAmmoIndex index = new ClientAmmoIndex();
        AmmoDisplay display = checkDisplay(clientPojo);
        checkName(clientPojo, index);
        checkTextureAndModel(display, index);
        checkSlotTexture(display, index);
        checkStackSize(clientPojo, index);
        return index;
    }

    private static void checkName(AmmoIndexPOJO ammoIndexPOJO, ClientAmmoIndex index) {
        index.name = ammoIndexPOJO.getName();
        if (StringUtils.isBlank(index.name)) {
            index.name = "custom.tac.gun.error.no_name";
        }
    }

    @NotNull
    private static AmmoDisplay checkDisplay(AmmoIndexPOJO ammoIndexPOJO) {
        ResourceLocation pojoDisplay = ammoIndexPOJO.getDisplay();
        if (pojoDisplay == null) {
            throw new IllegalArgumentException("index object missing display field");
        }
        AmmoDisplay display = ClientAssetManager.INSTANCE.getAmmoDisplay(pojoDisplay);
        if (display == null) {
            throw new IllegalArgumentException("there is no corresponding display file");
        }
        return display;
    }

    private static void checkTextureAndModel(AmmoDisplay display, ClientAmmoIndex index) {
        // 检查模型
        ResourceLocation modelLocation = display.getModelLocation();
        if (modelLocation == null) {
            throw new IllegalArgumentException("display object missing model field");
        }
        BedrockModelPOJO modelPOJO = ClientAssetManager.INSTANCE.getModels(modelLocation);
        if (modelPOJO == null) {
            throw new IllegalArgumentException("there is no corresponding model file");
        }
        // 检查材质
        ResourceLocation texture = display.getModelTexture();
        if (texture == null) {
            throw new IllegalArgumentException("display object missing textures field");
        }
        // 创建默认的 RenderType
        RenderType renderType = RenderType.itemEntityTranslucentCull(texture);
        // 先判断是不是 1.10.0 版本基岩版模型文件
        if (modelPOJO.getFormatVersion().equals(BedrockVersion.LEGACY.getVersion()) && modelPOJO.getGeometryModelLegacy() != null) {
            index.ammoModel = new BedrockAmmoModel(modelPOJO, BedrockVersion.LEGACY, renderType);
        }
        // 判定是不是 1.12.0 版本基岩版模型文件
        if (modelPOJO.getFormatVersion().equals(BedrockVersion.NEW.getVersion()) && modelPOJO.getGeometryModelNew() != null) {
            index.ammoModel = new BedrockAmmoModel(modelPOJO, BedrockVersion.NEW, renderType);
        }
        if (index.ammoModel == null) {
            throw new IllegalArgumentException("there is no model data in the model file");
        }
    }

    private static void checkSlotTexture(AmmoDisplay display, ClientAmmoIndex index) {
        // 加载 GUI 内枪械图标
        ResourceLocation slotTexture = Objects.requireNonNullElseGet(display.getSlotTextureLocation(), MissingTextureAtlasSprite::getLocation);
        index.slotRenderType = RenderType.entityTranslucent(slotTexture);
    }

    private static void checkStackSize(AmmoIndexPOJO clientPojo, ClientAmmoIndex index) {
        index.stackSize = Math.max(clientPojo.getStackSize(), 1);
    }

    public String getName() {
        return name;
    }

    public BedrockAmmoModel getAmmoModel() {
        return ammoModel;
    }

    public RenderType getSlotRenderType() {
        return slotRenderType;
    }

    public int getStackSize() {
        return stackSize;
    }
}
