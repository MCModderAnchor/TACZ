package com.tac.guns.client.resource.loader;

import com.mojang.blaze3d.audio.OggAudioStream;
import com.mojang.blaze3d.audio.SoundBuffer;
import com.tac.guns.GunMod;
import com.tac.guns.client.resource.cache.ClientAssetManager;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
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
            String filePath = String.format("%s/sounds/%s.ogg", namespace, path);
            ZipEntry entry = zipFile.getEntry(filePath);
            if (entry == null) {
                GunMod.LOGGER.warn(MARKER, "{} file don't exist", filePath);
                return false;
            }
            try (InputStream zipEntryStream = zipFile.getInputStream(entry); OggAudioStream audioStream = new OggAudioStream(zipEntryStream)) {
                ByteBuffer bytebuffer = audioStream.readAll();
                ResourceLocation registryName = new ResourceLocation(namespace, path);
                ClientAssetManager.INSTANCE.putSoundBuffer(registryName, new SoundBuffer(bytebuffer, audioStream.getFormat()));
                return true;
            } catch (IOException ioe) {
                GunMod.LOGGER.warn(MARKER, "Failed to load sound: {}", filePath);
                ioe.printStackTrace();
            }
        }
        return false;
    }
}
