package com.tac.guns.client.resource.loader;

import com.mojang.blaze3d.audio.OggAudioStream;
import com.mojang.blaze3d.audio.SoundBuffer;
import com.tac.guns.GunMod;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class SoundLoader {
    private static final Marker MARKER = MarkerManager.getMarker("SoundLoader");

    @Nullable
    public static SoundBuffer loadSoundPack(String namespace, String id, ZipFile zipFile, String path) {
        String soundPath = String.format("%s/sounds/%s", namespace, path);
        ZipEntry soundEntry = zipFile.getEntry(soundPath);

        if (soundEntry == null) {
            GunMod.LOGGER.warn(MARKER, "{} sound file don't exist", soundPath);
            return null;
        }

        try (InputStream zipEntryStream = zipFile.getInputStream(soundEntry); OggAudioStream audioStream = new OggAudioStream(zipEntryStream)) {
            ByteBuffer bytebuffer = audioStream.readAll();
            return new SoundBuffer(bytebuffer, audioStream.getFormat());
        } catch (IOException ioe) {
            GunMod.LOGGER.warn(MARKER, "Failed to load sound: {}", soundPath);
            ioe.printStackTrace();
        }
        return null;
    }
}
