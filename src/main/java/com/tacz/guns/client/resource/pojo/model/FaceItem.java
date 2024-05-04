package com.tacz.guns.client.resource.pojo.model;

import com.google.gson.annotations.SerializedName;

public class FaceItem {
    public static final FaceItem EMPTY = empty();

    @SerializedName("uv")
    private float[] uv;

    @SerializedName("uv_size")
    private float[] uvSize;

    public static FaceItem single16X() {
        FaceItem face = new FaceItem();
        face.uv = new float[]{0, 0};
        face.uvSize = new float[]{16, 16};
        return face;
    }

    private static FaceItem empty() {
        FaceItem face = new FaceItem();
        face.uv = new float[]{0, 0};
        face.uvSize = new float[]{0, 0};
        return face;
    }

    public float[] getUv() {
        return uv;
    }

    public float[] getUvSize() {
        return uvSize;
    }
}
