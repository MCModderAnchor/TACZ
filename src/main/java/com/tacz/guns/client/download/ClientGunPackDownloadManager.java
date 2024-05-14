package com.tacz.guns.client.download;

import com.tacz.guns.GunMod;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ClientGunPackDownloadManager {
    private static final Path DOWNLOAD_DIR_PATH = Paths.get("config", GunMod.MOD_ID, "server", "download");
    private static final ClientGunPackDownloader DOWNLOADER = new ClientGunPackDownloader(DOWNLOAD_DIR_PATH);

    public static void init() {
        createFolder();
    }

    public static void download(String url, String hash) {
        DOWNLOADER.downloadAndLoadGunPack(url, hash).thenRun(() -> {
            // TODO 向服务端发包，我成功加载了
        }).exceptionally(throwable -> {
            // TODO 向服务端发包，加载失败了
            return null;
        });
    }

    private static void createFolder() {
        File folder = DOWNLOAD_DIR_PATH.toFile();
        if (!folder.isDirectory()) {
            try {
                Files.createDirectories(folder.toPath());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
