package com.tacz.guns.client.resource.pojo.model;

public enum BedrockVersion {
    /**
     * 旧版本基岩版模型
     */
    LEGACY("1.10.0"),
    /**
     * 新版本基岩版模型，往后的 1.14.0，1.16.0 1.21.0 通通用此版本读取
     */
    NEW("1.12.0");

    private final String version;

    BedrockVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public static boolean isNewVersion(BedrockModelPOJO bedrockModel) {
        String[] checkVersion = bedrockModel.getFormatVersion().split("\\.", 3);
        String[] newVersion = NEW.getVersion().split("\\.", 3);
        if (checkVersion.length == 3 && newVersion.length == 3) {
            return Integer.parseInt(checkVersion[1]) >= Integer.parseInt(newVersion[1]);
        }
        return false;
    }

    public static boolean isLegacyVersion(BedrockModelPOJO bedrockModel) {
        return bedrockModel.getFormatVersion().equals(LEGACY.getVersion());
    }
}
