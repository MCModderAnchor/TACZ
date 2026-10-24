package com.tacz.guns.resource.convert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Selects immediate child packs and isolates failures between conversions. */
public final class GunPackConversionBatch {
    private GunPackConversionBatch() { }

    public static Path select(Path directory, String name) throws IOException {
        Path root = directory.toRealPath();
        Path path = root.resolve(name).normalize();
        if (!root.equals(path.getParent())) {
            throw new IOException("Select a gun pack directly inside tacz/");
        }
        Path real = path.toRealPath();
        if (!real.startsWith(root) || !(Files.isDirectory(real)
            || Files.isRegularFile(real) && name.toLowerCase(Locale.ROOT).endsWith(".zip"))) {
            throw new IOException("Select a gun pack directly inside tacz/");
        }
        return real;
    }

    public static List<Path> discover(Path directory) throws IOException {
        Path root = directory.toRealPath();
        List<Path> packs = new ArrayList<>();
        try (var paths = Files.list(root)) {
            for (Path path : paths.sorted().toList()) {
                boolean archive = Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                    && path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip");
                boolean folder = Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)
                    && Files.isRegularFile(path.resolve("gunpack.meta.json"), LinkOption.NOFOLLOW_LINKS);
                if (archive || folder) packs.add(select(root, path.getFileName().toString()));
            }
        }
        return packs.stream().distinct().toList();
    }

    public record Outcome(Path source, UniversalPackConverter.Result result, Exception failure) { }

    public record Summary(int complete, int partial, int failed) {
        public int total() { return complete + partial + failed; }
    }

    public static Summary convert(List<Path> sources, UniversalPackConverter converter, Path output,
                                  boolean export, Consumer<Outcome> listener) {
        int complete = 0;
        int partial = 0;
        int failed = 0;
        for (Path source : sources) {
            Outcome outcome;
            try {
                var result = converter.convert(source, output, export);
                if (result.report().hasErrors()) failed++;
                else if (result.report().isPartial()) partial++;
                else complete++;
                outcome = new Outcome(source, result, null);
            } catch (Exception e) {
                failed++;
                outcome = new Outcome(source, null, e);
            }
            listener.accept(outcome);
        }
        return new Summary(complete, partial, failed);
    }
}
