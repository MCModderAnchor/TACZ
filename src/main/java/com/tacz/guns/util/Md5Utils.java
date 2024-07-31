package com.tacz.guns.util;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class Md5Utils {
    private static final int STREAM_BUFFER_LENGTH = 1024;
    private static final MessageDigest DIGEST;

    static {
        try {
            DIGEST = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String md5Hex(InputStream inputStream) throws IOException {
        return toHexString(md5(inputStream));
    }

    public static String md5Hex(byte[] data) {
        return toHexString(DIGEST.digest(data));
    }

    public static byte[] md5(InputStream inputStream) throws IOException {
        final byte[] buffer = new byte[STREAM_BUFFER_LENGTH];
        int read = inputStream.read(buffer, 0, STREAM_BUFFER_LENGTH);
        while (read > -1) {
            DIGEST.update(buffer, 0, read);
            read = inputStream.read(buffer, 0, STREAM_BUFFER_LENGTH);
        }
        return DIGEST.digest();
    }

    public static byte[] md5(byte[] data) {
        return DIGEST.digest(data);
    }

    public static String toHexString(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
