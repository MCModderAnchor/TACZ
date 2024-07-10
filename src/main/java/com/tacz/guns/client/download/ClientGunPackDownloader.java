package com.tacz.guns.client.download;

import com.google.common.collect.Maps;
import com.tacz.guns.GunMod;
import com.tacz.guns.client.gui.GunPackProgressScreen;
import com.tacz.guns.client.resource.ClientReloadManager;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.WorldVersion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.HttpUtil;
import net.minecraftforge.fml.ModList;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

public class ClientGunPackDownloader {
    /**
     * 最大允许文件大小 250 M
     */
    private static final int MAX_FILE_SIZE = 250 * 1024 * 1024;
    private final ReentrantLock downloadLock = new ReentrantLock();
    private final Path serverGunPackPath;
    private @Nullable CompletableFuture<?> currentDownload;

    public ClientGunPackDownloader(Path serverGunPackPath) {
        this.serverGunPackPath = serverGunPackPath;
    }

    private static Map<String, String> getDownloadHeaders() {
        Map<String, String> map = Maps.newHashMap();
        User user = Minecraft.getInstance().getUser();
        WorldVersion currentVersion = SharedConstants.getCurrentVersion();

        map.put("X-Minecraft-Username", user.getName());
        map.put("X-Minecraft-UUID", user.getUuid());
        map.put("X-Minecraft-Version", currentVersion.getName());
        map.put("X-Minecraft-Version-ID", currentVersion.getId());
        map.put("X-TACZ-Version", ModList.get().getModFileById(GunMod.MOD_ID).versionString());
        map.put("User-Agent", "Minecraft Java/" + currentVersion.getName());

        return map;
    }

    public CompletableFuture<?> downloadAndLoadGunPack(String plainUrl, String hash) {
        // 加锁
        this.downloadLock.lock();
        // 最终返回的结果
        CompletableFuture<?> resultFuture;
        try {
            // 检查并清除之前未完成，或者可能失败的下载线程
            this.clearDownloadingGunPack();
            // 将 hash 作为下载资源包的名称
            File gunPack = serverGunPackPath.resolve(hash).toFile();
            // 检查缓存的文件对不对，不对进行删除
            this.removeMismatchFile(hash, gunPack);
            // 下载线程
            CompletableFuture<?> downloadFuture;
            // 如果此资源包存在，那么直接加载即可
            if (gunPack.exists()) {
                downloadFuture = CompletableFuture.completedFuture("");
            }
            // 否则下载，并打开下载界面
            else {
                // 下载进度界面
                GunPackProgressScreen progressScreen = new GunPackProgressScreen();
                Minecraft minecraft = Minecraft.getInstance();
                minecraft.executeBlocking(() -> minecraft.setScreen(progressScreen));
                URL url = new URL(plainUrl);
                downloadFuture = HttpUtil.downloadTo(gunPack, url, getDownloadHeaders(), MAX_FILE_SIZE, progressScreen, minecraft.getProxy());
            }

            // 下载完成后的处理
            this.currentDownload = downloadFuture.thenCompose(target -> {
                // 文件 hash 不匹配，抛出错误
                if (this.notMatchHash(hash, gunPack)) {
                    return Util.failedFuture(new RuntimeException("Hash check failure for file " + gunPack + ", see log"));
                } else {
                    // 否则，加载枪械包客户端部分
                    return this.loadClientGunPack(gunPack);
                }
            }).whenComplete((target, throwable) -> this.afterFail(throwable, gunPack));
            resultFuture = this.currentDownload;
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } finally {
            this.downloadLock.unlock();
        }
        return resultFuture;
    }

    private void afterFail(Throwable throwable, File gunPack) {
        if (throwable == null) {
            return;
        }
        GunMod.LOGGER.warn("Pack application failed: {}, deleting file {}", throwable.getMessage(), gunPack);
        try {
            Files.delete(gunPack.toPath());
        } catch (IOException exception) {
            GunMod.LOGGER.warn("Failed to delete file {}: {}", gunPack, exception.getMessage());
        }
        Minecraft.getInstance().execute(() -> this.displayFailScreen(Minecraft.getInstance()));
    }

    private void displayFailScreen(Minecraft mc) {
        MutableComponent title = Component.translatable("gui.tacz.client_gun_pack_downloader.fail.title");
        MutableComponent subTitle = Component.translatable("gui.tacz.client_gun_pack_downloader.fail.subtitle");
        Component yesButton = CommonComponents.GUI_PROCEED;
        MutableComponent noButton = Component.translatable("menu.disconnect");
        mc.setScreen(new ConfirmScreen(button -> {
            if (button) {
                mc.setScreen(null);
            } else {
                ClientPacketListener clientpacketlistener = mc.getConnection();
                if (clientpacketlistener != null) {
                    clientpacketlistener.getConnection().disconnect(Component.translatable("connect.aborted"));
                }
            }
        }, title, subTitle, yesButton, noButton));
    }

    public void clearDownloadingGunPack() {
        this.downloadLock.lock();
        try {
            if (this.currentDownload != null) {
                this.currentDownload.cancel(true);
            }
            this.currentDownload = null;
        } finally {
            this.downloadLock.unlock();
        }
    }

    public void removeMismatchFile(String expectedHash, File file) {
        if (file.exists() && notMatchHash(expectedHash, file)) {
            try {
                FileUtils.delete(file);
            } catch (IOException exception) {
                GunMod.LOGGER.warn("Failed to delete file {}: {}", file, exception.getMessage());
            }
        }
    }

    private boolean notMatchHash(String expectedHash, File file) {
        try (FileInputStream stream = new FileInputStream(file)) {
            String fileHash = DigestUtils.sha1Hex(stream);
            if (fileHash.toLowerCase(Locale.US).equals(expectedHash.toLowerCase(Locale.US))) {
                GunMod.LOGGER.info("Found file {} matching requested fileHash {}", file, expectedHash);
                return false;
            }
            GunMod.LOGGER.warn("File {} had wrong fileHash (expected {}, found {}).", file, expectedHash, fileHash);
        } catch (IOException ioexception) {
            GunMod.LOGGER.warn("File {} couldn't be hashed.", file, ioexception);
        }
        return true;
    }

    public CompletableFuture<?> loadClientGunPack(File file) {
        ClientReloadManager.loadClientDownloadGunPack(file);
        return CompletableFuture.completedFuture("");
    }
}
