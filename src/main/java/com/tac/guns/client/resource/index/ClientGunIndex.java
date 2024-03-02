package com.tac.guns.client.resource.index;

import com.tac.guns.client.animation.AnimationController;
import com.tac.guns.client.animation.Animations;
import com.tac.guns.client.animation.gltf.AnimationStructure;
import com.tac.guns.client.animation.internal.GunAnimationStateMachine;
import com.tac.guns.client.model.BedrockGunModel;
import com.tac.guns.client.resource.ClientAssetManager;
import com.tac.guns.client.resource.pojo.ClientGunIndexPOJO;
import com.tac.guns.client.resource.pojo.display.GunDisplay;
import com.tac.guns.client.resource.pojo.display.GunModelTexture;
import com.tac.guns.client.resource.pojo.display.GunTransform;
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
    private GunAnimationStateMachine animationStateMachine;
    private Map<String, ResourceLocation> sounds;
    private GunTransform transform;
    private RenderType slotRenderType;

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
            // 目前支持的动画为 gltf 动画。此处从缓存取出 gltf 的动画资源。
            AnimationStructure animations = ClientAssetManager.INSTANCE.getAnimations(display.getAnimationLocation());
            // 用 gltf 动画资源创建动画控制器
            AnimationController controller = Animations.createControllerFromGltf(animations, this.gunModel);
            // 将动画控制器包装起来
            this.animationStateMachine = new GunAnimationStateMachine(controller);
        }

        // 加载声音
        this.sounds = display.getSounds();
        // 加载 Transform 数据
        this.transform = display.getTransform();

        // 加载 GUI 内枪械图标
        if (display.getSlotTextureLocation() != null) {
            this.slotRenderType = RenderType.entityTranslucent(display.getSlotTextureLocation());
        }
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

    public GunAnimationStateMachine getAnimationStateMachine() {
        return animationStateMachine;
    }

    public ResourceLocation getSounds(String name) {
        return sounds.get(name);
    }

    public GunTransform getTransform() {
        return transform;
    }

    public RenderType getSlotRenderType() {
        return slotRenderType;
    }
}