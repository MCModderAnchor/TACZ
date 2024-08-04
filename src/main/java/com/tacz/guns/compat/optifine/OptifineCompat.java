package com.tacz.guns.compat.optifine;

public class OptifineCompat {
    private static final boolean IS_OPTIFINE_INSTALLED = isClassFound("net.optifine.Config");

    public static boolean isOptifineInstalled() {
        return IS_OPTIFINE_INSTALLED;
    }

    public static boolean isClassFound(String className) {
        try {
            Class.forName(className, false, Thread.currentThread().getContextClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
