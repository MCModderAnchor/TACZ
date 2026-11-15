package com.tacz.guns.resource.convert.folder;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import static org.apache.commons.io.file.PathUtils.isEmptyDirectory;

public final class EmptyFolderRemover extends SimpleFileVisitor<Path> {
    private final Path baseDir;

    public EmptyFolderRemover(Path baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        if (!baseDir.equals(dir) && isEmptyDirectory(dir)) {
            Files.delete(dir);
            return FileVisitResult.SKIP_SUBTREE;
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        if (exc != null) {
            throw exc;
        }
        if (!baseDir.equals(dir) && isEmptyDirectory(dir)) {
            Files.delete(dir);
        }
        return FileVisitResult.CONTINUE;
    }
}
