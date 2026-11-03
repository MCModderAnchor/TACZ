package com.tacz.guns.client.resource.pojo.display.gun;

import com.google.gson.annotations.SerializedName;
import javax.annotation.Nullable;

/** 枪包中可供多把枪引用的开镜运动和 FOV 预设。 */
public class AdsPreset {
    @SerializedName("ads_path")
    private @Nullable AdsPath path;
    @SerializedName("ads_fov")
    private @Nullable AdsFov fov;

    public @Nullable AdsPath getPath() {
        return path;
    }

    public @Nullable AdsFov getFov() {
        return fov;
    }
}
