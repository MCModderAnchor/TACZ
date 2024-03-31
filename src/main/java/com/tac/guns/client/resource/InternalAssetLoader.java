package com.tac.guns.client.resource;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tac.guns.client.animation.AnimationListenerSupplier;
import com.tac.guns.client.animation.Animations;
import com.tac.guns.client.animation.ObjectAnimation;
import com.tac.guns.client.animation.gltf.AnimationStructure;
import com.tac.guns.client.resource.pojo.animation.gltf.RawAnimationStructure;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class InternalAssetLoader {
    private static final ResourceLocation DEFAULT_PISTOL_ANIMATIONS_LOC = new ResourceLocation("tac", "animations/pistol_default.gltf");
    private static final ResourceLocation DEFAULT_RIFLE_ANIMATIONS_LOC = new ResourceLocation("tac", "animations/rifle_default.gltf");
    private static List<ObjectAnimation> defaultPistolAnimations;
    private static List<ObjectAnimation> defaultRifleAnimations;

    public static void onResourceReload() {
        AnimationStructure pistolAnimationStructure = loadAnimations(DEFAULT_PISTOL_ANIMATIONS_LOC);
        AnimationStructure rifleAnimationStructure = loadAnimations(DEFAULT_RIFLE_ANIMATIONS_LOC);
        defaultPistolAnimations = Animations.createAnimationFromGltf(pistolAnimationStructure, (AnimationListenerSupplier[]) null);
        defaultRifleAnimations = Animations.createAnimationFromGltf(rifleAnimationStructure, (AnimationListenerSupplier[]) null);
    }

    public static List<ObjectAnimation> getDefaultPistolAnimations() {
        return defaultPistolAnimations;
    }

    public static List<ObjectAnimation> getDefaultRifleAnimations() {
        return defaultRifleAnimations;
    }

    private static AnimationStructure loadAnimations(ResourceLocation resourceLocation) {
        try {
            Resource resource = Minecraft.getInstance().getResourceManager().getResource(resourceLocation);
            InputStream inputStream = resource.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            JsonObject json = JsonParser.parseReader(bufferedReader).getAsJsonObject();
            RawAnimationStructure rawAnimationStructure = ClientGunPackLoader.GSON.fromJson(json, RawAnimationStructure.class);
            return new AnimationStructure(rawAnimationStructure);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
