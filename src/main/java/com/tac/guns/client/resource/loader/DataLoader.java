package com.tac.guns.client.resource.loader;

import com.mojang.blaze3d.audio.SoundBuffer;
import com.tac.guns.GunMod;
import com.tac.guns.client.resource.ClientGunLoader;
import com.tac.guns.client.resource.cache.ClientAssetManager;
import com.tac.guns.client.resource.pojo.data.GunData;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class DataLoader {
    private static final Marker MARKER = MarkerManager.getMarker("DataLoader");

    @Nullable
    public static GunData loadDisplayData(String namespace, String id, String dataPath, ZipFile zipFile) throws IOException {
        String filePath = String.format("%s/guns/data/%s", namespace, dataPath);
        ZipEntry entry = zipFile.getEntry(filePath);
        if (entry == null) {
            GunMod.LOGGER.warn(MARKER, "{} file don't exist", dataPath);
            return null;
        }
        try (InputStream stream = zipFile.getInputStream(entry)) {
            // 获取枪械的 display 文件
            GunData data = ClientGunLoader.GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), GunData.class);
            // 缓存声音
            putSound(namespace, id, zipFile, data.getSounds().getDrawSoundLocation());
            putSound(namespace, id, zipFile, data.getSounds().getInspectSoundLocation());
            putSound(namespace, id, zipFile, data.getSounds().getReloadSoundLocation());
            putSound(namespace, id, zipFile, data.getSounds().getShootSoundLocation());
            return data;
        }
    }

    private static void putSound(String namespace, String id, ZipFile zipFile, String drawSoundLocation) {
        SoundBuffer soundBuffer = SoundLoader.loadSoundPack(namespace, id, zipFile, drawSoundLocation);
        ClientAssetManager.INSTANCE.putSoundBuffer(new ResourceLocation(namespace, drawSoundLocation), soundBuffer);
    }
}
