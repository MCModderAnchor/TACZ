package com.tac.guns.client.resource.pojo.display.gun;

import com.google.gson.annotations.SerializedName;
import com.mojang.math.Vector3f;
import com.tac.guns.client.resource.pojo.CommonTransformObject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class AnimationInfluenceCoefficient {
    @SerializedName("iron_view")
    @Nullable
    private CommonTransformObject ironView;
    @SerializedName("scope_view")
    @Nullable
    private CommonTransformObject scopeView;

    @Nonnull
    public CommonTransformObject getIronView() {
        if (ironView == null) {
            ironView = new CommonTransformObject(new Vector3f(0.2f, 0.2f, 0.2f), new Vector3f(0.2f, 0.2f, 0.2f), new Vector3f(0f, 0f, 0f));
        }
        return ironView;
    }

    @Nonnull
    public CommonTransformObject getScopeView() {
        if (scopeView == null) {
            scopeView = new CommonTransformObject(new Vector3f(0.2f, 0.2f, 0.2f), new Vector3f(0.2f, 0.2f, 0.2f), new Vector3f(0f, 0f, 0f));
        }
        return scopeView;
    }

    public void writeDefaultIfNull() {
        if (ironView == null) {
            ironView = new CommonTransformObject(new Vector3f(0.2f, 0.2f, 0.2f), new Vector3f(0.2f, 0.2f, 0.2f), new Vector3f(0f, 0f, 0f));
        }
        if (scopeView == null) {
            scopeView = new CommonTransformObject(new Vector3f(0.2f, 0.2f, 0.2f), new Vector3f(0.2f, 0.2f, 0.2f), new Vector3f(0f, 0f, 0f));
        }
        if (ironView.translation == null) {
            ironView.translation = new Vector3f(0.2f, 0.2f, 0.2f);
        }
        if (ironView.rotation == null) {
            ironView.rotation = new Vector3f(0.2f, 0.2f, 0.2f);
        }
        if (scopeView.translation == null) {
            scopeView.translation = new Vector3f(0.2f, 0.2f, 0.2f);
        }
        if (scopeView.rotation == null) {
            scopeView.rotation = new Vector3f(0.2f, 0.2f, 0.2f);
        }
    }
}
