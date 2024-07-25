package com.tacz.guns.resource.pojo.data.attachment;

import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;

public class ModifiedValue {
    @SerializedName("addend")
    private double addend = 0;

    @SerializedName("multiply")
    private Double multiply = null;

    @SerializedName("function")
    private String function = null;

    public double getAddend() {
        return addend;
    }

    @Nullable
    public Double getMultiply() {
        return multiply;
    }

    @Nullable
    public String getFunction() {
        return function;
    }
}
