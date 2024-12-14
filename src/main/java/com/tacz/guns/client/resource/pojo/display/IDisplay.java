package com.tacz.guns.client.resource.pojo.display;

import net.minecraft.resources.FileToIdConverter;

/**
 * 这个接口其实是妥协用的，用于将旧的texture路径转换为新的路径<br/>
 * 在反序列化完成后，会调用init方法，将所有的路径转换为新的路径
 */
public interface IDisplay {
    FileToIdConverter converter = new FileToIdConverter("textures", ".png");
    void init();
}
