package com.tacz.guns.resource.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** 服务端缓存快照与客户端接收端共用的压缩、分片协议。 */
public final class GunPackSync {
    public static final int CHUNK_SIZE = 256 * 1024;
    public static final int MAX_SYNC_SIZE = 64 * 1024 * 1024;

    private GunPackSync() {
    }

    public static List<Part> compressAndSplit(byte[] uncompressed) throws IOException {
        checkSize(uncompressed.length);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
            gzip.write(uncompressed);
        }
        byte[] compressed = output.toByteArray();
        checkSize(compressed.length);

        UUID transferId = UUID.randomUUID();
        List<Part> parts = new ArrayList<>();
        for (int offset = 0; offset < compressed.length; offset += CHUNK_SIZE) {
            parts.add(new Part(transferId, offset / CHUNK_SIZE, compressed.length, uncompressed.length,
                    Arrays.copyOfRange(compressed, offset, Math.min(offset + CHUNK_SIZE, compressed.length))));
        }
        return List.copyOf(parts);
    }

    private static byte[] decompress(byte[] compressed, int uncompressedSize) throws IOException {
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
            // 多读取一个字节以检查长度声明，避免无上限地解压数据。
            byte[] uncompressed = gzip.readNBytes(uncompressedSize + 1);
            if (uncompressed.length != uncompressedSize) {
                throw new IOException("Gun pack sync decompressed size does not match " + uncompressedSize);
            }
            return uncompressed;
        }
    }

    private static void checkSize(int size) {
        if (size <= 0 || size > MAX_SYNC_SIZE) {
            throw new IllegalArgumentException("Gun pack sync size must be between 1 and " + MAX_SYNC_SIZE + " bytes: " + size);
        }
    }

    public record Part(UUID transferId, int index, int compressedSize, int uncompressedSize, byte[] data) {
        public Part {
            Objects.requireNonNull(transferId);
            Objects.requireNonNull(data);
            checkSize(compressedSize);
            checkSize(uncompressedSize);
            int partCount = (compressedSize + CHUNK_SIZE - 1) / CHUNK_SIZE;
            if (index < 0 || index >= partCount) {
                throw new IllegalArgumentException("Invalid gun pack sync part index: " + index);
            }
            int expectedSize = Math.min(CHUNK_SIZE, compressedSize - index * CHUNK_SIZE);
            if (data.length != expectedSize) {
                throw new IllegalArgumentException("Invalid gun pack sync part size: " + data.length);
            }
        }
    }

    /** 同时只接收一个同步快照；连接切换时需要清理。 */
    public static final class Receiver {
        private UUID transferId;
        private byte[] compressed;
        private int uncompressedSize;
        private int nextIndex;

        /** TCP 保证分片有序，只有收到最后一片才返回解压后的完整数据。 */
        public byte[] accept(Part part) throws IOException {
            if (part.index() == 0) {
                clear();
                transferId = part.transferId();
                compressed = new byte[part.compressedSize()];
                uncompressedSize = part.uncompressedSize();
            }
            if (!part.transferId().equals(transferId) || part.index() != nextIndex
                    || compressed.length != part.compressedSize() || uncompressedSize != part.uncompressedSize()) {
                clear();
                throw new IllegalArgumentException("Inconsistent or out-of-order gun pack sync part");
            }
            int offset = part.index() * CHUNK_SIZE;
            System.arraycopy(part.data(), 0, compressed, offset, part.data().length);
            nextIndex++;
            if (offset + part.data().length < compressed.length) {
                return null;
            }

            byte[] complete = compressed;
            int completeSize = uncompressedSize;
            clear();
            return decompress(complete, completeSize);
        }

        public void clear() {
            transferId = null;
            compressed = null;
            uncompressedSize = 0;
            nextIndex = 0;
        }
    }
}
