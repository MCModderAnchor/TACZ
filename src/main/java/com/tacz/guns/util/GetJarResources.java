package com.tacz.guns.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.tacz.guns.GunMod;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.commons.io.filefilter.TrueFileFilter;

import javax.annotation.Nullable;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class GetJarResources {
    /**
     * 打包时间会影响压缩包的哈希值，故直接指定时间
     * <p>
     * 此时间为 TaCZ 第一笔提交时间
     */
    private static final Instant BACKUP_TIME = Instant.parse("2024-02-26T12:28:08.000Z");
    private static final Path BACKUP_PATH = Paths.get("config", GunMod.MOD_ID, "backup");
    private static final SimpleDateFormat BACKUP_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd-HHmmss");
    private static final int MAX_BACKUP_COUNT = 10;

    private GetJarResources() {
    }

    /**
     * 复制本模组的文件到指定文件夹。将强行覆盖原文件。
     *
     * @param srcPath jar 中的源文件地址
     * @param root    想要复制到的根目录
     * @param path    复制后的路径
     */
    public static void copyModFile(String srcPath, Path root, String path) {
        URL url = GunMod.class.getResource(srcPath);
        try {
            if (url != null) {
                FileUtils.copyURLToFile(url, root.resolve(path).toFile());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 复制本模组的文件夹到指定文件夹。将强行覆盖原文件夹。
     *
     * @param srcPath jar 中的源文件地址
     * @param root    想要复制到的根目录
     * @param path    复制后的路径
     */
    public static void copyModDirectory(Class<?> resourceClass, String srcPath, Path root, String path) {
        URL url = resourceClass.getResource(srcPath);
        try {
            if (url != null) {
                copyFolder(url.toURI(), root.resolve(path));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 复制本模组的文件夹到指定文件夹。将强行覆盖原文件夹。
     *
     * @param srcPath jar 中的源文件地址
     * @param root    想要复制到的根目录
     * @param path    复制后的路径
     */
    public static void copyModDirectory(String srcPath, Path root, String path) {
        copyModDirectory(GunMod.class, srcPath, root, path);
    }

    @Nullable
    public static InputStream readModFile(String filePath) {
        URL url = GunMod.class.getResource(filePath);
        try {
            if (url != null) {
                return url.openStream();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void copyFolder(URI sourceURI, Path targetPath) throws IOException {
        if (Files.isDirectory(targetPath)) {
            // 备份原文件夹
            backupFiles(targetPath);
            // 删掉原文件夹，达到强行覆盖的效果
            deleteFiles(targetPath);
        }
        // 使用 Files.walk() 遍历文件夹中的所有内容
        try (Stream<Path> stream = Files.walk(Paths.get(sourceURI), Integer.MAX_VALUE)) {
            stream.forEach(source -> {
                // 生成目标路径
                Path target = targetPath.resolve(sourceURI.relativize(source.toUri()).toString());
                try {
                    // 复制文件或文件夹
                    if (Files.isDirectory(source)) {
                        Files.createDirectories(target);
                    } else {
                        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    // 处理异常，例如权限问题等
                    e.printStackTrace();
                }
            });
        }
    }

    private static void backupFiles(Path targetPath) throws IOException {
        // 创建子备份文件夹
        String dirName = targetPath.getFileName().toString();
        Path resourcePacksPath = FMLPaths.GAMEDIR.get().resolve("tacz_backup");
        Path backupPath = resourcePacksPath.resolve(dirName);
        if (!Files.isDirectory(backupPath)) {
            Files.createDirectories(backupPath);
        }

        // 检查备份文件数量，超过十个，删除时间最久的
        // 同时得到所有备份的 md5
        Set<String> cacheMd5 = checkOldBackups(backupPath);

        // 先生成一个临时文件
        File tempFile = File.createTempFile(dirName, ".tmp");
        FileTime fileTime = FileTime.from(BACKUP_TIME);

        // 开始写入文件
        try (ZipOutputStream zs = new ZipOutputStream(new FileOutputStream(tempFile));
             Stream<Path> fileWalks = Files.walk(targetPath)) {
            fileWalks.filter(Files::isRegularFile).forEach(path -> {
                String entryPath = targetPath.relativize(path).toString();
                ZipEntry zipEntry = new ZipEntry(entryPath);
                // 防止哈希值不一致，需要指定固定时间
                zipEntry.setLastModifiedTime(fileTime);
                try {
                    zs.putNextEntry(zipEntry);
                    Files.copy(path, zs);
                    zs.closeEntry();
                } catch (IOException e) {
                    GunMod.LOGGER.info("Error in zip file: {}", e.getMessage());
                }
            });
        }

        // 尝试计算哈希值
        try (FileInputStream inputStream = new FileInputStream(tempFile)) {
            String md5Hex = Md5Utils.md5Hex(inputStream);
            // 检查该备份是否存在
            if (cacheMd5.contains(md5Hex)) {
                // 存在的话，那就删掉备份
                tempFile.deleteOnExit();
            } else {
                // 否则把备份文件复制一份
                String dataName = BACKUP_DATE_FORMAT.format(new Date()).toLowerCase(Locale.ENGLISH);
                Path backupZipFilePath = backupPath.resolve(String.format("backup-%s-%s.zip", dataName, md5Hex));
                FileUtils.copyFile(tempFile, backupZipFilePath.toFile());
            }
        }
    }

    private static Set<String> checkOldBackups(Path backupPath) {
        // 临时缓存文件 md5
        Set<String> allMd5Hex = Sets.newHashSet();
        if (!Files.isDirectory(backupPath)) {
            return allMd5Hex;
        }
        try {
            List<File> delFiles = Lists.newArrayList(FileUtils.listFiles(backupPath.toFile(), TrueFileFilter.TRUE, null));
            delFiles.sort(LastModifiedFileComparator.LASTMODIFIED_REVERSE);
            int count = 1;
            for (File file : delFiles) {
                if (count >= MAX_BACKUP_COUNT) {
                    // 超过十个的进行删除
                    GunMod.LOGGER.info("Deleting old backup gun pack {}", file.getName());
                    FileUtils.deleteQuietly(file);
                } else {
                    // 十个以内的，计算 md5，看看有没有重复
                    try (FileInputStream inputStream = new FileInputStream(file)) {
                        allMd5Hex.add(Md5Utils.md5Hex(inputStream));
                    }
                }
                count++;
            }
        } catch (Exception exception) {
            GunMod.LOGGER.error("Error while checking old backup gun pack : {}", exception.getMessage());
        }
        return allMd5Hex;
    }

    private static void deleteFiles(Path targetPath) throws IOException {
        Files.walkFileTree(targetPath, new SimpleFileVisitor<>() {
            // 先去遍历删除文件
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            // 再去遍历删除目录
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}