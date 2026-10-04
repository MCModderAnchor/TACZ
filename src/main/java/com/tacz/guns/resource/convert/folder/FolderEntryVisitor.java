package com.tacz.guns.resource.convert.folder;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;

public interface FolderEntryVisitor {
    default boolean visitFile(File baseDir, File subFile) throws IOException {
        return false;
    }

    // return null if you can't handle the given directory
    @Nullable
    default FileVisitResult visitDirectory(File baseDir, File subFolder) throws IOException {
        return null;
    }
}
