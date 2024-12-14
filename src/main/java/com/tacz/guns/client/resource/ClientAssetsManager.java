package com.tacz.guns.client.resource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tacz.guns.api.client.animation.gltf.AnimationStructure;
import com.tacz.guns.api.vmlib.LuaAnimationConstant;
import com.tacz.guns.api.vmlib.LuaGunAnimationConstant;
import com.tacz.guns.api.vmlib.LuaLibrary;
import com.tacz.guns.client.resource.manager.DisplayManager;
import com.tacz.guns.client.resource.manager.GltfManager;
import com.tacz.guns.client.resource.manager.PackInfoManager;
import com.tacz.guns.client.resource.manager.SoundAssetsManager;
import com.tacz.guns.client.resource.pojo.CommonTransformObject;
import com.tacz.guns.client.resource.pojo.PackInfo;
import com.tacz.guns.client.resource.pojo.animation.bedrock.AnimationKeyframes;
import com.tacz.guns.client.resource.pojo.animation.bedrock.BedrockAnimationFile;
import com.tacz.guns.client.resource.pojo.animation.bedrock.SoundEffectKeyframes;
import com.tacz.guns.client.resource.pojo.display.ammo.AmmoDisplay;
import com.tacz.guns.client.resource.pojo.display.attachment.AttachmentDisplay;
import com.tacz.guns.client.resource.pojo.display.block.BlockDisplay;
import com.tacz.guns.client.resource.pojo.display.gun.GunDisplay;
import com.tacz.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tacz.guns.client.resource.pojo.model.CubesItem;
import com.tacz.guns.client.resource.serialize.AnimationKeyframesSerializer;
import com.tacz.guns.client.resource.serialize.ItemStackSerializer;
import com.tacz.guns.client.resource.serialize.SoundEffectKeyframesSerializer;
import com.tacz.guns.client.resource.serialize.Vector3fSerializer;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.manager.JsonDataManager;
import com.tacz.guns.resource.manager.ScriptManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.luaj.vm2.LuaTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 客户端资源管理器<br/>
 * 所有枪包资源缓存在此
 */
@OnlyIn(Dist.CLIENT)
public enum ClientAssetsManager {
    INSTANCE;
    public static final Gson GSON = new GsonBuilder().registerTypeAdapter(ResourceLocation.class, new ResourceLocation.Serializer())
            .registerTypeAdapter(CubesItem.class, new CubesItem.Deserializer())
            .registerTypeAdapter(Vector3f.class, new Vector3fSerializer())
            .registerTypeAdapter(CommonTransformObject.class, new CommonTransformObject.Serializer())
            .registerTypeAdapter(ItemStack.class, new ItemStackSerializer())
            .registerTypeAdapter(AnimationKeyframes.class, new AnimationKeyframesSerializer())
            .registerTypeAdapter(SoundEffectKeyframes.class, new SoundEffectKeyframesSerializer())
            .registerTypeAdapter(ItemTransforms.class, new ItemTransforms.Deserializer())
            .registerTypeAdapter(ItemTransform.class, new ItemTransform.Deserializer())
            .create();

    // 枪械展示数据
    private JsonDataManager<GunDisplay> gunDisplay;
    // 弹药展示数据
    private JsonDataManager<AmmoDisplay> ammoDisplay;
    // 配件展示数据
    private JsonDataManager<AttachmentDisplay> attachmentDisplay;
    // 方块展示数据
    private JsonDataManager<BlockDisplay> blockDisplay;
    // 原始基岩版模型
    private JsonDataManager<BedrockModelPOJO> bedrockModel;
    // 基岩版模型动画
    private JsonDataManager<BedrockAnimationFile> bedrockAnimation;
    // gltf 动画
    private GltfManager gltfAnimation;
    // 客户端脚本
    private final List<LuaLibrary> libList = List.of(new LuaAnimationConstant(), new LuaGunAnimationConstant());
    private ScriptManager scriptManager;
    // 音效
    private SoundAssetsManager soundAssetsManager;
    // 枪包元数据
    private PackInfoManager packInfo;

    private List<PreparableReloadListener> listeners;

    public void reloadAndRegister(Consumer<PreparableReloadListener> register) {
        if (listeners == null) {
            listeners = new ArrayList<>();
            gunDisplay = register(new DisplayManager<>(GunDisplay.class, GSON, "display/guns", "GunDisplayLoader"));
            ammoDisplay = register(new DisplayManager<>(AmmoDisplay.class, GSON, "display/ammo", "AmmoDisplayLoader"));
            attachmentDisplay = register(new DisplayManager<>(AttachmentDisplay.class, GSON, "display/attachments", "AttachmentDisplayLoader"));
            blockDisplay = register(new DisplayManager<>(BlockDisplay.class, GSON, "display/blocks", "BlockDisplayLoader"));
            bedrockModel = register(new JsonDataManager<>(BedrockModelPOJO.class, GSON, "geo_models", "BedrockModelLoader"));
            bedrockAnimation = register(new JsonDataManager<>(BedrockAnimationFile.class, GSON, new FileToIdConverter("animations", ".animation.json"), "BedrockAnimationLoader"));
            gltfAnimation = register(new GltfManager());
            scriptManager = register(new ScriptManager(new FileToIdConverter("scripts", ".lua"), libList));
            soundAssetsManager = register(new SoundAssetsManager());
            packInfo = register(new PackInfoManager());
        }
        listeners.forEach(register);
    }

    private <T extends PreparableReloadListener> T register(T listener) {
        listeners.add(listener);
        return listener;
    }

    @Nullable
    public GunDisplay getGunDisplay(ResourceLocation id) {
        return gunDisplay.getData(id);
    }

    public Set<Map.Entry<ResourceLocation, GunDisplay>> getGunDisplays() {
        return gunDisplay.getAllData().entrySet();
    }

    @Nullable
    public AttachmentDisplay getAttachmentDisplay(ResourceLocation id) {
        return attachmentDisplay.getData(id);
    }

    @Nullable
    public AmmoDisplay getAmmoDisplay(ResourceLocation id) {
        return ammoDisplay.getData(id);
    }

    @Nullable
    public BlockDisplay getBlockDisplay(ResourceLocation id) {
        return blockDisplay.getData(id);
    }

    @Nullable
    public BedrockModelPOJO getBedrockModelPOJO(ResourceLocation id) {
        return bedrockModel.getData(id);
    }

    @Nullable
    public BedrockAnimationFile getBedrockAnimations(ResourceLocation id) {
        return bedrockAnimation.getData(id);
    }

    @Nullable
    public LuaTable getScript(ResourceLocation id) {
        return scriptManager.getScript(id);
    }

    @Nullable
    public AnimationStructure getGltfAnimation(ResourceLocation id) {
        return gltfAnimation.getGltfAnimation(id);
    }

    @Nullable
    public SoundAssetsManager.SoundData getSoundBuffers(ResourceLocation id) {
        return soundAssetsManager.getData(id);
    }

    @Nullable
    public PackInfo getPackInfo(String namespace) {
        return packInfo.getData(namespace);
    }

    @Nullable
    public PackInfo getPackInfo(@Nullable ResourceLocation namespace) {
        if (namespace == null) {
            return null;
        }
        return packInfo.getData(namespace.getNamespace());
    }

    @OnlyIn(Dist.CLIENT)
    public static void reloadAllPack() {
        try {
            Minecraft.getInstance().reloadResourcePacks().get();
            // 如果连接到多人游戏
            if (ServerLifecycleHooks.getCurrentServer() == null) {
                // 重建索引
                ClientIndexManager.reload();
            } else {
                // 直接刷新data
                CommonAssetsManager.reloadAllPack();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
