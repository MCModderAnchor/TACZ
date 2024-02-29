package com.tac.guns.client.resource.cache.data;

import com.tac.guns.client.animation.AnimationController;
import com.tac.guns.client.animation.Animations;
import com.tac.guns.client.animation.gltf.AnimationStructure;
import com.tac.guns.client.model.BedrockGunModel;
import com.tac.guns.client.resource.cache.ClientAssetManager;
import com.tac.guns.client.resource.pojo.ClientGunIndexPOJO;
import com.tac.guns.client.resource.pojo.display.GunDisplay;
import com.tac.guns.client.resource.pojo.display.GunModelTexture;
import com.tac.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tac.guns.client.resource.pojo.model.BedrockVersion;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Optional;

public class ClientGunIndex {
    private static final String DEFAULT_TEXTURE_NAME = "default";

    private String name;
    private String tooltip;
    private BedrockGunModel gunModel;
    private AnimationController controller;
    private Map<String, ResourceLocation> sounds;

    public ClientGunIndex(ClientGunIndexPOJO gunIndexPOJO) {
        this.name = gunIndexPOJO.getName();
        this.tooltip = gunIndexPOJO.getTooltip();
        GunDisplay display = ClientAssetManager.INSTANCE.getGunDisplay(gunIndexPOJO.getDisplay());

        // FIXME：这里的读取没有做判断
        // FIXME：如果某些地方读取，应当抛出错误，或者给予默认值
        // 加载材质
        BedrockModelPOJO modelPOJO = ClientAssetManager.INSTANCE.getModels(display.getModelLocation());
        // 检查默认材质是否存在，并创建默认的RenderType
        Optional<GunModelTexture> defaultOptional = display.getModelTextures().stream().filter(texture -> DEFAULT_TEXTURE_NAME.equals(texture.getId())).findAny();
        if (defaultOptional.isPresent()) {
            RenderType renderType = RenderType.itemEntityTranslucentCull(defaultOptional.get().getLocation());
            // 先判断是不是 1.10.0 版本基岩版模型文件
            if (modelPOJO.getFormatVersion().equals(BedrockVersion.LEGACY.getVersion()) && modelPOJO.getGeometryModelLegacy() != null) {
                this.gunModel = new BedrockGunModel(modelPOJO, BedrockVersion.LEGACY, renderType);
            }
            // 判定是不是 1.12.0 版本基岩版模型文件
            if (modelPOJO.getFormatVersion().equals(BedrockVersion.NEW.getVersion()) && modelPOJO.getGeometryModelNew() != null) {
                this.gunModel = new BedrockGunModel(modelPOJO, BedrockVersion.NEW, renderType);
            }
        }

        // 加载动画
        if (this.gunModel != null) {
            AnimationStructure animations = ClientAssetManager.INSTANCE.getAnimations(display.getAnimationLocation());
            this.controller = Animations.createControllerFromGltf(animations, this.gunModel);
        }

        // 加载声音
        this.sounds = display.getSounds();
    }

    public String getName() {
        return name;
    }

    public String getTooltip() {
        return tooltip;
    }

    public BedrockGunModel getGunModel() {
        return gunModel;
    }

    public AnimationController getController() {
        return controller;
    }

    public ResourceLocation getSounds(String name) {
        return sounds.get(name);
    }
}