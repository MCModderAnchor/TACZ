package com.tac.guns.client.resource.pojo.display;

import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;

public class GunTransform {
    @SerializedName("thirdperson_righthand")
    @Nullable
    private TransformContent thirdPersonRightHand;
    @SerializedName("thirdperson_lefthand")
    @Nullable
    private TransformContent thirdPersonLeftHand;
    @SerializedName("ground")
    @Nullable
    private TransformContent ground;
    @SerializedName("fixed")
    @Nullable
    private TransformContent fixed;

    @Nullable
    public TransformContent getThirdPersonRightHand() {
        return thirdPersonRightHand;
    }

    @Nullable
    public TransformContent getThirdPersonLeftHand() {
        return thirdPersonLeftHand;
    }

    @Nullable
    public TransformContent getGround() {
        return ground;
    }

    @Nullable
    public TransformContent getFixed() {
        return fixed;
    }
}
