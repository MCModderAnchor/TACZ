package com.tacz.guns.client.util;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;

public final class ClientRegistryAccess {
    private ClientRegistryAccess() {}
    public static HolderLookup.Provider registries() {
        var level = Minecraft.getInstance().level;
        return level == null ? RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY) : level.registryAccess();
    }
}
