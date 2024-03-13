package com.tac.guns.client.resource;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.math.Vector3f;
import com.tac.guns.GunMod;
import com.tac.guns.client.resource.index.ClientAmmoIndex;
import com.tac.guns.client.resource.index.ClientAttachmentIndex;
import com.tac.guns.client.resource.index.ClientGunIndex;
import com.tac.guns.client.resource.loader.*;
import com.tac.guns.client.resource.pojo.CommonTransformObject;
import com.tac.guns.client.resource.pojo.model.CubesItem;
import com.tac.guns.client.resource.serialize.Vector3fSerializer;
import com.tac.guns.resource.pojo.AmmoIndexPOJO;
import com.tac.guns.resource.pojo.AttachmentIndexPOJO;
import com.tac.guns.resource.pojo.GunIndexPOJO;
import com.tac.guns.util.GetJarResources;
import com.tac.guns.util.TacPathVisitor;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.tac.guns.resource.CommonGunPackLoader.*;

@OnlyIn(Dist.CLIENT)
public class ClientGunPackLoader {
    public static final Gson GSON = new GsonBuilder().registerTypeAdapter(ResourceLocation.class, new ResourceLocation.Serializer())
            .registerTypeAdapter(CubesItem.class, new CubesItem.Deserializer())
            .registerTypeAdapter(Vector3f.class, new Vector3fSerializer())
            .registerTypeAdapter(CommonTransformObject.class, new CommonTransformObject.Serializer())
            .create();

    private static final Marker MARKER = MarkerManager.getMarker("ClientGunPackLoader");
    /**
     * 储存修改过的客户端 index
     */
    private static final Map<ResourceLocation, ClientGunIndex> GUN_INDEX = Maps.newHashMap();
    private static final Map<ResourceLocation, ClientAmmoIndex> AMMO_INDEX = Maps.newHashMap();
    private static final Map<ResourceLocation, ClientAttachmentIndex> ATTACHMENT_INDEX = Maps.newHashMap();

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
        ClientAssetManager.INSTANCE.clearAll();

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
        ATTACHMENT_INDEX.clear();

        File[] files = FOLDER.toFile().listFiles((dir, name) -> true);
        if (files != null) {
            readIndex(files);
        }
    }

    public static Set<Map.Entry<ResourceLocation, ClientGunIndex>> getAllGuns() {
        return GUN_INDEX.entrySet();
    }

    public static Set<Map.Entry<ResourceLocation, ClientAmmoIndex>> getAllAmmo() {
        return AMMO_INDEX.entrySet();
    }

    public static Set<Map.Entry<ResourceLocation, ClientAttachmentIndex>> getAllAttachments(){
        return ATTACHMENT_INDEX.entrySet();
    }

    public static Optional<ClientGunIndex> getGunIndex(ResourceLocation registryName) {
        return Optional.ofNullable(GUN_INDEX.get(registryName));
    }

    public static Optional<ClientAmmoIndex> getAmmoIndex(ResourceLocation registryName) {
        return Optional.ofNullable(AMMO_INDEX.get(registryName));
    }

    public static Optional<ClientAttachmentIndex> getAttachmentIndex(ResourceLocation registryName) {
        return Optional.ofNullable(ATTACHMENT_INDEX.get(registryName));
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
        // TODO 改成可选安装
        String jarDefaultPackPath = String.format("/assets/%s/custom/%s", GunMod.MOD_ID, DEFAULT_GUN_PACK_NAME);
        GetJarResources.copyModDirectory(jarDefaultPackPath, FOLDER, DEFAULT_GUN_PACK_NAME);
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

    private static void readDirAsset(File root) {
        if (root.isDirectory()) {
            try {
                GunDisplayLoader.load(root);
                AmmoDisplayLoader.load(root);
                AttachmentDisplayLoader.load(root);
                AttachmentSkinLoader.load(root);
                AnimationLoader.load(root);
                BedrockModelLoader.load(root);
                TextureLoader.load(root);
                SoundLoader.load(root);
                LanguageLoader.load(root);
            } catch (IOException e) {
                e.printStackTrace();
            }
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

    private static void readZipAsset(File file) {
        try (ZipFile zipFile = new ZipFile(file)) {
            Enumeration<? extends ZipEntry> iteration = zipFile.entries();
            while (iteration.hasMoreElements()) {
                String path = iteration.nextElement().getName();
                // 加载全部的 display 文件
                if (GunDisplayLoader.load(zipFile, path)) {
                    continue;
                }
                if (AmmoDisplayLoader.load(zipFile, path)) {
                    continue;
                }
                if (AttachmentDisplayLoader.load(zipFile, path)){
                    continue;
                }
                // 加载全部的 skin 文件
                if (AttachmentSkinLoader.load(zipFile, path)){
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
                // 加载语言文件
                LanguageLoader.load(zipFile, path);
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
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
                    GUN_INDEX.put(registryName, ClientGunIndex.getInstance(indexPOJO));
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
                    GUN_INDEX.put(id, ClientGunIndex.getInstance(indexPOJO));
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
                    AMMO_INDEX.put(registryName, ClientAmmoIndex.getInstance(indexPOJO));
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
                    AMMO_INDEX.put(id, ClientAmmoIndex.getInstance(indexPOJO));
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
        Matcher matcher = ATTACHMENT_INDEX_PATTERN.matcher(path);
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
                    ATTACHMENT_INDEX.put(registryName, ClientAttachmentIndex.getInstance(registryName, indexPOJO));
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
                    ATTACHMENT_INDEX.put(id, ClientAttachmentIndex.getInstance(id, indexPOJO));
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
}
