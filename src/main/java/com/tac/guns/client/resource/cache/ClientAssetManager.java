package com.tac.guns.client.resource.cache;

import com.google.common.collect.Maps;
import com.tac.guns.client.animation.AnimationController;
import com.tac.guns.client.animation.gltf.AnimationStructure;
import com.tac.guns.client.model.BedrockAnimatedModel;
import com.tac.guns.client.resource.cache.data.BedrockAnimatedAsset;
import com.tac.guns.client.resource.cache.data.ClientGunInfo;
import com.tac.guns.client.resource.cache.data.GunTextureSet;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * 缓存Map的键统一为 ResourceLocation，其 namespace 为枪包的根目录的下一级文件夹的名称， path 为资源对应的 id 。
 * 举例来说，如果需要获取枪包中 "tac/index"
 */
public enum ClientAssetManager {
    INSTANCE;
    /**
     * 储存动画和模型，包括 模型、与模型绑定的动画控制器 等等。
     */
    private final Map<ResourceLocation, BedrockAnimatedAsset> animatedAssets = Maps.newHashMap();
    /**
     * 储存枪械需要的所有材质的材质注册名，可直接提供给渲染 api 调用。
     */
    private final Map<ResourceLocation, GunTextureSet> gunTextureSets = Maps.newHashMap();
    /**
     * 储存客户端需要的其他枪械数据，如显示名、tooltip、后坐力大小等
     */
    private final Map<ResourceLocation, ClientGunInfo> infos = Maps.newHashMap();

    public void setGunInfo(ResourceLocation registryName, ClientGunInfo info) {
        infos.put(registryName, info);
    }

    public ClientGunInfo getGunInfo(ResourceLocation registryName) {
        return infos.get(registryName);
    }

    public void setBedrockAnimatedAsset(ResourceLocation registryName, BedrockAnimatedModel model, AnimationStructure animation) {
        animatedAssets.put(registryName, new BedrockAnimatedAsset(model, new AnimationController(animation, model)));
    }

    public BedrockAnimatedAsset getBedrockAnimatedAsset(ResourceLocation registryName) {
        return animatedAssets.get(registryName);
    }

    public @Nonnull GunTextureSet getGunTextureSet(ResourceLocation registryName) {
        return gunTextureSets.compute(registryName, (k, v) -> {
            if (v == null) {
                return new GunTextureSet();
            }
            return v;
        });
    }
}
