package com.tac.guns.client.resource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tac.guns.GunMod;
import com.tac.guns.client.resource.cache.ClientAssetManager;
import com.tac.guns.client.resource.cache.data.ClientGunIndex;
import com.tac.guns.client.resource.loader.DataLoader;
import com.tac.guns.client.resource.loader.DisplayDataLoader;
import com.tac.guns.client.resource.loader.TextureLoader;
import com.tac.guns.client.resource.pojo.ClientGunIndexPOJO;
import com.tac.guns.client.resource.pojo.model.CubesItem;
import com.tac.guns.util.GetJarResources;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ClientGunLoader {
    public static final Gson GSON = new GsonBuilder().registerTypeAdapter(ResourceLocation.class, new ResourceLocation.Serializer()).registerTypeAdapter(CubesItem.class, new CubesItem.Deserializer()).create();
    /**
     * 放置自定义枪械模型的目录
     */
    public static final Path FOLDER = Paths.get("config", GunMod.MOD_ID, "custom");
    private static final Marker MARKER = MarkerManager.getMarker("ClientGunLoader");
    private static final String DEFAULT_GUN_PACK_NAME = "tac_default_gun.zip";
    private static final Pattern GUNS_PATTERN = Pattern.compile("^(\\w+)/index/(\\w+)\\.json$");

    /**
     * 加载客户端数据的入口方法
     */
    public static void initAndReload() {
        TextureLoader.TMP_REGISTER_TEXTURE.clear();
        createFolder();
        checkDefaultPack();
        readZipFiles();
    }

    private static void createFolder() {
        File folder = FOLDER.toFile();
        if (!folder.isDirectory()) {
            try {
                Files.createDirectories(folder.toPath());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void checkDefaultPack() {
        // 不管存不存在，强行覆盖
        String jarDefaultPackPath = String.format("/assets/%s/custom/%s", GunMod.MOD_ID, DEFAULT_GUN_PACK_NAME);
        GetJarResources.copyModFile(jarDefaultPackPath, FOLDER, DEFAULT_GUN_PACK_NAME);
    }

    private static void readZipFiles() {
        File[] files = FOLDER.toFile().listFiles((dir, name) -> true);
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".zip")) {
                readZipGunPack(file);
            }
            // TODO: 读取文件夹格式功能
        }
    }

    private static void readZipGunPack(File file) {
        try (ZipFile zipFile = new ZipFile(file)) {
            Enumeration<? extends ZipEntry> iteration = zipFile.entries();
            while (iteration.hasMoreElements()) {
                String path = iteration.nextElement().getName();
                loadGunFromZipPack(path, zipFile);
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private static void loadGunFromZipPack(String path, ZipFile zipFile) throws IOException {
        Matcher matcher = GUNS_PATTERN.matcher(path);
        if (matcher.find()) {
            String namespace = matcher.group(1);
            String id = matcher.group(2);
            ZipEntry entry = zipFile.getEntry(path);
            if (entry == null) {
                GunMod.LOGGER.warn(MARKER, "{} file don't exist", path);
                return;
            }
            try (InputStream stream = zipFile.getInputStream(entry)) {
                // 获取枪械的定义文件
                ClientGunIndexPOJO indexPOJO = GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), ClientGunIndexPOJO.class);

                // 加载 index 数据
                ClientGunIndex index = new ClientGunIndex();
                index.setName(indexPOJO.getName());
                index.setTooltip(indexPOJO.getTooltip());
                ClientAssetManager.INSTANCE.putGunIndex(new ResourceLocation(namespace, id), index);

                // 加载 display 数据
                DisplayDataLoader.loadDisplayData(namespace, id, path, zipFile, indexPOJO.getDisplay());

                // 加载 data 数据
                DataLoader.loadDisplayData(namespace, id, path, zipFile, indexPOJO.getData());
            }
        }
    }
}
