package com.tac.guns.resource;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tac.guns.GunMod;
import com.tac.guns.resource.index.CommonGunIndex;
import com.tac.guns.resource.loader.GunDataLoader;
import com.tac.guns.resource.pojo.GunIndexPOJO;
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
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class CommonGunPackLoader {
    public static final Gson GSON = new GsonBuilder().registerTypeAdapter(ResourceLocation.class, new ResourceLocation.Serializer()).create();

    private static final Map<ResourceLocation, CommonGunIndex> GUN_INDEX = Maps.newHashMap();
    private static final Marker MARKER = MarkerManager.getMarker("CommonGunPackLoader");
    /**
     * 放置自定义枪械模型的目录
     */
    public static final Path FOLDER = Paths.get("config", GunMod.MOD_ID, "custom");
    public static final String DEFAULT_GUN_PACK_NAME = "tac_default_gun";
    public static final Pattern GUNS_INDEX_PATTERN = Pattern.compile("^(\\w+)/guns/index/(\\w+)\\.json$");

    /**
     * 创建存放枪包的文件夹、放入默认枪包
     */
    public static void init() {
        createFolder();
        checkDefaultPack();
    }

    /**
     * 读取所有枪包的资源文件
     */
    public static void reloadAsset() {
        CommonAssetManager.INSTANCE.clearAll();

        File[] files = FOLDER.toFile().listFiles((dir, name) -> true);
        if (files != null) {
            readAsset(files);
        }
    }

    /**
     * 读取所有枪包的定义文件
     */
    public static void reloadIndex() {
        GUN_INDEX.clear();

        File[] files = FOLDER.toFile().listFiles((dir, name) -> true);
        if (files != null) {
            readGunIndex(files);
        }
    }

    private static void checkDefaultPack() {
        // todo 改成可选安装
        String jarDefaultPackPath = String.format("/assets/%s/custom/%s", GunMod.MOD_ID, DEFAULT_GUN_PACK_NAME);
        GetJarResources.copyModDirectory(jarDefaultPackPath, FOLDER, DEFAULT_GUN_PACK_NAME);
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

    private static void readAsset(File[] files) {
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".zip")) {
                readZipAsset(file);
            }
            if (file.isDirectory()) {
                File[] subFiles = file.listFiles((dir, name) -> true);
                if (subFiles == null) {
                    return;
                }
                for (File namespaceFile : subFiles) {
                    readDirAsset(namespaceFile);
                }
            }
        }
    }

    private static void readZipAsset(File file) {
        try (ZipFile zipFile = new ZipFile(file)) {
            Enumeration<? extends ZipEntry> iteration = zipFile.entries();
            while (iteration.hasMoreElements()) {
                String path = iteration.nextElement().getName();
                // 加载全部的 data 文件
                GunDataLoader.load(zipFile, path);
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private static void readDirAsset(File root) {
        if (root.isDirectory()) {
            try {
                GunDataLoader.load(root);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void readGunIndex(File[] files) {
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".zip")) {
                readZipGunIndex(file);
            }
            if (file.isDirectory()) {
                File[] subFiles = file.listFiles((dir, name) -> true);
                if (subFiles == null) {
                    return;
                }
                for (File namespaceFile : subFiles) {
                    readDirGunIndex(namespaceFile);
                }
            }
        }
    }

    private static void readZipGunIndex(File file) {
        try (ZipFile zipFile = new ZipFile(file)) {
            // 第一次读取
            Enumeration<? extends ZipEntry> iteration = zipFile.entries();
            while (iteration.hasMoreElements()) {
                String path = iteration.nextElement().getName();
                // 加载枪械的 index 文件
                loadGunIndex(path, zipFile);
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private static void readDirGunIndex(File root) {
        if (root.isDirectory()) {
            try {
                loadGunIndex(root);
            } catch (IOException e) {
                e.printStackTrace();
            }
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
                GunIndexPOJO indexPOJO = GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), GunIndexPOJO.class);
                ResourceLocation registryName = new ResourceLocation(namespace, id);
                try {
                    GUN_INDEX.put(registryName, CommonGunIndex.getInstance(indexPOJO));
                } catch (IllegalArgumentException exception) {
                    GunMod.LOGGER.warn("{} index file read fail!", path);
                    exception.printStackTrace();
                }
            }
        }
    }

    private static void loadGunIndex(File root) throws IOException {
        Path filePath = root.toPath().resolve("guns/index");
        if (Files.isDirectory(filePath)) {
            TacPathVisitor visitor = new TacPathVisitor(filePath.toFile(), root.getName(), ".json", (id, file) -> {
                try (InputStream stream = Files.newInputStream(file)) {
                    // 获取枪械的定义文件
                    GunIndexPOJO indexPOJO = GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), GunIndexPOJO.class);
                    GUN_INDEX.put(id, CommonGunIndex.getInstance(indexPOJO));
                } catch (IOException exception) {
                    GunMod.LOGGER.warn(MARKER, "Failed to read index file: {}", file);
                    exception.printStackTrace();
                } catch (IllegalArgumentException exception) {
                    GunMod.LOGGER.warn("{} index file read fail!", file);
                    exception.printStackTrace();
                }
            });
            Files.walkFileTree(filePath, visitor);
        }
    }

    public static Optional<CommonGunIndex> getGunIndex(ResourceLocation registryName) {
        return Optional.ofNullable(GUN_INDEX.get(registryName));
    }
}
