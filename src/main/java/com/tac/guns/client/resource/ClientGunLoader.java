package com.tac.guns.client.resource;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.math.Vector3f;
import com.tac.guns.GunMod;
import com.tac.guns.client.resource.index.ClientAmmoIndex;
import com.tac.guns.client.resource.index.ClientGunIndex;
import com.tac.guns.client.resource.loader.*;
import com.tac.guns.client.resource.pojo.ClientGunIndexPOJO;
import com.tac.guns.client.resource.pojo.model.CubesItem;
import com.tac.guns.client.resource.serialize.Vector3fSerializer;
import com.tac.guns.util.GetJarResources;
import com.tac.guns.util.TacPathVisitor;
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
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ClientGunLoader {
    public static final Gson GSON = new GsonBuilder().registerTypeAdapter(ResourceLocation.class, new ResourceLocation.Serializer())
            .registerTypeAdapter(CubesItem.class, new CubesItem.Deserializer())
            .registerTypeAdapter(Vector3f.class, new Vector3fSerializer())
            .create();
    /**
     * 放置自定义枪械模型的目录
     */
    public static final Path FOLDER = Paths.get("config", GunMod.MOD_ID, "custom");
    private static final Marker MARKER = MarkerManager.getMarker("ClientGunLoader");
    private static final String DEFAULT_GUN_PACK_NAME = "tac_default_gun";
    private static final Pattern GUNS_INDEX_PATTERN = Pattern.compile("^(\\w+)/guns/index/(\\w+)\\.json$");
    /**
     * 储存修改过的客户端 index
     */
    private static final Map<ResourceLocation, ClientGunIndex> GUN_INDEX = Maps.newHashMap();
    private static final Map<ResourceLocation, ClientAmmoIndex> AMMO_INDEX = Maps.newHashMap();

    /**
     * 加载客户端数据的入口方法
     */
    public static void initAndReload() {
        ClientAssetManager.INSTANCE.clearAll();

        GUN_INDEX.clear();
        AMMO_INDEX.clear();

        createFolder();
        checkDefaultPack();
        readFiles();
    }

    public static Set<ResourceLocation> getAllGuns() {
        return GUN_INDEX.keySet();
    }

    public static ClientGunIndex getGunIndex(ResourceLocation registryName) {
        return GUN_INDEX.get(registryName);
    }

    public static ClientAmmoIndex getAmmoIndex(ResourceLocation registryName) {
        return AMMO_INDEX.get(registryName);
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

        GetJarResources.copyModDirectory(jarDefaultPackPath, FOLDER, DEFAULT_GUN_PACK_NAME);
    }

    private static void readFiles() {
        File[] files = FOLDER.toFile().listFiles((dir, name) -> true);
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".zip")) {
                readZipGunPack(file);
            }
            if (file.isDirectory()) {
                File[] subFiles = file.listFiles((dir, name) -> true);
                if (subFiles == null) {
                    return;
                }
                for (File namespaceFile : subFiles) {
                    readDirGunPack(namespaceFile);
                }
            }
        }
    }

    private static void readDirGunPack(File root) {
        if (root.isDirectory()) {
            try {
                GunDisplayLoader.load(root);
                GunDataLoader.load(root);
                AnimationLoader.load(root);
                BedrockModelLoader.load(root);
                TextureLoader.load(root);
                SoundLoader.load(root);

                loadGunIndex(root);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void readZipGunPack(File file) {
        try (ZipFile zipFile = new ZipFile(file)) {
            // 第一次读取
            Enumeration<? extends ZipEntry> iteration = zipFile.entries();
            while (iteration.hasMoreElements()) {
                String path = iteration.nextElement().getName();
                // 加载全部的 display 文件
                if (GunDisplayLoader.load(zipFile, path)) {
                    continue;
                }
                // 加载全部的 data 文件
                if (GunDataLoader.load(zipFile, path)) {
                    continue;
                }
                // 加载全部的 animation 文件
                if (AnimationLoader.load(zipFile, path)) {
                    continue;
                }
                // 加载全部的 model 文件
                if (BedrockModelLoader.load(zipFile, path)) {
                    continue;
                }
                // 加载全部的 texture 文件
                if (TextureLoader.load(zipFile, path)) {
                    continue;
                }
                // 加载全部的 sound 文件
                if (SoundLoader.load(zipFile, path)) {
                    continue;
                }
            }

            // 第二次读取，开始抓药方
            iteration = zipFile.entries();
            while (iteration.hasMoreElements()) {
                String path = iteration.nextElement().getName();
                // 加载枪械的 index 文件
                loadGunIndex(path, zipFile);
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private static void loadGunIndex(String path, ZipFile zipFile) throws IOException {
        Matcher matcher = GUNS_INDEX_PATTERN.matcher(path);
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
                ResourceLocation registryName = new ResourceLocation(namespace, id);
                GUN_INDEX.put(registryName, new ClientGunIndex(indexPOJO));
            }
        }
    }

    private static void loadGunIndex(File root) throws IOException {
        Path filePath = root.toPath().resolve("guns/index");
        if (Files.isDirectory(filePath)) {
            TacPathVisitor visitor = new TacPathVisitor(filePath.toFile(), root.getName(), ".json", (id, file) -> {
                try (InputStream stream = Files.newInputStream(file)) {
                    // 获取枪械的定义文件
                    ClientGunIndexPOJO indexPOJO = GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), ClientGunIndexPOJO.class);
                    GUN_INDEX.put(id, new ClientGunIndex(indexPOJO));
                } catch (IOException exception) {
                    GunMod.LOGGER.warn(MARKER, "Failed to read index file: {}", file);
                    exception.printStackTrace();
                }
            });
            Files.walkFileTree(filePath, visitor);
        }
    }
}
