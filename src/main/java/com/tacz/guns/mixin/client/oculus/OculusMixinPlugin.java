package com.tacz.guns.mixin.client.oculus;

import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * 仅在安装 Oculus（Iris）时应用光影相关 mixin。
 * <p>
 * 目标为 Iris 私有内部类，未安装光影时该类不存在，必须跳过以避免 mixin 加载失败。
 */
public class OculusMixinPlugin implements IMixinConfigPlugin {
    private final boolean loaded;

    public OculusMixinPlugin() {
        loaded = LoadingModList.get().getModFileById("oculus") != null;
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return loaded;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
