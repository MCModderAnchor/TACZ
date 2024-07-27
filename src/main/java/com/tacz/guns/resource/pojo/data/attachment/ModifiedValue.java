package com.tacz.guns.resource.pojo.data.attachment;

import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;

public class ModifiedValue {
    @SerializedName("addend")
    private double addend = 0;

    @SerializedName("percent")
    private double percent = 0;

    @SerializedName("multiply")
    private double multiply = 1;

    @Nullable
    @SerializedName("function")
    private String function = null;

    public double getAddend() {
        return addend;
    }

    public double getPercent() {
        return percent;
    }

    public double getMultiply() {
        return multiply;
    }

    @Nullable
    public String getFunction() {
        return function;
    }

    @Deprecated
    public void setAddend(double addend) {
        this.addend = addend;
    }
}
