package com.tacz.guns.resource;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.resource.ResourceManager;
import com.tacz.guns.config.PreLoadConfig;
import com.tacz.guns.util.GetJarResources;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.locating.IModFile;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public enum GunPackLoader implements RepositorySource {
    INSTANCE;
    private static final Marker MARKER = MarkerManager.getMarker("GunPackFinder");
    public PackType packType;
    private boolean firstLoad = true;


    @Override
    public void loadPacks(Consumer<Pack> pOnLoad) {
        Pack extensionsPack = discoverExtensions();
        if (extensionsPack != null) {
            pOnLoad.accept(extensionsPack);
        }
    }

    public Pack discoverExtensions() {
        Path resourcePacksPath = FMLPaths.GAMEDIR.get().resolve("tacz");
        File folder = resourcePacksPath.toFile();
        if (!folder.isDirectory()) {
            try {
                Files.createDirectories(folder.toPath());
            } catch (Exception e) {
                GunMod.LOGGER.warn(MARKER, "Failed to init tacz resource directory...", e);
                return null;
            }
        }

        // NeoForge loads STARTUP configuration before pack discovery.

        // 仅在启动后的第一次加载时检查默认资源包是否需要导出；后续重载不重新导出
        if (firstLoad) {
            if (!PreLoadConfig.override.get()) {
                for (ResourceManager.ExtraEntry entry : ResourceManager.EXTRA_ENTRIES) {
                    GetJarResources.copyModDirectory(entry.modMainClass(), entry.srcPath(), resourcePacksPath, entry.extraDirName());
                }
            }
            firstLoad = false;
        }

        GunMod.LOGGER.info(MARKER, "Start scanning for gun packs in {}", resourcePacksPath);
        List<GunPack> gunPacks = scanExtensions(resourcePacksPath);
        GunMod.LOGGER.info(MARKER, "Found {} possible gunpack(s) and added them to resource set.", gunPacks.size());
        PackLocationInfo info = new PackLocationInfo("tacz_resources", Component.literal("TACZ Resources"), PackSource.BUILT_IN, Optional.empty());
        PackMetadataSection meta = new PackMetadataSection(Component.translatable("tacz.resources.modresources"), SharedConstants.getCurrentVersion().getPackVersion(packType), Optional.empty());
        PackSelectionConfig config = new PackSelectionConfig(true, Pack.Position.BOTTOM, false);
        return Pack.readMetaAndCreate(info, createResourcesSupplier(meta, gunPacks), packType, config);
    }

    static Pack.ResourcesSupplier createResourcesSupplier(PackMetadataSection meta, List<GunPack> gunPacks) {
        List<GunPack> packs = List.copyOf(gunPacks);
        return new Pack.ResourcesSupplier() {
            @Override
            public PackResources openPrimary(PackLocationInfo info) {
                // Pack.readPackMetadata closes this instance before the pack is used.
                return createResources(info, meta, List.of());
            }

            @Override
            public PackResources openFull(PackLocationInfo info, Pack.Metadata metadata) {
                List<PackResources> resources = new ArrayList<>();
                try {
                    for (GunPack gunPack : packs) {
                        try {
                            PackLocationInfo location = new PackLocationInfo(gunPack.name, Component.literal(gunPack.name), PackSource.BUILT_IN, Optional.empty());
                            resources.add(GunPackOverlayResolver.open(gunPack.path, location, gunPack.overlays));
                        } catch (IOException e) {
                            GunMod.LOGGER.error(MARKER, "Failed to open gun pack {}: {}", gunPack.path, e.getMessage());
                        }
                    }
                    return createResources(info, meta, resources);
                } catch (RuntimeException | Error e) {
                    for (PackResources resource : resources) {
                        try {
                            resource.close();
                        } catch (RuntimeException closeException) {
                            e.addSuppressed(closeException);
                        }
                    }
                    throw e;
                }
            }
        };
    }

    private static PackResources createResources(PackLocationInfo info, PackMetadataSection meta, List<PackResources> resources) {
        return new DelegatingPackResources(info, meta, resources) {
            @Override
            public IoSupplier<InputStream> getRootResource(String... paths) {
                if (paths.length == 1 && paths[0].equals("pack.png")) {
                    Path logoPath = getModIcon("tacz");
                    if (logoPath != null) {
                        return IoSupplier.create(logoPath);
                    }
                }
                return null;
            }
        };
    }

    public static @Nullable Path getModIcon(String modId) {
        Optional<? extends ModContainer> m = ModList.get().getModContainerById(modId);
        if (m.isPresent()) {
            IModInfo mod = m.get().getModInfo();
            IModFile file = mod.getOwningFile().getFile();
            if (file != null) {
                Path logoPath = file.findResource("icon.png");
                if (Files.exists(logoPath)) {
                    return logoPath;
                }
            }
        }

        return null;
    }

    // 检查路径中的config.json
    // 应该不会在用这个了，先保留
//    private static RepositoryConfig checkConfig(Path resourcePacksPath) {
//        Path configPath = resourcePacksPath.resolve("config.json");
//        if (Files.exists(configPath)) {
//            try (InputStream stream = Files.newInputStream(configPath)) {
//                return GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), RepositoryConfig.class);
//            } catch (IOException | JsonSyntaxException | JsonIOException e) {
//                GunMod.LOGGER.warn(MARKER, "Failed to read config json: {}", configPath);
//            }
//        }
//        // 不存在或者出问题了，新建一个
//        RepositoryConfig config = new RepositoryConfig(true);
//        // 使用Gson写文件
//        try (BufferedWriter writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
//            GSON.toJson(config, writer);
//        } catch (IOException e) {
//            GunMod.LOGGER.warn(MARKER, "Failed to init config json: {}", configPath);
//        }
//        return config;
//    }

    private static GunPack fromDirPath(Path path) throws IOException {
        Path packInfoFilePath = path.resolve("gunpack.meta.json");
        try (InputStream stream = Files.newInputStream(packInfoFilePath)) {
            PackMeta info = CommonAssetsManager.GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), PackMeta.class);

            if (info == null) {
                GunMod.LOGGER.warn(MARKER, "Failed to read info json: {}", packInfoFilePath.getFileName());
                return null;
            }

            if (info.getDependencies() !=null && !modVersionAllMatch(info)) {
                GunMod.LOGGER.warn(MARKER, "Mod version mismatch: {}", packInfoFilePath.getFileName());
                return null;
            }

            return createGunPack(path, info);
        } catch (IOException | JsonSyntaxException | JsonIOException | InvalidVersionSpecificationException | IllegalArgumentException exception) {
            GunMod.LOGGER.warn(MARKER, "Failed to read info json: {}", packInfoFilePath.getFileName());
            GunMod.LOGGER.warn(exception.getMessage());
        }
        return null;
    }

    private static GunPack fromZipPath(Path path)  {
        try(ZipFile zipFile = new ZipFile(path.toFile())){
            ZipEntry extDescriptorEntry = zipFile.getEntry("gunpack.meta.json");
            if (extDescriptorEntry == null) {
                GunMod.LOGGER.error(MARKER,"Failed to load extension from ZIP {}. Error: {}", path.getFileName(), "No gunpack.meta.json found");
                return null;
            }

            try (InputStream stream = zipFile.getInputStream(extDescriptorEntry)) {
                PackMeta info = CommonAssetsManager.GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), PackMeta.class);

                if (info == null) {
                    GunMod.LOGGER.warn(MARKER, "Failed to read info json: {}", path.getFileName());
                    return null;
                }

                if (info.getDependencies() !=null && !modVersionAllMatch(info)) {
                    GunMod.LOGGER.warn(MARKER, "Mod version mismatch: {}", path.getFileName());
                    return null;
                }

                return createGunPack(path, info);
            } catch (IOException | JsonSyntaxException | JsonIOException | InvalidVersionSpecificationException | IllegalArgumentException e) {
                GunMod.LOGGER.error(MARKER,"Failed to load extension from ZIP {}. Error: {}", path.getFileName(), e);
                return null;
            }
        } catch (IOException e) {
            GunMod.LOGGER.error(MARKER,"Failed to load extension from ZIP {}. Error: {}", path.getFileName(), e);
            return null;
        }
    }

    private static List<GunPack> scanExtensions(Path extensionsPath) {
        List<GunPack> gunPacks = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(extensionsPath)){
            for (Path entry : stream) {
                GunPack gunPack = null;
                if (Files.isDirectory(entry)) {
                    gunPack = fromDirPath(entry);
                } else if (entry.toString().endsWith(".zip")) {
                    gunPack = fromZipPath(entry);
                }
                if (gunPack != null) {
                    GunMod.LOGGER.info(MARKER, "- {}, Main namespace: {}", gunPack.path.getFileName(), gunPack.name);
                    gunPacks.add(gunPack);
                }
            }
        } catch (IOException e) {
            GunMod.LOGGER.error(MARKER, "Failed to scan extensions from {}. Error: {}", extensionsPath, e);
        }

        return gunPacks;
    }

    private static boolean modVersionAllMatch(PackMeta info) throws InvalidVersionSpecificationException {
        HashMap<String, String> dependencies = info.getDependencies();
        for (String modId : dependencies.keySet()) {
            if (!modVersionMatch(modId, dependencies.get(modId))) {
                return false;
            }
        }
        return true;
    }

    private static boolean modVersionMatch(String modId, String version) throws InvalidVersionSpecificationException {
        VersionRange versionRange = VersionRange.createFromVersionSpec(version);
        return ModList.get().getModContainerById(modId).map(mod -> {
            ArtifactVersion modVersion = mod.getModInfo().getVersion();
            return versionRange.containsVersion(modVersion);
        }).orElse(false);
    }


    private static GunPack createGunPack(Path path, PackMeta info) throws IOException {
        if (info.getName() == null || info.getName().isBlank()) throw new IOException("Missing gun pack namespace");
        List<String> overlays = GunPackOverlayResolver.resolve(info, SharedConstants.getCurrentVersion().getName());
        GunPackOverlayResolver.validate(path, overlays);
        return new GunPack(path, info.getName(), overlays);
    }

    public record GunPack(Path path, String name, List<String> overlays) {
    }
}
