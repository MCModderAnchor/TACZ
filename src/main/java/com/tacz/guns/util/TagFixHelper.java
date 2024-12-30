package com.tacz.guns.util;

import com.google.common.collect.ImmutableMap;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public class TagFixHelper {
    public static final Map<ResourceLocation, ResourceLocation> oldToNew;
    static  {
        var builder = ImmutableMap.<ResourceLocation, ResourceLocation>builder();
        builder.put(new ResourceLocation("tacz", "muzzle_silence_knight_qd"), new ResourceLocation("tacz", "muzzle_silencer_knight_qd"));
        builder.put(new ResourceLocation("tacz", "muzzle_silence_mirage"), new ResourceLocation("tacz", "muzzle_silencer_mirage"));
        builder.put(new ResourceLocation("tacz", "muzzle_silence_phantom_s1"), new ResourceLocation("tacz", "muzzle_silencer_phantom_s1"));
        builder.put(new ResourceLocation("tacz", "muzzle_silence_ptilopsis"), new ResourceLocation("tacz", "muzzle_silencer_ptilopsis"));
        builder.put(new ResourceLocation("tacz", "muzzle_silence_ursus"), new ResourceLocation("tacz", "muzzle_silencer_ursus"));
        builder.put(new ResourceLocation("tacz", "muzzle_silence_vulture"), new ResourceLocation("tacz", "muzzle_silencer_vulture"));
        oldToNew = builder.build();
    }

    public static ResourceLocation fix(ResourceLocation old) {
        return oldToNew.getOrDefault(old, old);
    }
}
