package com.tacz.guns.resource.convert;

import com.tacz.guns.resource.GunPackOverlayResolver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipFile;

/** A sorted, streaming source view; ZIPs are never extracted to disk. */
final class GunPackInput implements AutoCloseable {
    interface Opener { InputStream open() throws IOException; }
    final Map<String, Opener> files = new TreeMap<>();
    private ZipFile zip;

    GunPackInput(Path source) throws IOException {
        try {
            if (Files.isDirectory(source)) {
                Path root = source.toRealPath();
                try (var stream = Files.walk(source)) {
                    for (Path file : stream.toList()) {
                        if (!file.toRealPath().startsWith(root)) throw new IOException("Pack path escapes source: " + file);
                        if (Files.isSymbolicLink(file) && Files.isDirectory(file)) throw new IOException("Linked pack directories are not supported: " + file);
                        if (Files.isRegularFile(file)) {
                            String name = source.relativize(file).toString().replace('\\', '/');
                            GunPackOverlayResolver.validateEntryPath(name);
                            files.put(name, () -> Files.newInputStream(file));
                        }
                    }
                }
            } else {
                zip = new ZipFile(source.toFile());
                var entries = zip.entries();
                while (entries.hasMoreElements()) {
                    var entry = entries.nextElement();
                    GunPackOverlayResolver.validateEntryPath(entry.getName());
                    if (!entry.isDirectory() && files.putIfAbsent(entry.getName(), () -> zip.getInputStream(entry)) != null) {
                        throw new IOException("Duplicate ZIP entry: " + entry.getName());
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            close();
            throw e;
        }
    }

    @Override
    public void close() throws IOException {
        if (zip != null) zip.close();
    }
}
