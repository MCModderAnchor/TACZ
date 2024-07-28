package com.tacz.guns.resource;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.resource.ResourceManager;
import com.tacz.guns.config.common.OtherConfig;
import com.tacz.guns.crafting.GunSmithTableIngredient;
import com.tacz.guns.crafting.GunSmithTableResult;
import com.tacz.guns.resource.index.CommonAmmoIndex;
import com.tacz.guns.resource.index.CommonAttachmentIndex;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.loader.asset.*;
import com.tacz.guns.resource.loader.index.CommonAmmoIndexLoader;
import com.tacz.guns.resource.loader.index.CommonAttachmentIndexLoader;
import com.tacz.guns.resource.loader.index.CommonGunIndexLoader;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import com.tacz.guns.resource.network.CommonGunPackNetwork;
import com.tacz.guns.resource.pojo.data.gun.ExtraDamage;
import com.tacz.guns.resource.pojo.data.gun.Ignite;
import com.tacz.guns.resource.serialize.*;
import com.tacz.guns.util.GetJarResources;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class CommonGunPackLoader {
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(ResourceLocation.class, new ResourceLocation.Serializer())
            .registerTypeAdapter(Pair.class, new PairSerializer())
            .registerTypeAdapter(GunSmithTableIngredient.class, new GunSmithTableIngredientSerializer())
            .registerTypeAdapter(GunSmithTableResult.class, new GunSmithTableResultSerializer())
            .registerTypeAdapter(ExtraDamage.DistanceDamagePair.class, new DistanceDamagePairSerializer())
            .registerTypeAdapter(Vec3.class, new Vec3Serializer())
            .registerTypeAdapter(Ignite.class, new IgniteSerializer())
            .create();
    /**
     * 放置自定义枪械模型的目录
     */
    public static final Path FOLDER = Paths.get("config", GunMod.MOD_ID, "custom");
    /**
     * 各种 INDEX 缓存
     */
    public static final Map<ResourceLocation, CommonGunIndex> GUN_INDEX = Maps.newHashMap();
    public static final Map<ResourceLocation, CommonAmmoIndex> AMMO_INDEX = Maps.newHashMap();
    public static final Map<ResourceLocation, CommonAttachmentIndex> ATTACHMENT_INDEX = Maps.newHashMap();

    /**
     * 创建存放枪包的文件夹、放入默认枪包，清空网络包缓存
     */
    public static void init() {
        createFolder();
        checkDefaultPack();
        CommonGunPackNetwork.clear();
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

        // 刷新全部手持枪械的玩家的缓存
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            List<ServerPlayer> players = server.getPlayerList().getPlayers();
            players.forEach(player -> {
                if (player != null && IGun.mainhandHoldGun(player)) {
                    AttachmentPropertyManager.postChangeEvent(player, player.getMainHandItem());
                }
            });
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
            for (ResourceManager.ExtraEntry entry : ResourceManager.EXTRA_ENTRIES) {
                GetJarResources.copyModDirectory(entry.modMainClass(), entry.srcPath(), FOLDER, entry.extraDirName());
            }
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
                // 配件 tag
                AttachmentTagsLoader.load(zipFile, path);
                // 枪械允许的 tag
                AllowAttachmentTagsLoader.load(zipFile, path);
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private static void readDirAsset(File root) {
        if (root.isDirectory()) {
            GunDataLoader.load(root);
            AttachmentDataLoader.load(root);
            AttachmentTagsLoader.load(root);
            AllowAttachmentTagsLoader.load(root);
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
            Enumeration<? extends ZipEntry> iteration = zipFile.entries();
            while (iteration.hasMoreElements()) {
                String path = iteration.nextElement().getName();
                CommonAmmoIndexLoader.loadAmmoIndex(path, zipFile);
                CommonGunIndexLoader.loadGunIndex(path, zipFile);
                CommonAttachmentIndexLoader.loadAttachmentIndex(path, zipFile);
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private static void readDirIndex(File root) {
        if (root.isDirectory()) {
            try {
                CommonAmmoIndexLoader.loadAmmoIndex(root);
                CommonGunIndexLoader.loadGunIndex(root);
                CommonAttachmentIndexLoader.loadAttachmentIndex(root);
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
            Enumeration<? extends ZipEntry> iteration = zipFile.entries();
            while (iteration.hasMoreElements()) {
                String path = iteration.nextElement().getName();
                RecipeLoader.load(zipFile, path);
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private static void readDirRecipes(File root) {
        if (root.isDirectory()) {
            RecipeLoader.load(root);
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
