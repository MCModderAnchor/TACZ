package com.tacz.guns.client.resource_legacy.loader.asset;

import com.mojang.blaze3d.audio.OggAudioStream;
import com.tacz.guns.GunMod;
import com.tacz.guns.client.resource_legacy.ClientAssetManager;
import com.tacz.guns.client.resource.manager.SoundAssetsManager;
import com.tacz.guns.util.TacPathVisitor;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class SoundLoader {
    private static final Marker MARKER = MarkerManager.getMarker("SoundLoader");
    private static final Pattern SOUND_PATTERN = Pattern.compile("^(\\w+)/sounds/([\\w/]+)\\.ogg$");

    @Nullable
    public static boolean load(ZipFile zipFile, String zipPath) {
        Matcher matcher = SOUND_PATTERN.matcher(zipPath);
        if (matcher.find()) {
            String namespace = matcher.group(1);
            String path = matcher.group(2);
            ZipEntry entry = zipFile.getEntry(zipPath);
            if (entry == null) {
                GunMod.LOGGER.warn(MARKER, "{} file don't exist", zipPath);
                return false;
            }
            try (InputStream zipEntryStream = zipFile.getInputStream(entry); OggAudioStream audioStream = new OggAudioStream(zipEntryStream)) {
                ByteBuffer bytebuffer = audioStream.readAll();
                ResourceLocation registryName = new ResourceLocation(namespace, path);
                ClientAssetManager.INSTANCE.putSoundBuffer(registryName, new SoundAssetsManager.SoundData(bytebuffer, audioStream.getFormat()));
                return true;
            } catch (IOException ioe) {
                GunMod.LOGGER.warn(MARKER, "Failed to load sound: {}", zipPath);
                ioe.printStackTrace();
            }
        }
        return false;
    }

    public static void load(File root) {
        Path filePath = root.toPath().resolve("sounds");
        if (Files.isDirectory(filePath)) {
            TacPathVisitor visitor = new TacPathVisitor(filePath.toFile(), root.getName(), ".ogg", (id, file) -> {
                try (InputStream stream = Files.newInputStream(file); OggAudioStream audioStream = new OggAudioStream(stream)) {
                    ByteBuffer bytebuffer = audioStream.readAll();
                    ClientAssetManager.INSTANCE.putSoundBuffer(id, new SoundAssetsManager.SoundData(bytebuffer, audioStream.getFormat()));
                } catch (IOException exception) {
                    GunMod.LOGGER.warn(MARKER, "Failed to read sound file: {}", file);
                    exception.printStackTrace();
                }
            });
            try {
                Files.walkFileTree(filePath, visitor);
            } catch (IOException e) {
                GunMod.LOGGER.warn(MARKER, "Failed to walk file tree: {}", filePath);
                e.printStackTrace();
            }
        }
    }
}
