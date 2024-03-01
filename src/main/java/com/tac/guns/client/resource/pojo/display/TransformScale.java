package com.tac.guns.client.resource.pojo.display;

import com.google.gson.annotations.SerializedName;
import com.mojang.math.Vector3f;

import javax.annotation.Nullable;

public class TransformScale {
    @SerializedName("firstperson")
    @Nullable
    private Vector3f firstPerson;
    @SerializedName("thirdperson")
    @Nullable
    private Vector3f thirdPerson;
    @SerializedName("ground")
    @Nullable
    private Vector3f ground;
    @SerializedName("fixed")
    @Nullable
    private Vector3f fixed;
}
