package com.tac.guns.client.resource.animation;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tac.guns.client.resource.animation.gltf.RawAnimationStructure;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class AnimationAssetLoader {
    public static RawAnimationStructure loadRawAnimationStructure(ResourceLocation resourceLocation) throws IOException {
        Resource resource = Minecraft.getInstance().getResourceManager().getResource(resourceLocation);
        InputStream inputStream = resource.getInputStream();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        JsonObject json = JsonParser.parseReader(bufferedReader).getAsJsonObject();
        Gson gson = new Gson();
        return gson.fromJson(json, RawAnimationStructure.class);
    }
}
