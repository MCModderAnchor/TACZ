package com.tacz.guns.resource.convert.folder;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tacz.guns.GunMod;
import com.tacz.guns.client.resource.pojo.PackInfo;
import com.tacz.guns.resource.PackMeta;
import com.tacz.guns.resource.convert.PackConverter;
import com.tacz.guns.resource.convert.UnsafeResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static com.tacz.guns.resource.PackConvertor.GSON;
import static com.tacz.guns.resource.convert.ConverterUtils.*;

public enum FolderPackConverter implements PackConverter<File> {
    INSTANCE
    ;
    private final List<FolderEntryVisitor> processors = new ArrayList<>();

    {
        processors.add(createFileRenamer("pack.json", "pack_info.json", PackType.CLIENT_RESOURCES));
        processors.add(createFolderRenamer("tags", "tacz_tags", PackType.SERVER_DATA));
        processors.add(createFolderMover("player_animator", PackType.CLIENT_RESOURCES));
        processors.add(createFolderRenamer("sounds", "tacz_sounds", PackType.CLIENT_RESOURCES));
        processors.add(createFolderMover("textures", PackType.CLIENT_RESOURCES));
        processors.add(createFolderMover("animations", PackType.CLIENT_RESOURCES));
        processors.add(createFolderMover("lang", PackType.CLIENT_RESOURCES));
        processors.add(createFolderRenamer("models", "geo_models", PackType.CLIENT_RESOURCES));
        processors.add(createSubFolderMover("display", PackType.CLIENT_RESOURCES));
        processors.add(createSubFolderMover("index", PackType.SERVER_DATA));
        processors.add(createSubFolderMover("data", PackType.SERVER_DATA));
        processors.add(createCategoryFileVisitor("recipes",
                createJsonFileOperator(json -> {
                    JsonObject object = json.getAsJsonObject();
                    if (!object.has("type")) {
                        object.addProperty("type", "tacz:gun_smith_table_crafting");
                        return object;
                    }
                    return null;
                }).andThen(new FileOp() {
                    @Override
                    public boolean run(File baseDir, File file, UnsafeResourceLocation fileResourceLocation) {
                        return move(baseDir, file, fileResourceLocation, PackType.SERVER_DATA);
                    }

                    @Override
                    public String toString() {
                        return "move file to " + PackType.SERVER_DATA.getDirectory();
                    }
                })));
    }

    // return false if the given file does not represent a legacy pack
    // return true even if conversion failed
    @Override
    public boolean convert(File baseDir) throws IOException {
        if (!baseDir.isDirectory()) {
            return false;
        }
        File[] subFiles = Objects.requireNonNullElseGet(baseDir.listFiles(), () -> new File[0]);
        if (subFiles.length == 0) {
            return false;
        }
        String namespace = null;
        for (File subFile : subFiles) {
            if (subFile.isDirectory()) {
                File legacyPackFile = new File(subFile, "pack.json");
                if (legacyPackFile.isFile()) {
                    BufferedReader reader = Files.newBufferedReader(legacyPackFile.toPath(), StandardCharsets.UTF_8);
                    PackInfo legacyPackInfo = GSON.fromJson(reader, PackInfo.class);
                    reader.close();
                    if (legacyPackInfo != null) {
                        namespace = relativePath(baseDir, subFile);
                        break;
                    }
                }
            }
        }
        if (namespace == null) {
            return false;
        }
        Path resourcePacksPath = FMLPaths.GAMEDIR.get().resolve("tacz");
        Path newPath = resourcePacksPath.resolve(baseDir.getName());
        File newPathAsFile = newPath.toFile();
        if (newPathAsFile.isDirectory()) {
            throw new FileAlreadyExistsException("New path already exists");
        }
        FileUtils.copyDirectory(baseDir, newPathAsFile);
        AtomicBoolean failed = new AtomicBoolean();
        Files.walkFileTree(newPath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                GunMod.LOGGER.debug("Visiting old pack file {}", file);
                for (FolderEntryVisitor visitor : processors) {
                    GunMod.LOGGER.debug("Trying to run visitFile from {}", visitor);
                    if (visitor.visitFile(newPathAsFile, file.toFile())) {
                        GunMod.LOGGER.debug("File handled, skipping remaining visitors");
                        break; // a visitor handled this file, go next
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                GunMod.LOGGER.debug("Visiting old pack directory {}", dir);
                if (!newPath.equals(dir)) { // always visit the root directory!
                    for (FolderEntryVisitor visitor : processors) {
                        GunMod.LOGGER.debug("Trying to run visitDirectory from {}", visitor);
                        FileVisitResult result = visitor.visitDirectory(newPathAsFile, dir.toFile());
                        if (result != null) {
                            GunMod.LOGGER.debug("Folder handled with result {}", result);
                            return result;
                        }
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                GunMod.LOGGER.error("Error visiting old pack file", exc);
                failed.set(true);
                return FileVisitResult.TERMINATE;
            }
        });
        if (!failed.get()) {
            Files.walkFileTree(newPath, new EmptyFolderRemover(newPath));
            PackMeta newPackMeta = new PackMeta(namespace, null);
            String newPackMetaAsJson = GSON.toJson(newPackMeta);
            Path newPackMetaPath = newPath.resolve("gunpack.meta.json");
            Files.writeString(newPackMetaPath, newPackMetaAsJson, StandardCharsets.UTF_8);
        }
        return true;
    }

    private static FolderEntryVisitor createFileRenamer(String oldPath, String newPath, PackType newFolderType) {
        return new FolderEntryVisitor() {
            @Override
            public boolean visitFile(File baseDir, File subFile) {
                UnsafeResourceLocation entry = parseEntryName(baseDir, subFile);
                if (entry != null) {
                    String path = entry.getPath();
                    if (path.equals(oldPath)) {
                        String namespace = entry.getNamespace();
                        String newRelativePath = toFilePath(namespace, newPath, newFolderType);
                        File newFile = new File(baseDir, newRelativePath);
                        newFile.getParentFile().mkdirs();
                        return subFile.renameTo(newFile);
                    }
                }
                return false;
            }

            @Override
            public String toString() {
                return "file renamer " + oldPath + " -> " + newPath + " (in " + newFolderType.getDirectory() + ")";
            }
        };
    }

    private static FolderEntryVisitor createFolderRenamer(String oldPath, String newPath, PackType folderType) {
        return new FolderEntryVisitor() {
            @Override
            public @Nullable FileVisitResult visitDirectory(File baseDir, File subFolder) {
                UnsafeResourceLocation entry = parseEntryName(baseDir, subFolder);
                if (entry != null) {
                    String path = entry.getPath();
                    if (path.equals(oldPath)) {
                        String namespace = entry.getNamespace();
                        String newRelativePath = toFilePath(namespace, newPath, folderType);
                        File newFile = new File(baseDir, newRelativePath);
                        newFile.getParentFile().mkdirs();
                        subFolder.renameTo(newFile);
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                }
                return null;
            }

            @Override
            public String toString() {
                return "folder renamer " + oldPath + " -> " + newPath + " (in " + folderType.getDirectory() + ")";
            }
        };
    }

    private static FolderEntryVisitor createSubFolderMover(String path, PackType folderType) {
        return new FolderEntryVisitor() {
            @Override
            public @Nullable FileVisitResult visitDirectory(File baseDir, File subFolder) {
                UnsafeResourceLocation entry = parseEntryName(baseDir, subFolder);
                if (entry != null) {
                    String entryPath = entry.getPath();
                    int i = entryPath.indexOf('/');
                    if (i != -1) {
                        String subPath = entryPath.substring(i + 1);
                        if (subPath.equals(path)) {
                            String parentPath = entryPath.substring(0, i);
                            String namespace = entry.getNamespace();
                            String newRelativePath = toFilePath(namespace, path + "/" + parentPath, folderType);
                            File newFile = new File(baseDir, newRelativePath);
                            newFile.getParentFile().mkdirs();
                            subFolder.renameTo(newFile);
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                    }
                }
                return null;
            }

            @Override
            public String toString() {
                return "sub folder mover " + path + " to " + folderType.getDirectory();
            }
        };
    }

    private static FolderEntryVisitor createFolderMover(String path, PackType folderType) {
        return createFolderRenamer(path, path, folderType);
    }

    private static FolderEntryVisitor createCategoryFileVisitor(String category, FileOp fileOperator) {
        String categoryPlusSlash = category + "/";
        return new FolderEntryVisitor() {
            @Override
            public boolean visitFile(File baseDir, File subFile) throws IOException {
                UnsafeResourceLocation entry = parseEntryName(baseDir, subFile);
                if (entry != null) {
                    String path = entry.getPath();
                    if (path.startsWith(categoryPlusSlash)) {
                        return fileOperator.run(baseDir, subFile, entry);
                    }
                }
                return false;
            }

            @Override
            public String toString() {
                return "category file visitor (category " + category + ", file operator " + fileOperator + ")";
            }
        };
    }

    private static FileOp createJsonFileOperator(Function<JsonElement, @Nullable JsonElement> jsonOperator) {
        return new FileOp() {
            @Override
            public boolean run(File baseDir, File file, UnsafeResourceLocation fileResourceLocation) throws IOException {
                if (file.getName().endsWith(".json")) {
                    Path asPath = file.toPath();
                    BufferedReader reader = Files.newBufferedReader(asPath, StandardCharsets.UTF_8);
                    JsonElement oldJson = JsonParser.parseReader(reader);
                    reader.close();
                    JsonElement newJson = jsonOperator.apply(oldJson);
                    if (newJson != null) {
                        BufferedWriter writer = Files.newBufferedWriter(asPath, StandardCharsets.UTF_8);
                        GSON.toJson(newJson, writer);
                        writer.close();
                        return true;
                    }
                }
                return false;
            }

            @Override
            public String toString() {
                return "json file modifier";
            }
        };
    }

    private static boolean move(File baseDir, File file, UnsafeResourceLocation location, PackType folderType) {
        String newPath = toFilePath(location, folderType);
        File newFile = new File(baseDir, newPath);
        newFile.getParentFile().mkdirs();
        return file.renameTo(newFile);
    }
}
