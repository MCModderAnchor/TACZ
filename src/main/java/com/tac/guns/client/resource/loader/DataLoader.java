package com.tac.guns.client.resource.loader;

import com.mojang.blaze3d.audio.SoundBuffer;
import com.tac.guns.client.resource.cache.ClientAssetManager;
import com.tac.guns.client.resource.pojo.data.GunData;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.util.zip.ZipFile;

public final class DataLoader {
    private static final Marker MARKER = MarkerManager.getMarker("DataLoader");

    public static void loadDisplayData(String namespace, String id, String path, ZipFile zipFile, GunData data) {
        // 缓存声音
        putSound(namespace, id, zipFile, data.getSounds().getDrawSoundLocation());
        putSound(namespace, id, zipFile, data.getSounds().getInspectSoundLocation());
        putSound(namespace, id, zipFile, data.getSounds().getReloadSoundLocation());
        putSound(namespace, id, zipFile, data.getSounds().getShootSoundLocation());
    }

    private static void putSound(String namespace, String id, ZipFile zipFile, String drawSoundLocation) {
        SoundBuffer soundBuffer = SoundLoader.loadSoundPack(namespace, id, zipFile, drawSoundLocation);
        ClientAssetManager.INSTANCE.putSoundBuffer(new ResourceLocation(namespace, drawSoundLocation), soundBuffer);
    }
}
