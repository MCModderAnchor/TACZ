package com.tac.guns.util;

import java.io.File;

public final class PathHandler {
    public static String getPath(File root, File file, String suffix) {
        int preLength = root.getPath().length() + 1;
        String path = file.getPath();
        String relativePath = path.substring(preLength, path.length() - suffix.length());
        return relativePath.replace('\\', '/');
    }
}
