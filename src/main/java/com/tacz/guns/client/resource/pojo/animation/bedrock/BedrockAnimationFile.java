package com.tacz.guns.client.resource.pojo.animation.bedrock;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class BedrockAnimationFile {
    @SerializedName("version")
    private String version;

    @SerializedName("animations")
    private Map<String, BedrockAnimation> animations;

    public String getVersion() {
        return version;
    }

    public Map<String, BedrockAnimation> getAnimations() {
        return animations;
    }
}
