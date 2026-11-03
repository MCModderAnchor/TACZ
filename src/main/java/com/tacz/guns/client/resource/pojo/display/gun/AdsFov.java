package com.tacz.guns.client.resource.pojo.display.gun;

import javax.annotation.Nullable;

/** 模型和世界 FOV 的开镜过渡可分别配置，缺省或无效通道保留原行为。 */
public class AdsFov {
    private @Nullable Channel model;
    private @Nullable Channel world;

    public @Nullable Channel getModel() {
        return model != null && model.isValid() ? model : null;
    }

    public @Nullable Channel getWorld() {
        return world != null && world.isValid() ? world : null;
    }

    public static class Channel extends AdsCurve {
        // 缺省保留原有 FOV 平滑器，可按通道关闭。
        private boolean smooth = true;

        public boolean isSmooth() {
            return smooth;
        }
    }
}
