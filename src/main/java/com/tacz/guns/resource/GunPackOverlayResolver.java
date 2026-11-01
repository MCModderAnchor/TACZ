package com.tacz.guns.resource;

import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PathPackResources;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipFile;

public final class GunPackOverlayResolver {
    private GunPackOverlayResolver() { }

    public static List<String> resolve(PackMeta meta, String minecraftVersion) {
        if (meta.getOverlays() == null) return List.of();
        if (meta.getOverlays().entries() == null) {
            throw new IllegalArgumentException("overlays.entries must be an array");
        }
        Set<String> seen = new HashSet<>();
        List<String> selected = new ArrayList<>();
        for (PackMeta.OverlayEntry entry : meta.getOverlays().entries()) {
            if (entry == null || entry.directory() == null || !entry.directory().matches("[a-z0-9_-]+")
                    || Set.of("assets", "data").contains(entry.directory())) {
                throw new IllegalArgumentException("Invalid overlay directory: " + entry);
            }
            if (!seen.add(entry.directory())) {
                throw new IllegalArgumentException("Duplicate overlay directory: " + entry.directory());
            }
            if (matches(entry.minecraft(), minecraftVersion)) selected.add(entry.directory());
        }
        return List.copyOf(selected);
    }

    public static boolean matches(String specification, String version) {
        if (specification == null || specification.isBlank()) {
            throw new IllegalArgumentException("Missing Minecraft version range");
        }
        try {
            VersionRange range = VersionRange.createFromVersionSpec(specification);
            // Maven's bare version is a recommendation, not a constraint.
            if (!range.hasRestrictions()) {
                throw new IllegalArgumentException("Use an explicit Minecraft range, e.g. [1.21.1]: " + specification);
            }
            if (!version.matches("\\d+\\.\\d+(?:\\.\\d+)?")) {
                return specification.equals("[" + version + "]");
            }
            return range.containsVersion(new DefaultArtifactVersion(version));
        } catch (InvalidVersionSpecificationException e) {
            throw new IllegalArgumentException("Invalid Minecraft version range: " + specification, e);
        }
    }

    public static void validate(Path path, List<String> selected) throws IOException {
        if (selected.isEmpty()) return;
        if (Files.isDirectory(path)) {
            Path root = path.toRealPath();
            for (String directory : selected) {
                Path overlay = path.resolve(directory);
                if (!Files.isDirectory(overlay) || !overlay.toRealPath().startsWith(root)) {
                    throw new IOException("Missing or escaping overlay directory: " + directory);
                }
                try (var paths = Files.walk(overlay)) {
                    for (Path child : paths.toList()) {
                        if (Files.isSymbolicLink(child) && Files.isDirectory(child)) {
                            throw new IOException("Linked overlay directories are not supported: " + child);
                        }
                        if (!child.toRealPath().startsWith(root)) {
                            throw new IOException("Overlay path escapes gun pack: " + child);
                        }
                    }
                }
            }
        } else {
            try (ZipFile zip = new ZipFile(path.toFile())) {
                Set<String> found = new HashSet<>();
                var entries = zip.entries();
                while (entries.hasMoreElements()) {
                    String name = entries.nextElement().getName();
                    for (String directory : selected) {
                        if (name.startsWith(directory + "/")) {
                            validateEntryPath(name);
                            found.add(directory);
                        }
                    }
                }
                if (!found.containsAll(selected)) {
                    throw new IOException("Missing overlay directories: " + selected.stream().filter(s -> !found.contains(s)).toList());
                }
            }
        }
    }

    public static void validateEntryPath(String name) throws IOException {
        if (name.isEmpty() || name.startsWith("/") || name.contains("\\") || name.contains(":")) {
            throw new IOException("Unsafe pack path: " + name);
        }
        String relative = name.endsWith("/") ? name.substring(0, name.length() - 1) : name;
        for (String segment : relative.split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                throw new IOException("Unsafe pack path: " + name);
            }
        }
    }

    public static PackResources open(Path path, PackLocationInfo info, List<String> selected) throws IOException {
        validate(path, selected);
        List<PackResources> overlays = new ArrayList<>();
        if (Files.isDirectory(path)) {
            for (String directory : selected) overlays.add(new PathPackResources(info, path.resolve(directory)));
            return new OverlayPackResources(new PathPackResources(info, path), overlays, null);
        }
        // Like the legacy Forge loader, use directory traversal on a mounted ZIP
        // instead of enumerating the entire archive for every resource query.
        var zip = FileSystems.newFileSystem(path, Map.of());
        try {
            Path root = zip.getPath("/");
            for (String directory : selected) overlays.add(new PathPackResources(info, root.resolve(directory)));
            return new OverlayPackResources(new PathPackResources(info, root), overlays, zip);
        } catch (RuntimeException | Error e) {
            try {
                zip.close();
            } catch (IOException closeException) {
                e.addSuppressed(closeException);
            }
            throw e;
        }
    }
}
