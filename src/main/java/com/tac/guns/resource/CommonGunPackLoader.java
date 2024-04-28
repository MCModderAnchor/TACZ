package com.tac.guns.resource;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tac.guns.GunMod;
import com.tac.guns.config.common.OtherConfig;
import com.tac.guns.crafting.GunSmithTableIngredient;
import com.tac.guns.crafting.GunSmithTableResult;
import com.tac.guns.resource.index.CommonAmmoIndex;
import com.tac.guns.resource.index.CommonAttachmentIndex;
import com.tac.guns.resource.index.CommonGunIndex;
import com.tac.guns.resource.loader.AttachmentDataLoader;
import com.tac.guns.resource.loader.GunDataLoader;
import com.tac.guns.resource.loader.RecipeLoader;
import com.tac.guns.resource.pojo.AmmoIndexPOJO;
import com.tac.guns.resource.pojo.AttachmentIndexPOJO;
import com.tac.guns.resource.pojo.GunIndexPOJO;
import com.tac.guns.resource.pojo.data.gun.ExtraDamage;
import com.tac.guns.resource.serialize.DistanceDamagePairSerializer;
import com.tac.guns.resource.serialize.PairSerializer;
import com.tac.guns.util.GetJarResources;
import com.tac.guns.util.TacPathVisitor;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.tuple.Pair;
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
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class CommonGunPackLoader {
    public static final Gson GSON = new GsonBuilder().registerTypeAdapter(ResourceLocation.class, new ResourceLocation.Serializer())
            .registerTypeAdapter(Pair.class, new PairSerializer())
            .registerTypeAdapter(GunSmithTableIngredient.class, new GunSmithTableIngredient.Serializer())
            .registerTypeAdapter(GunSmithTableResult.class, new GunSmithTableResult.Serializer())
            .registerTypeAdapter(ExtraDamage.DistanceDamagePair.class, new DistanceDamagePairSerializer())
            .create();
    /**
     * 放置自定义枪械模型的目录
     */
    public static final Path FOLDER = Paths.get("config", GunMod.MOD_ID, "custom");
    public static final String DEFAULT_GUN_PACK_NAME = "tac_default_gun";
    public static final Pattern GUNS_INDEX_PATTERN = Pattern.compile("^(\\w+)/guns/index/(\\w+)\\.json$");
    public static final Pattern AMMO_INDEX_PATTERN = Pattern.compile("^(\\w+)/ammo/index/(\\w+)\\.json$");
    public static final Pattern ATTACHMENT_INDEX_PATTERN = Pattern.compile("^(\\w+)/attachment/index/(\\w+)\\.json$");
    private static final Map<ResourceLocation, CommonGunIndex> GUN_INDEX = Maps.newHashMap();
    private static final Map<ResourceLocation, CommonAmmoIndex> AMMO_INDEX = Maps.newHashMap();
    private static final Map<ResourceLocation, CommonAttachmentIndex> ATTACHMENT_INDEX = Maps.newHashMap();
    private static final Marker MARKER = MarkerManager.getMarker("CommonGunPackLoader");

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
        AMMO_INDEX.clear();

        File[] files = FOLDER.toFile().listFiles((dir, name) -> true);
        if (files != null) {
            readIndex(files);
        }
    }

    /**
     * 读取所有枪包的定义文件
     */
    public static void reloadRecipes() {
        CommonAssetManager.INSTANCE.clearRecipes();
        File[] files = FOLDER.toFile().listFiles((dir, name) -> true);
        if (files != null) {
            readRecipes(files);
        }
    }

    private static void checkDefaultPack() {
        if (!OtherConfig.DEFAULT_PACK_DEBUG.get()) {
            String jarDefaultPackPath = String.format("/assets/%s/custom/%s", GunMod.MOD_ID, DEFAULT_GUN_PACK_NAME);
            GetJarResources.copyModDirectory(jarDefaultPackPath, FOLDER, DEFAULT_GUN_PACK_NAME);
        }
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
                // 加载枪械 data 文件
                GunDataLoader.load(zipFile, path);
                // 加载配件 data 文件
                AttachmentDataLoader.load(zipFile, path);
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private static void readDirAsset(File root) {
        if (root.isDirectory()) {
            try {
                GunDataLoader.load(root);
                AttachmentDataLoader.load(root);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void readIndex(File[] files) {
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".zip")) {
                readZipIndex(file);
            }
            if (file.isDirectory()) {
                File[] subFiles = file.listFiles((dir, name) -> true);
                if (subFiles == null) {
                    return;
                }
                for (File namespaceFile : subFiles) {
                    readDirIndex(namespaceFile);
                }
            }
        }
    }

    private static void readZipIndex(File file) {
        try (ZipFile zipFile = new ZipFile(file)) {
            // 第一次读取
            Enumeration<? extends ZipEntry> iteration = zipFile.entries();
            while (iteration.hasMoreElements()) {
                String path = iteration.nextElement().getName();
                // 加载枪械的 index 文件
                loadAmmoIndex(path, zipFile);
                loadGunIndex(path, zipFile);
                loadAttachmentIndex(path, zipFile);
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private static void readDirIndex(File root) {
        if (root.isDirectory()) {
            try {
                loadAmmoIndex(root);
                loadGunIndex(root);
                loadAttachmentIndex(root);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void readRecipes(File[] files) {
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".zip")) {
                readZipRecipes(file);
            }
            if (file.isDirectory()) {
                File[] subFiles = file.listFiles((dir, name) -> true);
                if (subFiles == null) {
                    return;
                }
                for (File namespaceFile : subFiles) {
                    readDirRecipes(namespaceFile);
                }
            }
        }
    }

    private static void readZipRecipes(File file) {
        try (ZipFile zipFile = new ZipFile(file)) {
            // 第一次读取
            Enumeration<? extends ZipEntry> iteration = zipFile.entries();
            while (iteration.hasMoreElements()) {
                String path = iteration.nextElement().getName();
                // 加载枪械的 recipe 文件
                RecipeLoader.load(zipFile, path);
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private static void readDirRecipes(File root) {
        if (root.isDirectory()) {
            try {
                RecipeLoader.load(root);
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

    private static void loadAmmoIndex(String path, ZipFile zipFile) throws IOException {
        Matcher matcher = AMMO_INDEX_PATTERN.matcher(path);
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
                AmmoIndexPOJO indexPOJO = GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), AmmoIndexPOJO.class);
                ResourceLocation registryName = new ResourceLocation(namespace, id);
                try {
                    AMMO_INDEX.put(registryName, CommonAmmoIndex.getInstance(indexPOJO));
                } catch (IllegalArgumentException exception) {
                    GunMod.LOGGER.warn("{} index file read fail!", path);
                    exception.printStackTrace();
                }
            }
        }
    }

    private static void loadAmmoIndex(File root) throws IOException {
        Path filePath = root.toPath().resolve("ammo/index");
        if (Files.isDirectory(filePath)) {
            TacPathVisitor visitor = new TacPathVisitor(filePath.toFile(), root.getName(), ".json", (id, file) -> {
                try (InputStream stream = Files.newInputStream(file)) {
                    // 获取枪械的定义文件
                    AmmoIndexPOJO indexPOJO = GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), AmmoIndexPOJO.class);
                    AMMO_INDEX.put(id, CommonAmmoIndex.getInstance(indexPOJO));
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

    private static void loadAttachmentIndex(String path, ZipFile zipFile) throws IOException {
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
                AttachmentIndexPOJO indexPOJO = GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), AttachmentIndexPOJO.class);
                ResourceLocation registryName = new ResourceLocation(namespace, id);
                try {
                    ATTACHMENT_INDEX.put(registryName, CommonAttachmentIndex.getInstance(indexPOJO));
                } catch (IllegalArgumentException exception) {
                    GunMod.LOGGER.warn("{} index file read fail!", path);
                    exception.printStackTrace();
                }
            }
        }
    }

    private static void loadAttachmentIndex(File root) throws IOException {
        Path filePath = root.toPath().resolve("attachments/index");
        if (Files.isDirectory(filePath)) {
            TacPathVisitor visitor = new TacPathVisitor(filePath.toFile(), root.getName(), ".json", (id, file) -> {
                try (InputStream stream = Files.newInputStream(file)) {
                    // 获取枪械的定义文件
                    AttachmentIndexPOJO indexPOJO = GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), AttachmentIndexPOJO.class);
                    ATTACHMENT_INDEX.put(id, CommonAttachmentIndex.getInstance(indexPOJO));
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

    public static Optional<CommonAmmoIndex> getAmmoIndex(ResourceLocation registryName) {
        return Optional.ofNullable(AMMO_INDEX.get(registryName));
    }

    public static Optional<CommonAttachmentIndex> getAttachmentIndex(ResourceLocation registryName) {
        return Optional.ofNullable(ATTACHMENT_INDEX.get(registryName));
    }

    public static Set<Map.Entry<ResourceLocation, CommonGunIndex>> getAllGuns() {
        return GUN_INDEX.entrySet();
    }

    public static Set<Map.Entry<ResourceLocation, CommonAmmoIndex>> getAllAmmo() {
        return AMMO_INDEX.entrySet();
    }

    public static Set<Map.Entry<ResourceLocation, CommonAttachmentIndex>> getAllAttachments() {
        return ATTACHMENT_INDEX.entrySet();
    }
}
