package com.tacz.guns.resource.pojo.data.gun;

import com.google.gson.annotations.SerializedName;

public class Ignite {
    @SerializedName("entity")
    private boolean igniteEntity = false;

    @SerializedName("block")
    private boolean igniteBlock = false;

    public Ignite(boolean igniteEntity, boolean igniteBlock) {
        this.igniteEntity = igniteEntity;
        this.igniteBlock = igniteBlock;
    }

    public Ignite(boolean ignite) {
        this(ignite, ignite);
    }

    public boolean isIgniteEntity() {
        return igniteEntity;
    }

    public boolean isIgniteBlock() {
        return igniteBlock;
    }
}
