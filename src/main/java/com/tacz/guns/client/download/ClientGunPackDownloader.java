package com.tacz.guns.client.download;

import com.google.common.collect.Maps;
import com.tacz.guns.GunMod;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.WorldVersion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.HttpUtil;
import net.minecraftforge.fml.ModList;
import org.apache.commons.codec.digest.DigestUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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

    public CompletableFuture<?> downloadAndLoadGunPack(String url, String hash) {
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
                ProgressScreen progressScreen = new ProgressScreen(true);
                Minecraft minecraft = Minecraft.getInstance();
                minecraft.executeBlocking(() -> minecraft.setScreen(progressScreen));
                downloadFuture = HttpUtil.downloadTo(gunPack, url, getDownloadHeaders(), MAX_FILE_SIZE, progressScreen, minecraft.getProxy());
            }

            // 下载完成后的处理
            this.currentDownload = downloadFuture.thenCompose(target -> {
                // 文件 hash 不匹配，抛出错误
                if (!this.checkHash(hash, gunPack)) {
                    return Util.failedFuture(new RuntimeException("Hash check failure for file " + gunPack + ", see log"));
                } else {
                    // 否则，加载枪械包客户端部分
                    return this.loadClientGunPack(gunPack);
                }
            }).whenComplete((target, throwable) -> this.afterFail(throwable, gunPack));
            resultFuture = this.currentDownload;
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
        TranslatableComponent title = new TranslatableComponent("multiplayer.texturePrompt.failure.line1");
        TranslatableComponent subTitle = new TranslatableComponent("multiplayer.texturePrompt.failure.line2");
        Component yesButton = CommonComponents.GUI_PROCEED;
        TranslatableComponent noButton = new TranslatableComponent("menu.disconnect");
        mc.setScreen(new ConfirmScreen(button -> {
            if (button) {
                mc.setScreen(null);
            } else {
                ClientPacketListener clientpacketlistener = mc.getConnection();
                if (clientpacketlistener != null) {
                    clientpacketlistener.getConnection().disconnect(new TranslatableComponent("connect.aborted"));
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
        if (file.exists() && !checkHash(expectedHash, file)) {
            try {
                Files.delete(file.toPath());
            } catch (IOException exception) {
                GunMod.LOGGER.warn("Failed to delete file {}: {}", file, exception.getMessage());
            }
        }
    }

    private boolean checkHash(String expectedHash, File file) {
        try {
            String fileHash = DigestUtils.sha1Hex(new FileInputStream(file));
            if (fileHash.toLowerCase(Locale.ROOT).equals(expectedHash.toLowerCase(Locale.ROOT))) {
                GunMod.LOGGER.info("Found file {} matching requested fileHash {}", file, expectedHash);
                return true;
            }
            GunMod.LOGGER.warn("File {} had wrong fileHash (expected {}, found {}).", file, expectedHash, fileHash);
        } catch (IOException ioexception) {
            GunMod.LOGGER.warn("File {} couldn't be hashed.", file, ioexception);
        }
        return false;
    }

    public CompletableFuture<?> loadClientGunPack(File file) {
        // TODO 完成加载部分
        return CompletableFuture.completedFuture("");
    }
}
