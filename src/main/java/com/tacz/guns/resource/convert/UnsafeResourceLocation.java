package com.tacz.guns.resource.convert;

// 不验证路径是否有效的 ResourceLocation 实现
public record UnsafeResourceLocation(String namespace, String path) {
    public String getNamespace() {
        return namespace;
    }

    public String getPath() {
        return path;
    }
}
