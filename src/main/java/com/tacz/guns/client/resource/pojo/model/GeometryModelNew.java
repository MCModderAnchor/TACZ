package com.tacz.guns.client.resource.pojo.model;

import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;
import java.util.List;

public class GeometryModelNew {
    @SerializedName("description")
    private Description description;

    @SerializedName("bones")
    @Nullable
    private List<BonesItem> bones;

    public Description getDescription() {
        return description;
    }

    @Nullable
    public List<BonesItem> getBones() {
        return bones;
    }

    public GeometryModelNew deco() {
        if (bones != null) {
            this.bones.forEach(bonesItem -> {
                if (bonesItem.getCubes() != null) {
                    bonesItem.getCubes().forEach(cubesItem -> {
                        if (!cubesItem.isHasMirror()) {
                            cubesItem.setMirror(bonesItem.isMirror());
                        }
                    });
                }
            });
        }
        return this;
    }
}
