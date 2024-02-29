package com.tac.guns.client.resource.loader;

import com.tac.guns.GunMod;
import com.tac.guns.client.resource.ClientGunLoader;
import com.tac.guns.client.resource.cache.ClientAssetManager;
import com.tac.guns.client.resource.pojo.data.GunData;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class GunDataLoader {
    private static final Marker MARKER = MarkerManager.getMarker("GunDataLoader");
    private static final Pattern DATA_PATTERN = Pattern.compile("^(\\w+)/guns/data/([\\w/]+)\\.json$");

    public static boolean load(ZipFile zipFile, String zipPath) throws IOException {
        Matcher matcher = DATA_PATTERN.matcher(zipPath);
        if (matcher.find()) {
            String namespace = matcher.group(1);
            String path = matcher.group(2);
            String filePath = String.format("%s/guns/data/%s.json", namespace, path);
            ZipEntry entry = zipFile.getEntry(filePath);
            if (entry == null) {
                GunMod.LOGGER.warn(MARKER, "{} file don't exist", filePath);
                return false;
            }
            try (InputStream stream = zipFile.getInputStream(entry)) {
                ResourceLocation registryName = new ResourceLocation(namespace, path);
                GunData data = ClientGunLoader.GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), GunData.class);
                ClientAssetManager.INSTANCE.putGunData(registryName, data);
                return true;
            }
        }
        return false;
    }
}
