package com.tacz.guns.compat.playeranimator.animation;

import com.google.common.collect.Maps;
import com.google.gson.JsonParseException;
import com.tacz.guns.GunMod;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.data.gson.AnimationSerializing;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.*;

public class PlayerAnimatorAssetManager extends SimplePreparableReloadListener<Map<ResourceLocation, HashMap<String, KeyframeAnimation>>> {
    private static PlayerAnimatorAssetManager INSTANCE;

    private final FileToIdConverter filetoidconverter = new FileToIdConverter("player_animator", ".json");
    private final HashMap<ResourceLocation, HashMap<String, KeyframeAnimation>> animations = new HashMap<>();

    public static PlayerAnimatorAssetManager get() {
        if (INSTANCE == null) {
            INSTANCE = new PlayerAnimatorAssetManager();
        }
        return INSTANCE;
    }

    void putAnimation(ResourceLocation id, InputStream stream) throws IOException {
        List<KeyframeAnimation> keyframeAnimations = AnimationSerializing.deserializeAnimation(stream);
        for (var animation : keyframeAnimations) {
            if (animation.extraData.get("name") instanceof String text) {
                String name = PlayerAnimationRegistry.serializeTextToString(text).toLowerCase(Locale.ENGLISH);
                animations.computeIfAbsent(id, k -> Maps.newHashMap()).put(name, animation);
            }
        }
    }

    Optional<KeyframeAnimation> getAnimations(ResourceLocation id, String name) {
        var animationHashMap = this.animations.get(id);
        if (animationHashMap == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(animationHashMap.get(name));
    }

    public boolean containsKey(ResourceLocation id) {
        return animations.containsKey(id);
    }

    public void clearAll() {
        animations.clear();
    }

    @Override
    protected Map<ResourceLocation, HashMap<String, KeyframeAnimation>> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, HashMap<String, KeyframeAnimation>> output = Maps.newHashMap();
        for(Map.Entry<ResourceLocation, Resource> entry : filetoidconverter.listMatchingResources(manager).entrySet()) {
            ResourceLocation resourcelocation = entry.getKey();
            ResourceLocation resourcelocation1 = filetoidconverter.fileToId(resourcelocation);

            try (Reader reader = entry.getValue().openAsReader()) {
                List<KeyframeAnimation> keyframeAnimations = AnimationSerializing.deserializeAnimation(reader);
                for (var animation : keyframeAnimations) {
                    if (animation.extraData.get("name") instanceof String text) {
                        String name = PlayerAnimationRegistry.serializeTextToString(text).toLowerCase(Locale.ENGLISH);
                        output.computeIfAbsent(resourcelocation1, k -> Maps.newHashMap()).put(name, animation);
                    }
                }
            } catch (IllegalArgumentException | IOException | JsonParseException jsonparseexception) {
                GunMod.LOGGER.warn("Failed to player animation file: {}, entry: {}", resourcelocation, entry);
            }
        }
        return output;
    }

    @Override
    protected void apply(Map<ResourceLocation, HashMap<String, KeyframeAnimation>> map, ResourceManager manager, ProfilerFiller profiler) {
        animations.clear();
        animations.putAll(map);
    }
}