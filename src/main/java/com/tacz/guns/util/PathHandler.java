package com.tacz.guns.util;

import java.nio.file.Path;

public final class PathHandler {
    public static String getPath(Path root, Path file, String suffix) {
        String relative = root.relativize(file).toString();
        String relativeWithoutSuffix = relative.substring(0, relative.length() - suffix.length());
        return relativeWithoutSuffix.replace('\\', '/');
    }
}
